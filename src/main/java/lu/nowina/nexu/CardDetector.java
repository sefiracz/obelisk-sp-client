/**
 * © Nowina Solutions, 2015-2016
 *
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package lu.nowina.nexu;

import com.sun.jna.*;
import iaik.pkcs.pkcs11.TokenException;
import lu.nowina.nexu.api.DetectedCard;
import lu.nowina.nexu.api.EnvironmentInfo;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.smartcardio.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Detects terminals and present smartcards/tokens
 */
@SuppressWarnings("restriction")
public class CardDetector {

	private static final List<String> RESET_CONTEXT_ERRORS = Arrays.asList(
			"SCARD_E_SERVICE_STOPPED", "WINDOWS_ERROR_INVALID_HANDLE", "SCARD_E_INVALID_HANDLE", "SCARD_E_NO_SERVICE");

	private static final Logger logger = LoggerFactory.getLogger(CardDetector.class.getSimpleName());

	private CardTerminals cardTerminals;

	private final NexuAPI api;
	private final WinscardLibrary lib;
	private final PresentCards presentCards = new PresentCards();
	private final ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "Monitor");
		t.setDaemon(true);
		return t;
	});

	public CardDetector(final NexuAPI api, final EnvironmentInfo info) {
		this.api = api;
		if (info.getOs() == OS.LINUX) {
			logger.info("The OS is Linux, we check for Library");
			try {
				final File libFile = at.gv.egiz.smcc.util.LinuxLibraryFinder.getLibraryPath("pcsclite", "1");
				if (libFile != null) {
					logger.info("Library installed is at " + libFile.getAbsolutePath());
					System.setProperty("sun.security.smartcardio.library", libFile.getAbsolutePath());
				}
			} catch (final Exception e) {
				logger.error("Error while loading library for Linux", e);
			}
		}
		if (info.getOs() == OS.MACOSX) {
			System.setProperty("sun.security.smartcardio.library", MAC_PATH);
		}

		this.cardTerminals = null;
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if(cardTerminals != null) {
				try {
					closeCardTerminals();
				} catch (Exception e) {
					logger.warn("Exception when closing cardTerminals", e);
				}
			}
		}));

		final String libraryName = Platform.isWindows() ? WINDOWS_PATH : Platform.isMac() ? MAC_PATH : PCSC_PATH;
		this.lib = (WinscardLibrary) Native.loadLibrary(libraryName, WinscardLibrary.class);
		startMonitor();
	}

	private List<CardTerminal> getCardTerminals() {
		final boolean cardTerminalsCreated;
		if(cardTerminals == null) {
			final TerminalFactory terminalFactory = TerminalFactory.getDefault();
			cardTerminals = terminalFactory.terminals();
			cardTerminalsCreated = true;
		} else {
			cardTerminalsCreated = false;
		}
		try {
			return cardTerminals.list();
		} catch(final CardException e) {
			final Throwable cause = e.getCause();
			if((cause != null) && RESET_CONTEXT_ERRORS.contains(cause.getMessage()) && !cardTerminalsCreated) {
				logger.debug("Error class: " + cause.getClass().getName() +
						". Message: " + cause.getMessage() +
						". Re-establish a new connection.");
				try {
					closeCardTerminals();
				} catch(final Exception e1) {
					logger.warn("Exception when closing cardTerminals", e1);
				}
				try {
					establishNewContext();
				} catch(final Exception e1) {
					Throwable t = e1;
					while (t != null) {
						if ("SCARD_E_NO_SERVICE".equals(t.getMessage())) {
							logger.error(e.getMessage(), e);
						}
						t = t.getCause();
					}
					// Rethrow exception as is.
					throw new RuntimeException(e1);
				}
				this.cardTerminals = null;
				return getCardTerminals();
			} else if((cause != null) && "SCARD_E_NO_READERS_AVAILABLE".equals(cause.getMessage())) {
				return Collections.emptyList();
			} else {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Checks the present cards and returns the present instance
	 * @param selector Selector card that is used as search template
	 * @return Card that is currently connected (might not be initialized yet) and present in computer
	 * @throws CardException Unable to determine the correct card or not found any match
	 */
	public DetectedCard getPresentCard(DetectedCard selector) throws CardException {
    if(selector.isConnected()) {
      // already connected, just return, nothing to do
      return selector;
    }
    // check if the card is currently being detected and present
    List<DetectedCard> detectedCards = presentCards.match(selector);
    if (detectedCards.size() == 1) {
      DetectedCard detectedCard = detectedCards.get(0);
      // set key/certificate values
      detectedCard.setCertificate(selector.getCertificate());
      detectedCard.setCertificateId(selector.getCertificateId());
      detectedCard.setKeyAlias(selector.getKeyAlias());
      return detectedCard;
    } else {
      // look for the card in terminals
      logger.info("Detect card '" + selector.getTokenLabel() + "' terminal connection");
      final List<DetectedCard> detectedCardsList = new ArrayList<>();
      int terminalIndex = 0;
      for (CardTerminal cardTerminal : getCardTerminals()) {
        try {
          final Card card = cardTerminal.connect("*");
          final ATR atr = card.getATR();
          DetectedCard detectedCard = new DetectedCard(atr.getBytes(), cardTerminal, terminalIndex, api);
          // completely different card, just skip
          if (!selector.getAtr().equals(detectedCard.getAtr())) {
            continue;
          }
          detectedCard.connectToken(api, null);
          if (presentCards.get(detectedCard) == null) {
            presentCards.add(detectedCard);
          } else {
            detectedCard.closeToken();
            detectedCard = presentCards.get(detectedCard); // use the existing one
          }
          detectedCardsList.add(detectedCard);
          logger.info(MessageFormat.format("Found card in terminal {0} with ATR {1}.", terminalIndex, detectedCard.getAtr()));
        } catch (CardException | IOException | TokenException e) {
          // Card not present or unreadable
          logger.warn(MessageFormat.format("No card present in terminal {0}, or not readable.", Integer.toString(terminalIndex)));
        }
        terminalIndex++;
      }
      // process found cards
      logger.info("Detected " + detectedCardsList.size() + " card(s) that could be card " + selector.getTokenLabel());
      if (detectedCardsList.size() == 1) {
        DetectedCard detectedCard = detectedCardsList.get(0);
        detectedCard.setCertificate(selector.getCertificate());
        detectedCard.setCertificateId(selector.getCertificateId());
        detectedCard.setKeyAlias(selector.getKeyAlias());
        return detectedCard;
      } else if (detectedCardsList.isEmpty()) {
        logger.warn("Card '" + selector.getTokenLabel() + "' not found");
        throw new CardNotPresentException("Card '" + selector.getTokenLabel() + "' not found");
      } else {
        logger.warn("Cannot conclusively determine card, return what is in the same terminal");
        for(DetectedCard detectedCard : detectedCardsList) {
        	if(detectedCard.getTerminalLabel().equals(selector.getTerminalLabel())) {
        		return detectedCard;
					}
				}
        // TODO - nutne otestovat s vice stejnymi kartami
        // TODO specificke okno pro tuto chybu? info at uzivatel odpoji nepotrebne tokeny
        throw new CardException("Cannot conclusively determine card");
      }
    }
	}

	/**
	 * Detect the smartcards connected to the computer.
	 *
	 * @return A list of detected smartcards.
	 */
	public List<DetectedCard> detectCards() {
		logger.info("Detect all cards in terminals");
		final List<DetectedCard> listCardDetect = new ArrayList<DetectedCard>();
		int terminalIndex = 0;
		for (CardTerminal cardTerminal : getCardTerminals()) {
			try {
				final Card card = cardTerminal.connect("*");
				final ATR atr = card.getATR();
				DetectedCard detectedCard = new DetectedCard(atr.getBytes(), cardTerminal, terminalIndex, api);
				detectedCard.connectToken(api, null);
				if (presentCards.get(detectedCard) == null) {
					presentCards.add(detectedCard);
				} else {
					detectedCard.closeToken();
					detectedCard = presentCards.get(detectedCard); // use the existing one
				}
				listCardDetect.add(detectedCard);
				logger.info(MessageFormat.format("Found card in terminal {0} with ATR {1}.", terminalIndex, detectedCard.getAtr()));
			} catch (IOException | TokenException | CardException e) {
				// Card not present or unreadable
				logger.warn(MessageFormat.format("No card present in terminal {0}, or not readable.", Integer.toString(terminalIndex)));
			}
			terminalIndex++;
		}
		return listCardDetect;
	}

	public CardTerminal getCardTerminal(final DetectedCard detectedCard) {
		for(final CardTerminal cardTerminal : getCardTerminals()) {
			Card card = null;
			try {
				card = cardTerminal.connect("*");
				final byte[] atr = card.getATR().getBytes();
				if(((detectedCard.getTerminalLabel() == null) || cardTerminal.getName().equals(detectedCard.getTerminalLabel())) &&
				   DetectedCard.atrToString(atr).equals(detectedCard.getAtr())) {
					return cardTerminal;
				}
			} catch(final CardException e) {
				// Log exception and continue
				logger.debug("CardException on connect", e);
			} finally {
				try {
					if(card != null) {
						card.disconnect(false);
					}
				} catch (CardException e) {
					logger.warn("CardException on disconnect.", e);
				}
			}
		}
		throw new IllegalArgumentException("Cannot find CardTerminal with label " +
				detectedCard.getTerminalLabel() + " and ATR " + detectedCard.getAtr());
	}

	private void closeCardTerminals() throws Exception {
		final Class<?> pcscTerminalsClass = Class.forName("sun.security.smartcardio.PCSCTerminals");
		final Field contextIdField = pcscTerminalsClass.getDeclaredField("contextId");
		contextIdField.setAccessible(true);
		final long contextId = contextIdField.getLong(null);

		if (contextId != 0L) {
			// Release current context
			final Dword result = lib.SCardReleaseContext(new SCardContext(contextId));
			if (result.longValue() != 0L) {
				logger.warn("Error when releasing context: " + result.longValue());
			}
			else {
				logger.debug("Context was released successfully.");
			}

			// Remove current context value
			contextIdField.setLong(null, 0L);

			// Clear terminals
			final Field terminalsField = pcscTerminalsClass.getDeclaredField("terminals");
			terminalsField.setAccessible(true);
			final Map<?, ?> terminals = (Map<?, ?>) terminalsField.get(null);
			terminals.clear();

			// finalize all initialized modules
			api.getPKCS11Manager().finalizeAllModules();
		}
	}

	private void establishNewContext() throws Exception {
		final Class<?> pcscTerminalsClass = Class.forName("sun.security.smartcardio.PCSCTerminals");
    	final Method initContextMethod = pcscTerminalsClass.getDeclaredMethod("initContext");
    	initContextMethod.setAccessible(true);
    	initContextMethod.invoke(null);
	}

	private boolean isPresent(DetectedCard card) {
	  CardTerminal cardTerminal = card.getTerminal();
    try {
      return cardTerminal != null && cardTerminal.isCardPresent();
    } catch (CardException ex) {
      return false;
    }
  }

	private void startMonitor() {
    monitor.scheduleAtFixedRate(() -> {
      List<DetectedCard> remove = new ArrayList<>();
      for(DetectedCard card : presentCards.getPresentCards()) {
        boolean present = isPresent(card);
        if (!present) {
          logger.info("Card "+card.getAtr()+" disconnected from terminal '"+card.getTerminal().getName()+"'");
          card.closeToken();
          remove.add(card);
        }
      }
      remove.forEach(presentCards::remove); // remove closed cards
    }, 0, 100, TimeUnit.MILLISECONDS);
	}

  private static class PresentCards {

    private final Object lock = new Object();
    private final List<DetectedCard> presentCards = new ArrayList<>();

    private List<DetectedCard> getPresentCards() {
      synchronized (lock) {
        return Collections.unmodifiableList(presentCards);
      }
    }

    private void add(DetectedCard detectedCard) {
      synchronized (lock) {
        presentCards.add(detectedCard);
      }
    }

    private DetectedCard get(DetectedCard selector) {
      synchronized (lock) {
        int index = presentCards.indexOf(selector);
        if (index != -1) {
          return presentCards.get(index);
        }
        return null;
      }
    }

    private List<DetectedCard> match(DetectedCard card) {
      synchronized (lock) {
        return presentCards.stream().filter(c -> c.softEquals(card)).collect(Collectors.toList());
      }
    }

    private void remove(DetectedCard card) {
      synchronized (lock) {
        presentCards.remove(card);
      }
    }

  }

	/***********************************************************************************************************/
	/* All following are inspired by                                                                           */
	/* https://github.com/jnasmartcardio/jnasmartcardio/blob/master/src/main/java/jnasmartcardio/Winscard.java */
	/***********************************************************************************************************/

	private static final String WINDOWS_PATH = "WinSCard.dll";
  private static final String MAC_LEGACY_PATH = "/System/Library/Frameworks/PCSC.framework/PCSC";
	private static final String MAC_PATH = "/System/Library/Frameworks/PCSC.framework/Versions/Current/PCSC";
	private static final String PCSC_PATH = "libpcsclite.so.1";

	/**
	 * The winscard API, also known as PC/SC. Implementations of this API exist
	 * on Windows, OS X, and Linux, although the symbol names and sizeof
	 * parameters differs on different platforms.
	 */
	private static interface WinscardLibrary extends Library {
		Dword SCardReleaseContext(SCardContext hContext);
	}

	// Following classes are public for {@link NativeMappedConverter#defaultValue()}.

	/**
	 * The DWORD type used by WinSCard.h, used wherever an integer is needed in
	 * SCard functions. On Windows and OS X, this is always typedef'd to a
	 * uint32_t. In the pcsclite library on Linux, it is a long
	 * instead, which is 64 bits on 64-bit Linux.
	 */
	public static class Dword extends IntegerType {
		public static final int SIZE = Platform.isWindows() || Platform.isMac() ? 4 : NativeLong.SIZE;

		private static final long serialVersionUID = 1L;

		public Dword() {
			this(0l);
		}

		public Dword(long value) {
			super(SIZE, value);
		}

		@Override
		public String toString() {
			return Long.toString(longValue());
		}
	}

	/**
	 * Base class for handles used in PC/SC. On Windows, it is a handle
	 * (ULONG_PTR which cannot be dereferenced). On PCSC, it is an integer
	 * (int32_t on OS X, long on Linux).
	 */
	public static class Handle extends IntegerType {
		private static final long serialVersionUID = 1L;

		public static final int SIZE = Platform.isWindows() ? Pointer.SIZE : Dword.SIZE;

		public Handle(long value) {
			super(SIZE, value);
		}

		@Override
		public String toString() {
			return String.format("%s{%x}", getClass().getSimpleName(), longValue());
		}
	}

	/**
	 * The SCARDCONTEXT type defined in WinSCard.h, used for most SCard
	 * functions.
	 */
	public static class SCardContext extends Handle {
		private static final long serialVersionUID = 1L;

		/** no-arg constructor needed for {@link NativeMappedConverter#defaultValue()}*/
		public SCardContext() {
			this(0l);
		}

		public SCardContext(long value) {
			super(value);
		}
	}
}

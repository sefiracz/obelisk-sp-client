/**
 * © Nowina Solutions, 2015-2015
 * © SEFIRA spol. s r.o., 2020-2021
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
package cz.sefira.obelisk.token.pkcs11;

import cz.sefira.obelisk.api.AbstractProduct;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.model.KeystoreType;
import cz.sefira.obelisk.api.model.OS;
import cz.sefira.obelisk.api.ws.model.SmartcardInfo;
import cz.sefira.obelisk.generic.ConnectionInfo;
import cz.sefira.obelisk.generic.SessionManager;
import cz.sefira.obelisk.view.BusyIndicator;
import iaik.pkcs.pkcs11.TokenException;

import javax.smartcardio.CardTerminal;
import java.io.IOException;
import java.util.*;

public class DetectedCard extends AbstractProduct {

	/**
	 * The atr.
	 */
	private String atr;

	/**
	 * The token label
	 */
	private String tokenLabel;

	/**
	 * The token serial number
	 */

	private String tokenSerial;

	/**
	 * The token manufacturer name
	 */
	private String tokenManufacturer;

  /**
   * Used card terminal
   */
  private transient CardTerminal terminal;

  /**
   * The terminal index.
   */
  private transient int terminalIndex;

  /**
   * The terminal label.
   */
  private transient String terminalLabel;

  /**
   * PKCS11 token handler
   */
	private transient TokenHandler tokenHandler;

  /**
   * Token is initialized
   */
	private transient boolean initialized = false;

  /**
   * Token has open session
   */
	private transient boolean opened = false;

  /**
   * Token has known configuration
   */
  private transient SmartcardInfo knownToken;

	private ConnectionInfo connectionInfo;

	public DetectedCard(byte[] atr, CardTerminal terminal, int terminalIndex, PlatformAPI api) {
		this.terminal = terminal;
		this.atr = DetectedCard.atrToString(atr);
		this.tokenLabel = api.getPKCS11Manager().getName(this.atr);
		this.terminalIndex = terminalIndex;
		this.terminalLabel = terminal.getName();
	}

	public DetectedCard() {
		super();
		this.type = KeystoreType.PKCS11;
	}

	public DetectedCard(String atr, int terminalIndex) {
		super();
		this.atr = atr;
		this.terminalIndex = terminalIndex;
		this.type = KeystoreType.PKCS11;
	}

	public DetectedCard(String atr, int terminalIndex, String terminalLabel) {
		super();
		this.atr = atr;
		this.terminalIndex = terminalIndex;
		this.terminalLabel = terminalLabel;
		this.type = KeystoreType.PKCS11;
	}

	/**
	 * Gets the atr.
	 *
	 * @return the atr
	 */
	public String getAtr() {
		return atr;
	}

	/**
	 * Sets the atr.
	 *
	 * @param atr
	 *            the atr to set
	 */
	public void setAtr(String atr) {
		this.atr = atr;
	}

  public void setTerminal(CardTerminal terminal) {
    this.terminal = terminal;
  }

  /**
	 * Get the index of the terminal from which the card info was read.
	 *
	 * @return the terminalIndex
	 */
	public int getTerminalIndex() {
		return terminalIndex;
	}

	/**
	 * Set the index of the terminal from which the card info was read.
	 *
	 * @param terminalIndex
	 *            the terminalIndex to set
	 */
	public void setTerminalIndex(int terminalIndex) {
		this.terminalIndex = terminalIndex;
	}

	/**
	 * Returns the label of the terminal from which the card info was read.
	 * @return The label of the terminal from which the card info was read.
	 */
	public String getTerminalLabel() {
		return terminalLabel;
	}

	/**
	 * Sets the label of the terminal from which the card info was read.
	 * @param terminalLabel The label of the terminal from which the card info was read.
	 */
	public void setTerminalLabel(String terminalLabel) {
		this.terminalLabel = terminalLabel;
	}

  /**
   * Returns value of TokenLabel that might take on value of TokenLabel,
   * ModelName or ATR depending on level of known card support
   * @return TokenLabel value
   */
	public String getTokenLabel() {
		return tokenLabel;
	}

  /**
   * Returns token serial or null
   * @return Token serial or null
   */
	public String getTokenSerial() {
		return tokenSerial;
	}

  /**
   * Returns token manufacturer or null
   * @return Token manufacturer or null
   */
	public String getTokenManufacturer() {
		return tokenManufacturer;
	}

  /**
   * Returns connected terminal
   * @return Connected terminal
   */
  public CardTerminal getTerminal() {
    return terminal;
  }

  /**
   * Returns initialized PKCS11 token handler
   * @return
   */
  public TokenHandler getTokenHandler() {
    return tokenHandler;
  }

  /**
   * Sets token label, model name or ATR depending on level of known card support
   * @param tokenLabel Token label value
   */
	public void setTokenLabel(String tokenLabel) {
		this.tokenLabel = tokenLabel;
	}

  /**
   * Sets token serial number
   * @param tokenSerial Token serial number
   */
	public void setTokenSerial(String tokenSerial) {
		this.tokenSerial = tokenSerial;
	}

  /**
   * Sets token manufacturer name
   * @param tokenManufacturer Token manufacturer name
   */
	public void setTokenManufacturer(String tokenManufacturer) {
		this.tokenManufacturer = tokenManufacturer;
	}

  public boolean isInitialized() {
    return initialized;
  }

  public boolean isOpened() {
    return opened;
  }

  public SmartcardInfo isKnownToken() {
    return knownToken;
  }

	public void setKnownToken(SmartcardInfo knownToken) {
		this.knownToken = knownToken;
	}

	public ConnectionInfo getConnectionInfo() {
		return connectionInfo;
	}

	public void setConnectionInfo(ConnectionInfo connectionInfo) {
		this.connectionInfo = connectionInfo;
	}

	@Override
	public String getTooltip() {
		return connectionInfo.getApiParam();
	}

	/**
   * Transform an ATR byte array into a string.
   *
   * @param b
   *            the ATR byte array
   * @return the string (empty if the ATR byte array is empty or null)
   */
  public static String atrToString(byte[] b) {
    final StringBuilder sb = new StringBuilder();
    if (b != null && b.length > 0) {
      sb.append(Integer.toHexString((b[0] & 240) >> 4));
      sb.append(Integer.toHexString(b[0] & 15));

      for (int i = 1; i < b.length; i++) {
        // sb.append(' ');
        sb.append(Integer.toHexString((b[i] & 240) >> 4));
        sb.append(Integer.toHexString(b[i] & 15));
      }
    }
    return sb.toString().toUpperCase();
  }

	@Override
	public String getSimpleLabel() {
		return getTokenLabel()+ " " + getTokenSerial()  + " (" + getTokenManufacturer() + ")";
	}

	@Override
	public String getLabel() {
		ResourceBundle rb = ResourceBundle.getBundle("bundles/nexu");
		String label = rb.getString("card.label.tokenLabel")+": "+getTokenLabel();
		if(getTokenManufacturer() != null) {
			label += "\n"+rb.getString("card.label.tokenManufacturer")+": " + getTokenManufacturer();
		}
		if(getTokenSerial() != null) {
			label += "\n"+rb.getString("card.label.tokenSerial")+": " + getTokenSerial();
		}
		return label;
	}

	public void initializeToken(PlatformAPI api, String pkcs11Path, boolean showBusy) throws IOException, TokenException {
		PKCS11Module module = api.getPKCS11Manager().getModule(atr, pkcs11Path);
		if (module != null) {
			try (BusyIndicator busyIndicator = new BusyIndicator(showBusy)) {
				tokenHandler = new TokenHandler(module, terminalLabel);
				tokenHandler.initialize();
				tokenLabel = tokenHandler.getTokenLabel();
				tokenSerial = tokenHandler.getTokenSerial();
				tokenManufacturer = tokenHandler.getTokenManufacturer();
				type = KeystoreType.PKCS11;
				initialized = true;
			}
		}
		knownToken = api.getPKCS11Manager().getAvailableSmartcardInfo(atr);
	}

	public void openToken() {
		if (initialized) {
			try (BusyIndicator busyIndicator = new BusyIndicator()) {
				// open session
				if (tokenHandler.openSession() > -1) {
					opened = true;
				}
			}
		}
	}

	public void closeToken() {
    SessionManager.getManager().destroy(this);
		if (initialized && opened) {
		  // close session
			tokenHandler.closeSession();
		}
    opened = false;
    initialized = false;
	}

	public boolean match(Object o) {
    if (this == o) return true;
		if (o == null || (getClass() != o.getClass() && !getClass().isAssignableFrom(o.getClass()) &&
				!o.getClass().isAssignableFrom(getClass()))) return false;

    DetectedCard that = (DetectedCard) o;

    if (!getAtr().equalsIgnoreCase(that.getAtr())) return false;
    if (getCertificateId() != null && that.getCertificateId() != null) {
      if (!getCertificateId().equals(that.getCertificateId())) return false;
    }
    if (getKeyAlias() != null && that.getKeyAlias() != null) {
      if (!getKeyAlias().equals(that.getKeyAlias())) return false;
    }
    // no terminal check (might have changed)
    return getSimpleLabel().equalsIgnoreCase(that.getSimpleLabel());
  }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || (getClass() != o.getClass() && !getClass().isAssignableFrom(o.getClass()) &&
				!o.getClass().isAssignableFrom(getClass()))) return false;

		DetectedCard that = (DetectedCard) o;

		if (!getAtr().equalsIgnoreCase(that.getAtr())) return false;
		if (getCertificateId() != null && that.getCertificateId() != null) {
			if (!getCertificateId().equals(that.getCertificateId())) return false;
		}
		if (getKeyAlias() != null && that.getKeyAlias() != null) {
			if (!getKeyAlias().equals(that.getKeyAlias())) return false;
		}
		if(getTerminalLabel() != null && that.getTerminalLabel() != null) {
			if (!getTerminalLabel().equalsIgnoreCase(that.getTerminalLabel())) return false;
		}
		if(getConnectionInfo() != null && that.getConnectionInfo() != null) {
			if (!getConnectionInfo().equals(that.getConnectionInfo())) return false;
		}
		return getSimpleLabel().equalsIgnoreCase(that.getSimpleLabel());
	}

	@Override
	public boolean matchToken(Object o) {
		if (this == o) return true;
		if (o == null || (getClass() != o.getClass() && !getClass().isAssignableFrom(o.getClass()) &&
				!o.getClass().isAssignableFrom(getClass()))) return false;

		DetectedCard that = (DetectedCard) o;
		if (!getAtr().equalsIgnoreCase(that.getAtr())) return false;
		if(getConnectionInfo() != null && that.getConnectionInfo() != null) {
			if (!getConnectionInfo().equals(that.getConnectionInfo())) return false;
		}
		return getSimpleLabel().equalsIgnoreCase(that.getSimpleLabel());
	}

	@Override
	public int hashCode() {
		int result = getAtr().hashCode();
		if(getKeyAlias() != null) {
			result = 31 * result + getKeyAlias().hashCode();
		}
		if(getCertificateId() != null) {
			result = 31 * result + getCertificateId().hashCode();
		}
		if(getTerminalLabel() != null) {
			result = 31 * result + getTerminalLabel().hashCode();
		}
		result = 31 * result + getSimpleLabel().hashCode();
		return result;
	}

}

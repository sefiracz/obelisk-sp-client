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
package lu.nowina.nexu;

import eu.europa.esig.dss.token.SignatureTokenConnection;
import javafx.scene.control.Button;
import lu.nowina.nexu.api.*;
import lu.nowina.nexu.api.flow.BasicOperationStatus;
import lu.nowina.nexu.api.flow.FutureOperationInvocation;
import lu.nowina.nexu.api.flow.OperationFactory;
import lu.nowina.nexu.api.plugin.HttpPlugin;
import lu.nowina.nexu.cache.TokenCache;
import lu.nowina.nexu.flow.Flow;
import lu.nowina.nexu.flow.FlowRegistry;
import lu.nowina.nexu.flow.operation.CoreOperationStatus;
import lu.nowina.nexu.generic.*;
import lu.nowina.nexu.pkcs11.PKCS11Manager;
import lu.nowina.nexu.view.DialogMessage;
import lu.nowina.nexu.view.core.NonBlockingUIOperation;
import lu.nowina.nexu.view.core.UIDisplay;
import lu.nowina.nexu.view.core.UIOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * Implementation of the NexuAPI
 *
 * @author David Naramski
 *
 */
@SuppressWarnings("restriction")
public class InternalAPI implements NexuAPI {

	public static final ThreadGroup EXECUTOR_THREAD_GROUP = new ThreadGroup("ExecutorThreadGroup");

	private Logger logger = LoggerFactory.getLogger(InternalAPI.class.getName());

	private CardDetector detector;

	private List<ProductAdapter> adapters = new ArrayList<>();

	private Map<TokenId, SignatureTokenConnection> connections;

	private Map<String, HttpPlugin> httpPlugins = new HashMap<>();

	private UIDisplay display;

	private SCDatabase smartcardDatabase;

	private SmartcardInfoDatabase smartcardInfoDatabase;

	private PKCS11Manager pkcs11Manager;

	private FlowRegistry flowRegistry;

	private OperationFactory operationFactory;

	private AppConfig appConfig;

	private ExecutorService executor;

	private Future<?> currentTask;

	public InternalAPI(UIDisplay display, SCDatabase smartcardDatabase, SmartcardInfoDatabase scInfoDatabase,
										 FlowRegistry flowRegistry, OperationFactory operationFactory, AppConfig appConfig) {
		this.display = display;
		this.smartcardDatabase = smartcardDatabase;
		this.smartcardInfoDatabase = scInfoDatabase;
		this.detector = new CardDetector(this, EnvironmentInfo.buildFromSystemProperties(System.getProperties()));
		this.flowRegistry = flowRegistry;
		this.operationFactory = operationFactory;
		this.appConfig = appConfig;
		this.pkcs11Manager = new PKCS11Manager(this, scInfoDatabase);
		this.connections = new TokenCache<>(this.appConfig.getConnectionsCacheMaxSize());
		this.executor = Executors.newSingleThreadExecutor(r -> {
			final Thread t = new Thread(EXECUTOR_THREAD_GROUP, r);
			t.setDaemon(true);
			return t;
		});
		this.currentTask = null;
	}

	@Override
	public List<DetectedCard> detectCards() {
		return detector.detectCards();
	}

	@Override
	public DetectedCard getPresentCard(DetectedCard selector) throws CardException {
		return detector.getPresentCard(selector);
	}

	@Override
	public CardTerminal getCardTerminal(DetectedCard card) {
		return detector.getCardTerminal(card);
	}
	@Override
	public void detectCardTerminal(DetectedCard card) {
		detector.detectCardTerminal(card);
	}

	@Override
	public List<Match> matchingProductAdapters(Product p) {
		if (p == null) {
			logger.warn("Product argument should not be null");
			return Collections.emptyList();
		}
		List<Match> matches = new ArrayList<>();
		for (ProductAdapter adapter : adapters) {
			if (adapter.accept(p)) {
				logger.info("Product is instance of " + adapter.getClass().getSimpleName());
				matches.add(new Match(adapter, p));
			}
		}
		if (matches.isEmpty() && (p instanceof DetectedCard)) {
			final DetectedCard d = (DetectedCard) p;
			smartcardDatabase = loadDatabase(SCDatabase.class, "database-smartcard.xml");
			SCInfo info = smartcardDatabase.getInfo(d);
			if (info == null) {
				logger.info("Card " + d.getAtr() + " is not in the personal database");
				matches.addAll(checkKnownPKCS11Tokens(d));
			} else {
			  ConnectionInfo cInfo = info.getInfos().get(0);
				matches.add(new Match(new GenericCardAdapter(info, this), d, cInfo.getSelectedApi(), cInfo.getApiParam()));
			}
		}
		return matches;
	}

	private List<Match> checkKnownPKCS11Tokens(DetectedCard d) {
		logger.info("Check if "+d.getAtr()+" has known and present PKCS11 library.");
		List<Match> matches = new ArrayList<>();
		String pkcs11 = pkcs11Manager.getAvailablePkcs11Library(d.getAtr());
		if(pkcs11 != null) {
			SCInfo info = new SCInfo(d);
			// create connection info
			ConnectionInfo cInfo = new ConnectionInfo();
			cInfo.setApiParam(pkcs11);
			cInfo.setSelectedApi(ScAPI.PKCS_11);
			cInfo.setEnv(EnvironmentInfo.buildFromSystemProperties(System.getProperties()));
			info.getInfos().add(cInfo);
			// return smartcard adapter
			matches.add(new Match(new GenericCardAdapter(info, this), d, ScAPI.PKCS_11, pkcs11));
		}
		return matches;
	}

	@Override
	public void registerProductAdapter(ProductAdapter adapter) {
		adapters.add(adapter);
	}

	@Override
	public EnvironmentInfo getEnvironmentInfo() {
		return EnvironmentInfo.buildFromSystemProperties(System.getProperties());
	}

	@Override
	public TokenId registerTokenConnection(SignatureTokenConnection connection) {
		TokenId id = new TokenId();
		connections.put(id, connection);
		return id;
	}

	@Override
	public SignatureTokenConnection getTokenConnection(TokenId tokenId) {
		return connections.get(tokenId);
	}

	private <I, O> Execution<O> executeRequest(Flow<I, O> flow, I request) {
		Execution<O> resp = null;

		try {
			if(!EXECUTOR_THREAD_GROUP.equals(Thread.currentThread().getThreadGroup())) {
				final Future<Execution<O>> task;
				// Prevent race condition on currentTask
				synchronized (this) {
					if((currentTask != null) && !currentTask.isDone()) {
						currentTask.cancel(true);
					}

					task = executor.submit(() -> {
						return flow.execute(this, request);
					});
					currentTask = task;
				}

				resp = task.get();
			} else {
				// Allow re-entrant calls
				resp = flow.execute(this, request);
			}
			if(resp == null) {
				resp = new Execution<O>(CoreOperationStatus.NO_RESPONSE);
			}
			return resp;
		}  catch (Exception e) {
			resp = new Execution<O>(BasicOperationStatus.EXCEPTION);
			logger.error("Cannot execute request", e);
			final Feedback feedback = new Feedback(e);
			resp.setFeedback(feedback);
			return resp;
		} finally {
			final Feedback feedback;
			if(resp.getFeedback() == null) {
				feedback = new Feedback();
				resp.setFeedback(feedback);
			} else {
				feedback = resp.getFeedback();
			}
			feedback.setVersion(this.getAppConfig().getApplicationVersion());
			feedback.setInfo(this.getEnvironmentInfo());
		}
	}

	@Override
	public Execution<GetCertificateResponse> getCertificate(GetCertificateRequest request) {
		Flow<GetCertificateRequest, GetCertificateResponse> flow =
				flowRegistry.getFlow(FlowRegistry.CERTIFICATE_FLOW, display, this);
		flow.setOperationFactory(operationFactory);
		return executeRequest(flow, request);
	}

	@Override
	public Execution<GetTokenResponse> getToken(GetTokenRequest request) {
		Flow<GetTokenRequest, GetTokenResponse> flow =
				flowRegistry.getFlow(FlowRegistry.TOKEN_FLOW, display, this);
		flow.setOperationFactory(operationFactory);
		return executeRequest(flow, request);
	}

	@Override
	public Execution<SignatureResponse> sign(SignatureRequest request) {
		Flow<SignatureRequest, SignatureResponse> flow =
				flowRegistry.getFlow(FlowRegistry.SIGNATURE_FLOW, display, this);
		flow.setOperationFactory(operationFactory);
		return executeRequest(flow, request);
	}

  @Override
  public Execution<SmartcardListResponse> smartcardList(SmartcardListRequest request) {
    Flow<SmartcardListRequest, SmartcardListResponse> flow =
            flowRegistry.getFlow(FlowRegistry.SMARTCARD_LIST_FLOW, display, this);
    flow.setOperationFactory(operationFactory);
    return executeRequest(flow, request);
	}

	@Override
	public HttpPlugin getHttpPlugin(String context) {
		return httpPlugins.get(context);
	}

	@Override
	public void supportedSmartcardInfos(List<SmartcardInfo> infos, byte[] digest) {
		pkcs11Manager.supportedSmartcardInfos(infos, digest);
	}

  public void showSslWarning(String browserTypeProperty, String certName) {
    ResourceBundle resources = ResourceBundle.getBundle("bundles/nexu");
    String messageText = MessageFormat.format(resources.getString("install.ca.cert.fail.message"),
            certName, resources.getString(browserTypeProperty));
    DialogMessage message = new DialogMessage(DialogMessage.Level.ERROR);
    message.setHeight(200);
    message.setWidth(475);
    message.setMessage(messageText);
    message.setShowDoNotShowCheckbox(true, "ssl-warning");
    // add button
    Button cert = new Button();
    cert.setText(resources.getString("install.ca.cert.button.cert.location"));
    cert.getStyleClass().add("btn-secondary");
    cert.setOnAction(e -> {
      try {
        Desktop.getDesktop().open(this.getAppConfig().getNexuHome());
      } catch (IOException io) {
        logger.error(io.getMessage(), io);
      }
    });
    message.addButton(cert);
    operationFactory.getMessageDialog(this, message, false);
  }

	@Override
	public PKCS11Manager getPKCS11Manager() {
		return pkcs11Manager;
	}

	public <T extends EntityDatabase> T loadDatabase(Class<T> entityClass, String filename) {
    return EntityDatabaseLoader.load(entityClass, new File(getAppConfig().getNexuHome(), filename));
  }

	public void registerHttpContext(String context, HttpPlugin plugin) {
		httpPlugins.put(context, plugin);
	}

	@Override
	public AppConfig getAppConfig() {
		return appConfig;
	}

	@Override
	public List<SystrayMenuItem> getExtensionSystrayMenuItem() {
		NexuAPI api = this;
		final List<SystrayMenuItem> result = new ArrayList<>();

		// keystore manager menu item
		result.add(new SystrayMenuItem() {

			@Override
			public String getName() {
				return "systray.menu.manage.keystores";
			}

			@Override
			public String getLabel() {
				return ResourceBundle.getBundle("bundles/nexu").getString(getName());
			}

			@Override
			public FutureOperationInvocation<Void> getFutureOperationInvocation() {
				return UIOperation.getFutureOperationInvocation(NonBlockingUIOperation.class,
						"/fxml/manage-keystores.fxml", api);
			}
		});

		// systray menu items for each adapter
		for(final ProductAdapter adapter : adapters) {
			final SystrayMenuItem menuItem = adapter.getExtensionSystrayMenuItem(api);
			if (menuItem != null) {
				result.add(menuItem);
			}
		}

		return result;
	}

	@Override
	public List<Product> detectProducts() {
		final List<Product> result = new ArrayList<>();
		for(final ProductAdapter adapter : adapters) {
			result.addAll(adapter.detectProducts());
		}
		return result;
	}

	public void detectAll() {
		detectProducts();
		detectCards();
	}

	@Override
	public String getLabel(Product p) {
		String label;
		final List<Match> matches = this.matchingProductAdapters(p);
		if(matches.isEmpty()) {
			label = p.getLabel();
		} else {
			final ProductAdapter adapter = matches.iterator().next().getAdapter();
			if(adapter.supportMessageDisplayCallback(p)) {
				label = adapter.getLabel(this, p, display.getPasswordInputCallback(p), display.getMessageDisplayCallback());
			} else {
				label = adapter.getLabel(this, p, display.getPasswordInputCallback(p));
			}
		}
		if(p instanceof DetectedCard) {
			ResourceBundle rb = ResourceBundle.getBundle("bundles/nexu");
			String terminalLabel = ((DetectedCard) p).getTerminalLabel();
			return label + "\n" + rb.getString("card.label.terminal") + ": " +
					(terminalLabel != null ? terminalLabel : rb.getString("card.label.terminal.disconnected"));
		}
		return label;
	}
}

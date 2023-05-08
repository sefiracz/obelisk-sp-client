/**
 * © Nowina Solutions, 2015-2015
 * © SEFIRA spol. s r.o., 2020-2021
 * <p>
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 * <p>
 * http://ec.europa.eu/idabc/eupl5
 * <p>
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package cz.sefira.obelisk;

import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.api.flow.BasicOperationStatus;
import cz.sefira.obelisk.api.flow.OperationFactory;
import cz.sefira.obelisk.api.model.EnvironmentInfo;
import cz.sefira.obelisk.api.model.ScAPI;
import cz.sefira.obelisk.api.plugin.VersionPlugin;
import cz.sefira.obelisk.api.ws.model.*;
import cz.sefira.obelisk.api.ws.ssl.SSLCertificateProvider;
import cz.sefira.obelisk.flow.Flow;
import cz.sefira.obelisk.flow.FlowRegistry;
import cz.sefira.obelisk.flow.operation.CoreOperationStatus;
import cz.sefira.obelisk.generic.*;
import cz.sefira.obelisk.storage.EventsStorage;
import cz.sefira.obelisk.storage.ProductStorage;
import cz.sefira.obelisk.storage.SmartcardStorage;
import cz.sefira.obelisk.token.pkcs11.DetectedCard;
import cz.sefira.obelisk.token.pkcs11.PKCS11Manager;
import cz.sefira.obelisk.view.StandaloneDialog;
import cz.sefira.obelisk.view.core.StageState;
import cz.sefira.obelisk.view.core.UIDisplay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import java.beans.PropertyChangeSupport;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Implementation of the NexuAPI
 *
 * @author David Naramski
 *
 */
@SuppressWarnings("restriction")
public class InternalAPI implements PlatformAPI {

  public static final ThreadGroup EXECUTOR_THREAD_GROUP = new ThreadGroup("ExecutorThreadGroup");

  private static final Logger logger = LoggerFactory.getLogger(InternalAPI.class.getName());

  private static final Object notificationLock = new Object();

  private final CardDetector detector;

  private final List<ProductAdapter> adapters = new ArrayList<>();

  private final UIDisplay display;

  private final PKCS11Manager pkcs11Manager;

  private final FlowRegistry flowRegistry;

  private final OperationFactory operationFactory;

  private SSLCertificateProvider sslCertificateProvider;

  private final ProductStorage productStorage;
  private final EventsStorage eventsStorage;
  private final ExecutorService executor;
  private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  private Future<?> currentTask;

  public InternalAPI(UIDisplay display, ProductStorage productStorage, SmartcardStorage smartcardStorage,
                     EventsStorage eventsStorage, FlowRegistry flowRegistry, OperationFactory operationFactory) {
    this.display = display;
    this.productStorage = productStorage;
    this.eventsStorage = eventsStorage;
    this.detector = new CardDetector(this, EnvironmentInfo.buildFromSystemProperties(System.getProperties()));
    this.flowRegistry = flowRegistry;
    this.operationFactory = operationFactory;
    this.pkcs11Manager = new PKCS11Manager(this, smartcardStorage);
    this.executor = Executors.newSingleThreadExecutor(r -> {
      final Thread t = new Thread(EXECUTOR_THREAD_GROUP, r);
      t.setDaemon(true);
      return t;
    });
    this.currentTask = null;
    StandaloneDialog.runLater(() -> StandaloneDialog.createDialogFromFXML("/fxml/notification.fxml", null, StageState.HIDDEN, propertyChangeSupport));
  }

  @Override
  public List<DetectedCard> detectCards(boolean showBusy) {
    return detector.detectCards(showBusy);
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
      final DetectedCard card = (DetectedCard) p;
      ProductStorage<DetectedCard> storage = getProductStorage(DetectedCard.class);
      if (!storage.getProducts(DetectedCard.class).contains(card)) {
        logger.info("Card " + card.getAtr() + " is not in the personal database");
        matches.addAll(checkKnownPKCS11Tokens(card));
      } else {
        card.setKnownToken(getPKCS11Manager().getAvailableSmartcardInfo(card.getAtr()));
        DetectedCard storedCard = storage.getProduct(card);
        ConnectionInfo cInfo = storedCard.getConnectionInfo();
        card.setConnectionInfo(cInfo);
        matches.add(new Match(new GenericCardAdapter(card, this), card, cInfo.getSelectedApi(), cInfo.getApiParam()));
      }
    }
    return matches;
  }

  private List<Match> checkKnownPKCS11Tokens(DetectedCard card) {
    logger.info("Check if " + card.getAtr() + " has known and present PKCS11 library.");
    List<Match> matches = new ArrayList<>();
    String pkcs11 = pkcs11Manager.getAvailablePkcs11Library(card.getAtr());
    if (pkcs11 != null) {
      // create connection info
      ConnectionInfo cInfo = new ConnectionInfo();
      cInfo.setApiParam(pkcs11);
      cInfo.setSelectedApi(ScAPI.PKCS_11);
      cInfo.setOs(EnvironmentInfo.buildFromSystemProperties(System.getProperties()).getOs());
      card.setConnectionInfo(cInfo);
      // return smartcard adapter
      matches.add(new Match(new GenericCardAdapter(card, this), card, ScAPI.PKCS_11, pkcs11));
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


  private <I, O> Execution<O> executeRequest(Flow<I, O> flow, I request) {
    Execution<O> resp;
    try {
      if (!EXECUTOR_THREAD_GROUP.equals(Thread.currentThread().getThreadGroup())) {
        final Future<Execution<O>> task;
        // Prevent race condition on currentTask
        synchronized (this) {
          if ((currentTask != null) && !currentTask.isDone()) {
            currentTask.cancel(true);
          }
          task = executor.submit(() -> flow.execute(this, request));
          currentTask = task;
        }
        resp = task.get();
      } else {
        // Allow re-entrant calls
        resp = flow.execute(this, request);
      }
      if (resp == null) {
        resp = new Execution<O>(CoreOperationStatus.NO_RESPONSE);
      }
      return resp;
    } catch (Exception e) {
      resp = new Execution<O>(BasicOperationStatus.EXCEPTION, e);
      logger.error("Cannot execute request", e);
      return resp;
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
  public Execution<SignatureResponse> sign(SignatureRequest request) {
    Flow<SignatureRequest, SignatureResponse> flow =
        flowRegistry.getFlow(FlowRegistry.SIGNATURE_FLOW, display, this);
    flow.setOperationFactory(operationFactory);
    return executeRequest(flow, request);
  }

  @Override
  public Execution<Boolean> checkSession(SessionValue sessionValue) {
    Flow<SessionValue, Boolean> flow = flowRegistry.getFlow(FlowRegistry.CHECK_SESSION_FLOW, display, this);
    flow.setOperationFactory(operationFactory);
    return executeRequest(flow, sessionValue);
  }

  public VersionPlugin getVersionPlugin() {
    return null;
  }

  @Override
  public void supportedSmartcardInfos(List<SmartcardInfo> infos, byte[] digest) {
    pkcs11Manager.supportedSmartcardInfos(infos, digest);
  }

  @Override
  public PKCS11Manager getPKCS11Manager() {
    return pkcs11Manager;
  }

  @Override
  public <T extends AbstractProduct> ProductStorage<T> getProductStorage(Class<T> c) {
    return productStorage;
  }

  @Override
  public EventsStorage getEventsStorage() {
    return eventsStorage;
  }

  @Override
  public void setSslCertificateProvider(SSLCertificateProvider sslCertificateProvider) {
    this.sslCertificateProvider = sslCertificateProvider;
  }

  public SSLCertificateProvider getSslCertificateProvider() {
    return sslCertificateProvider;
  }


  public void pushNotification(Notification notification) {
    synchronized (this) {
      logger.info("Push notification: " + notification.getMessageText());
      // push notification into events
      eventsStorage.addNotification(notification);
      // push notification message to dialog
      propertyChangeSupport.firePropertyChange("notify", new Object(), notification);
    }
  }

  public PropertyChangeSupport getPropertyChangeSupport() {
    synchronized (this) {
      return propertyChangeSupport;
    }
  }

  public OperationFactory getOperationFactory() {
    return operationFactory;
  }

  @Override
  public List<Product> detectProducts() {
    final List<Product> result = new ArrayList<>();
    for (final ProductAdapter adapter : adapters) {
      result.addAll(adapter.detectProducts());
    }
    return result;
  }

  public void detectAll() {
    detectProducts();
    detectCards(true);
  }

  @Override
  public String getLabel(Product p) {
    String label;
    final List<Match> matches = this.matchingProductAdapters(p);
    if (matches.isEmpty()) {
      label = p.getLabel();
    } else {
      final ProductAdapter adapter = matches.iterator().next().getAdapter();
      label = adapter.getLabel(this, p, display.getPasswordInputCallback(p));
    }
    if (p instanceof DetectedCard) {
      ResourceBundle rb = ResourceBundle.getBundle("bundles/nexu");
      String terminalLabel = ((DetectedCard) p).getTerminalLabel();
      return label + "\n" + rb.getString("card.label.terminal") + ": " +
          (terminalLabel != null ? terminalLabel : rb.getString("card.label.terminal.disconnected"));
    }
    return label;
  }

  @Override
  public UIDisplay getDisplay() {
    return display;
  }
}

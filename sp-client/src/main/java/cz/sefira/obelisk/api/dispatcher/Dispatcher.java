package cz.sefira.obelisk.api.dispatcher;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.Dispatcher
 *
 * Created: 26.01.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.AppConfigurer;
import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.api.flow.BasicOperationStatus;
import cz.sefira.obelisk.api.plugin.InitErrorMessage;
import cz.sefira.obelisk.api.plugin.AppPlugin;
import cz.sefira.obelisk.api.ws.GenericApiException;
import cz.sefira.obelisk.api.ws.SpApiClient;
import cz.sefira.obelisk.api.ws.auth.*;
import cz.sefira.obelisk.api.ws.model.*;
import cz.sefira.obelisk.api.ws.ssl.HttpResponse;
import cz.sefira.obelisk.api.ws.ssl.SSLCommunicationException;
import cz.sefira.obelisk.dss.DigestAlgorithm;
import cz.sefira.obelisk.ipc.Message;
import cz.sefira.obelisk.ipc.MessageQueue;
import cz.sefira.obelisk.ipc.MessageQueueFactory;
import cz.sefira.obelisk.json.GsonHelper;
import cz.sefira.obelisk.util.DSSUtils;
import cz.sefira.obelisk.util.HttpUtils;
import cz.sefira.obelisk.util.TextUtils;
import cz.sefira.obelisk.view.DialogMessage;
import cz.sefira.obelisk.view.StandaloneDialog;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static cz.sefira.obelisk.api.ws.model.Operation.*;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Dispatcher handling messages from queue
 */
public class Dispatcher implements AppPlugin {

  private static final Logger logger = LoggerFactory.getLogger(Dispatcher.class.getName());
  private static final long SYNC_SUPPORTED_SMARTCARDS_MILLISECONDS = TimeUnit.MINUTES.toMillis(15);
  private static final long IDLE_PERIOD_MILLISECONDS = TimeUnit.MILLISECONDS.toMillis(1000);
  private static final long IDLE_TIMEOUT_MILLISECONDS = TimeUnit.MINUTES.toMillis(5);

  private PlatformAPI api;
  private MessageQueue messageQueue;
  private SpApiClient client;

  private Date initializedDate;
  private boolean initialized;

  private long lastSyncTimestamp = 0L;
  private long idleTime = 0L;

  private final ScheduledExecutorService dispatcher = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "Dispatcher");
    t.setDaemon(true);
    return t;
  });

  @Override
  public List<InitErrorMessage> init(String pluginId, PlatformAPI api) {
    Security.addProvider(new BouncyCastleProvider());
    logger.info("Dispatcher initializing");
    try {
      this.api = api;
      this.messageQueue = MessageQueueFactory.getInstance(AppConfig.get());
      this.client = new SpApiClient(api);
      dispatcher.scheduleAtFixedRate(() -> {
        // init thread
        if (!initialized) {
          logger.info("Dispatcher thread started");
          initialized = true;
          initializedDate = new Date();
        }
        validateMessage(messageQueue.getMessage());
      }, 1000, 500, TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return List.of(new InitErrorMessage(this.getClass().getSimpleName(), "error.application.init", e));
    }
    return Collections.emptyList();
  }

  private void validateMessage(Message message) {
    // get message
    String notificationProperty = null;
    try {
      if (message == null) {
        return;
      }
      logger.info("Processing message: "+message.getId());
      // payload?
      byte[] payload = message.getPayload();
      if (payload == null) {
        logger.error("Ignoring message, missing payload");
        return;
      }
      // process input message
      URIBuilder uriBuilder = new URIBuilder(new String(payload)); // TODO decrypt payload?
      NameValuePair magicParam = uriBuilder.getFirstQueryParam("m");
      // process magiclink
      if (magicParam == null || magicParam.getValue() == null) {
        logger.error("Ignoring message, missing magic link");
        return;
      }
      // process language
      NameValuePair langParam = uriBuilder.getFirstQueryParam("l");
      if (langParam != null && langParam.getValue() != null) {
        AppConfigurer.applyLocale(langParam.getValue());
      }
      // process expiration
      NameValuePair timestampParam = uriBuilder.getFirstQueryParam("t");
      if (timestampParam != null && timestampParam.getValue() != null) {
        try {
          long expirationTime = Long.parseLong(timestampParam.getValue());
          if (expirationTime - System.currentTimeMillis() < 0) {
            logger.error("Ignoring message, already expired: "+TextUtils.formatXsDateTime(new Date(expirationTime)));
            return;
          }
        } catch (NumberFormatException e) {
          logger.error(e.getMessage(), e);
        }
      }
      // process version
      // TODO
      String magicLink = URLDecoder.decode(magicParam.getValue(), UTF_8);
      AuthenticationProvider tokenProvider = new BearerTokenProvider(magicLink, api); // obtain authorization credentials
      Execution<?> result = processMessage(tokenProvider);
      if (result != null) {
        if (result.isSuccess()) {
          notificationProperty = "notification.event.success";
        } else if (BasicOperationStatus.USER_CANCEL.getCode().equals(result.getError())) {
          notificationProperty = "notification.event.user.cancel";
        } else {
          notificationProperty = "notification.event.exception";
        }
      }
    } catch (GenericApiException e) {
      logger.error(e.getMessage(), e);
      DialogMessage errMsg = new DialogMessage(e.getMessageProperty(), DialogMessage.Level.ERROR);
      StandaloneDialog.runLater(() -> StandaloneDialog.showErrorDialog(errMsg, null, e));
      notificationProperty = "notification.event.fatal";
    }  catch (SSLCommunicationException e) {
      logger.error(e.getMessage(), e);
      StandaloneDialog.runLater(() -> StandaloneDialog.showSslErrorDialog(e));
      notificationProperty = "notification.event.fatal";
    } catch (HttpResponseException e) {
      logger.error(e.getMessage(), e);
      DialogMessage errMsg = new DialogMessage("dispatcher.communication.error", DialogMessage.Level.ERROR,
          new String[]{String.valueOf(e.getStatusCode()), e.getReasonPhrase()}, 475, 150);
      StandaloneDialog.runLater(() -> StandaloneDialog.showErrorDialog(errMsg, null, e));
      notificationProperty = "notification.event.fatal";
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      DialogMessage errMsg = new DialogMessage("dispatcher.generic.error", DialogMessage.Level.ERROR);
      StandaloneDialog.runLater(() -> StandaloneDialog.showErrorDialog(errMsg, null, e));
      notificationProperty = "notification.event.fatal";
    } finally {
      if (notificationProperty != null) {
        // close notification
        String messageText = ResourceBundle.getBundle("bundles/nexu").getString(notificationProperty);
        api.pushNotification(new Notification(messageText, true, 5));
      }
    }
  }

  private Execution<?> processMessage(AuthenticationProvider tokenProvider)
      throws GeneralSecurityException, URISyntaxException, IOException, InterruptedException {
    String url = tokenProvider.getRedirectUri();
    boolean sync = performSync();
    Execution<?> result;
    do {
      // GET work request
      HttpResponse response = client.call("GET", url, tokenProvider, null, sync);
      if (response.getCode() == HttpStatus.SC_ACCEPTED) {
        idle(); // wait operation = go back to GET method
        continue;
      } else if (response.getCode() != HttpStatus.SC_OK) {
        throw new HttpResponseException(response.getCode(), response.getReasonPhrase());
      }
      idleTime = 0;
      BaseRequest req = GsonHelper.fromJson(new String(response.getContent(), StandardCharsets.UTF_8), BaseRequest.class);
      // execute  result
      while (true) {
        // check if request is present
        if (req == null) {
          break;
        }
        // check session
        if (!checkSession(req.getSession(), url, tokenProvider)) {
          return null;
        }
        // notification
        api.pushNotification(new Notification(req.getDescription()));
        // synchronize supported hardware database
        sync = sync && syncDevices(req);
        // execute flow
        result = executeFlow(req, response.getContent());
        if (result != null) {
          audit(result, response);
          // send results
          response = client.call("POST", url, tokenProvider, result, false);
          int responseCode = response.getCode();
          if (responseCode == HttpStatus.SC_OK) {
            req = GsonHelper.fromJson(new String(response.getContent(), StandardCharsets.UTF_8), BaseRequest.class);
          } else if (responseCode == HttpStatus.SC_NO_CONTENT) {
            return result;
          } else if (responseCode == HttpStatus.SC_SEE_OTHER || responseCode == HttpStatus.SC_MOVED_TEMPORARILY) {
            url = HttpUtils.getLocationURI(response);
            break; // go back to GET method
          } else if (responseCode == HttpStatus.SC_ACCEPTED) {
            idle();
            break; // wait operation = go back to GET method
          }
        }
      }
    } while (true); // batch ended
  }

  private Execution<?> executeFlow(BaseRequest req, byte[] requestData) {
    // start flow
    Execution<?> result;
    switch (req.getOperation()) {
      case GET_CERTIFICATE:
        GetCertificateRequest getCertificateRequest = GsonHelper.fromJson(new String(requestData, StandardCharsets.UTF_8),
                GetCertificateRequest.class);
        result = api.getCertificate(getCertificateRequest);
        break;
      case SIGN:
        SignatureRequest signatureRequest = GsonHelper.fromJson(new String(requestData, StandardCharsets.UTF_8), SignatureRequest.class);
        result = api.sign(signatureRequest);
        result.setStepId(signatureRequest.getSignParams().getStepId());
        break;
      default:
        throw new IllegalStateException("Unknown operation: " + req.getOperation());
    }
    return result;
  }

  private boolean checkSession(SessionValue sessionValue, String url, AuthenticationProvider tokenProvider)
      throws GeneralSecurityException, URISyntaxException, IOException {
    Execution<?> result = api.checkSession(sessionValue);
    if (!result.isSuccess()) {
      client.call("POST", url, tokenProvider, result, false);
      logger.error("Session invalid, stop process");
      return false;
    }
    return true;
  }

  private void idle() throws InterruptedException {
    if (idleTime > IDLE_TIMEOUT_MILLISECONDS) {
      throw new GenericApiException("Operation timeout, server communication stalled", "dispatcher.expired.error");
    }
    Thread.sleep(IDLE_PERIOD_MILLISECONDS);
    idleTime += IDLE_PERIOD_MILLISECONDS;
  }

  private boolean syncDevices(BaseRequest baseRequest) {
    List<SmartcardInfo> smartcardInfos = baseRequest.getSmartcards();
    if (smartcardInfos == null) {
      return true; // try again
    }
    logger.info("Synchronizing supported hardware tokens database");
    byte[] digest = DSSUtils.digest(DigestAlgorithm.SHA1, smartcardInfos.toString().getBytes());
    api.supportedSmartcardInfos(smartcardInfos, digest);
    // mark sync time
    lastSyncTimestamp = System.currentTimeMillis();
    return false; // synchronized
  }

  private boolean performSync() {
    long syncTime = System.currentTimeMillis() - lastSyncTimestamp;
    return syncTime > SYNC_SUPPORTED_SMARTCARDS_MILLISECONDS;
  }


  private void audit(Execution<?> result, HttpResponse response) {
    Audit audit = new Audit(result.getUsedProduct(), initializedDate);
    result.setAudit(audit);
  }

}

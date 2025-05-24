package cz.sefira.obelisk.api.plugin;

import com.sun.javafx.webkit.WebConsoleListener;
import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.ws.HttpMethod;
import cz.sefira.obelisk.api.ws.SpApiClient;
import cz.sefira.obelisk.api.ws.ssl.HttpResponse;
import cz.sefira.obelisk.api.ws.ssl.SSLCommunicationException;
import cz.sefira.obelisk.util.annotation.NotNull;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.apache.hc.core5.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.FutureTask;

public class JavaFXWebViewPlugin extends CookiesPlugin {

  private static final Logger logger = LoggerFactory.getLogger(JavaFXWebViewPlugin.class.getName());

  private boolean initSSL = true;
  private WebView webView;
  private WebEngine webEngine;

  @Override
  public List<InitErrorMessage> init(String pluginId, PlatformAPI api) {
    try {
      // this call will check if javafx-web libraries are present
      WebConsoleListener.setDefaultListener((webView1, message, lineNumber, sourceId) -> {
        if (logger.isDebugEnabled()) {
          logger.debug("Console: [{}:{}] {}", sourceId, lineNumber, message);
        }
      });
      setPlatformAPI(api);

      final FutureTask<List<InitErrorMessage>> init = new FutureTask<>(() -> {
        try {
          webView = new WebView();
          webView.setContextMenuEnabled(false);
          webEngine = webView.getEngine();
          webEngine.setJavaScriptEnabled(true);
          webEngine.setOnError((e) -> logger.error("Error: {}", e.getMessage()));
          webEngine.getLoadWorker().exceptionProperty().addListener((ov, t, t1) -> {
            if (t1 != null) {
              String message = t1.getMessage();
              webEngine.loadContent(message); // show the error message instead of white page
              logger.error("Received exception: {}", message);
            }
          });
          return List.of();
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
          return List.of(new InitErrorMessage(this.getClass().getSimpleName(), "error.application.init", e));
        }
      });

      Platform.runLater(init);
      return init.get();
    } catch (Throwable e) {
      if (isOptional() && e instanceof NoClassDefFoundError) {
        throw new OptionalPluginException("Optional plugin '"+ JavaFXWebViewPlugin.class.getName()+"' not available");
      }
      logger.error(e.getMessage(), e);
      return List.of(new InitErrorMessage(this.getClass().getSimpleName(), "error.application.init", e));
    }
  }

  @Override
  public boolean checkAccess(SpApiClient client, String statusPage) throws SSLCommunicationException {
    try {
      HttpResponse response = client.call(HttpMethod.GET, statusPage);
      int responseCode = response.getCode();
      if (responseCode == HttpStatus.SC_OK) {
        String html = new String(response.getContent(), StandardCharsets.UTF_8);
        if (isStatusPage(html)) {
          return true;
        }
      }
      logger.info("Status page not accessible: {}", responseCode);
      return false;
    } catch (SSLCommunicationException e) {
      logger.error("Status page SSL error: {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      logger.error("WebView access check failed: "+e.getMessage(), e);
    }
    return false;
  }

  @Override
  public void load(Object sync, PlatformAPI api, String url) {
    Platform.runLater(()-> {
      initSSL(api);
      logger.info("Loading webview URL: {}", url);
      webEngine.load(url);

      VBox vBox = new VBox(webView);
      Scene scene = new Scene(vBox, 960, 600);
      Stage stage = new Stage();
      stage.titleProperty().bind(webEngine.titleProperty());
      stage.getIcons().add(new Image(AppConfig.get().getIconLogoStream()));
      stage.setScene(scene);

      stage.addEventHandler(KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
        if (event.getCode() == KeyCode.ESCAPE) {
          stage.close();
        }
      });

      webEngine.getLoadWorker().stateProperty().addListener((obs, oldValue, newValue) -> {
        String html = (String) webEngine.executeScript("document.documentElement.outerHTML");
        if (logger.isDebugEnabled()) {
          logger.debug("Loaded HTML: {}", html);
        }
        if (isStatusPage(html)) {
          stage.close();
        }
      });

      stage.showAndWait(); // show the webview and wait for it to close
      synchronized(sync) {
        sync.notify(); // notify waiting thread
      }
    });
  }

  @Override
  public boolean isOptional() {
    return true;
  }

  private void initSSL(PlatformAPI api) {
    // initialize SSL context before first call
    if (initSSL) {
      try {
        SSLContext.setDefault(api.getSslCertificateProvider().getSSLContext());
      } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
        logger.error("Failure to initialize SSLContext: "+e.getMessage(), e);
      }
      initSSL = false;
    }
  }

  private boolean isStatusPage(@NotNull String html) {
    return (html.contains("issuer") && html.contains("authorization_endpoint") && html.contains("token_endpoint") && html.contains("jwks_uri")) ||
        (html.contains("RUNNING") && html.contains("isSecure:") && html.contains("host:")); // /obelisk-sp-api/status
  }

}

package cz.sefira.obelisk.api.plugin;

import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.plugin.webview.JavaFXWebView;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.FutureTask;

public class JavaFXWebViewPlugin extends CookiesPlugin {

  private static final Logger logger = LoggerFactory.getLogger(JavaFXWebViewPlugin.class.getName());

  private JavaFXWebView webView;

  @Override
  public List<InitErrorMessage> init(String pluginId, PlatformAPI api) {
    try {
      webView = new JavaFXWebView();
      FutureTask<List<InitErrorMessage>> futureTask = webView.init(api);
      Platform.runLater(futureTask);
      return futureTask.get();
    } catch (Throwable e) {
      if (isOptional() && e instanceof NoClassDefFoundError) {
        throw new OptionalPluginException("Optional plugin '"+ JavaFXWebViewPlugin.class.getName()+"' not available");
      }
      logger.error(e.getMessage(), e);
      return List.of(new InitErrorMessage(this.getClass().getSimpleName(), "error.application.init", e));
    }
  }

  @Override
  public void load(Object sync, PlatformAPI api, String url) {
    webView.load(sync, api, url);
  }

  @Override
  public void dispose() {
    // no-op
  }

  @Override
  public boolean isOptional() {
    return true;
  }

}

package cz.sefira.obelisk.api.plugin;

import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.plugin.webview.JCEFWebView;
import cz.sefira.obelisk.view.DialogMessage;
import cz.sefira.obelisk.view.StandaloneDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class JCEFWebViewPlugin extends CookiesPlugin {

  private static final Logger logger = LoggerFactory.getLogger(JCEFWebViewPlugin.class.getName());

  private JCEFWebView webView;

  @Override
  public List<InitErrorMessage> init(String pluginId, PlatformAPI api) {
    try {
      webView = new JCEFWebView();
      webView.init(api);
      return List.of();
    } catch (Throwable e) {
      if (isOptional() && e instanceof NoClassDefFoundError) {
        throw new OptionalPluginException("Optional plugin '"+ JCEFWebViewPlugin.class.getName()+"' not available");
      }
      logger.error(e.getMessage(), e);
      return List.of(new InitErrorMessage(this.getClass().getSimpleName(), "error.application.init", e));
    }
  }

  @Override
  public void load(Object sync, PlatformAPI api, String url) {
    try {
      webView.load(sync, cookieStore, url);
    } catch (Throwable e) {
      logger.error("JCEF failed to load URL "+url+": "+e.getMessage(), e);
      DialogMessage errMsg = new DialogMessage("webview.load.failure", DialogMessage.Level.ERROR, 500, 170);
      StandaloneDialog.runLater(() -> StandaloneDialog.showErrorDialog(errMsg, null, e));
      synchronized (sync) {
        sync.notify();
      }
    }
  }

  @Override
  public void dispose() {
    webView.dispose();
  }

  @Override
  public boolean isOptional() {
    return true;
  }

}

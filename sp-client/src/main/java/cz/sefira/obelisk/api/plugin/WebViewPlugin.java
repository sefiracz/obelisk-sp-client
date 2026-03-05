package cz.sefira.obelisk.api.plugin;

/*
 * Copyright 2025 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.plugin.WebViewPlugin
 *
 * Created: 20/08/2025
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.plugin.webview.EdgeWebView;
import cz.sefira.obelisk.api.plugin.webview.JCEFWebView;
import cz.sefira.obelisk.api.plugin.webview.JavaFXWebView;
import cz.sefira.obelisk.api.plugin.webview.WebView;
import cz.sefira.obelisk.view.DialogMessage;
import cz.sefira.obelisk.view.StandaloneDialog;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.FutureTask;

public class WebViewPlugin extends CookiesPlugin {

  private static final Logger logger = LoggerFactory.getLogger(WebViewPlugin.class.getName());

  private WebView webView;

  @Override
  public List<InitErrorMessage> init(String pluginId, PlatformAPI api) {
    try {
      // Eclipse SWT WebView2 Edge
      try {
        webView = new EdgeWebView();
        webView.init(api);
        return List.of();
      } catch (Throwable t) {
        webView = null;
        logger.info("EdgeWebView not initialized: {}", t.getMessage());
      }

      // JCEF
      try {
        webView = new JCEFWebView();
        webView.init(api);
        return List.of();
      } catch (Throwable t) {
        webView = null;
        logger.info("JCEFWebView not initialized: {}", t.getMessage());
      }

      // JavaFX
      try {
        webView = new JavaFXWebView();
        FutureTask<List<InitErrorMessage>> futureTask = webView.futureInit(api);
        Platform.runLater(futureTask);
        return futureTask.get();
      } catch (Throwable t) {
        webView = null;
        logger.info("JavaFXWebView not initialized: {}", t.getMessage());
      }

      // check webview is initialized
      if (webView == null && isOptional()) {
        throw new OptionalPluginException("Optional plugin '"+ WebViewPlugin.class.getName()+"' not available");
      } else {
        throw new IllegalStateException("No WebView implementation initialized");
      }
    } catch (Throwable e) {
      if (e instanceof OptionalPluginException optional) {
        throw optional;
      }
      logger.error(e.getMessage(), e);
      return List.of(new InitErrorMessage(this.getClass().getSimpleName(), "error.application.init", e));
    }
  }

  @Override
  public void load(Object sync, PlatformAPI api, String url) {
    try {
      webView.load(sync, api, cookieStore, url);
    } catch (Throwable e) {
      logger.error("WebView failed to load URL "+url+": "+e.getMessage(), e);
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


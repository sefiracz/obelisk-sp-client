package cz.sefira.obelisk.api.plugin.webview;

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.model.OS;
import cz.sefira.obelisk.api.ws.ApacheCookieStore;
import cz.sefira.obelisk.api.ws.ssl.SSLCertificateProvider;
import cz.sefira.obelisk.prefs.PreferencesFactory;
import cz.sefira.obelisk.prefs.UserPreferences;
import cz.sefira.obelisk.util.HttpUtils;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.EnumProgress;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.apache.hc.client5.http.utils.DateUtils;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.*;
import org.cef.handler.*;
import org.cef.network.CefCookieManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocket;
import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import static org.cef.CefSettings.LogSeverity.LOGSEVERITY_FATAL;

public class JCEFWebView {

  private static final Logger logger = LoggerFactory.getLogger(JCEFWebView.class.getName());

  private boolean initialized = false;

  private CefClient cefClient;
  private CefBrowser cefBrowser;

  private JFrame browserWindow;
  private JTextField addressBar;
  private boolean browserFocus = true;

  public JCEFWebView() {}

  public void init(PlatformAPI api) throws Exception {
    CefAppBuilder builder = new CefAppBuilder();
    if (OS.isWindows() && System.getProperty("force.dynamic.libs") == null) {
      builder.setInstallDir(Paths.get(AppConfig.get().getWindowsInstalledPath(), "jcef-bundle").toFile());
    } else {
      builder.setInstallDir(AppConfig.get().getAppProcessDirectory().resolve("jcef-bundle").toFile());
    }

    builder.setProgressHandler((state, percent) -> {
      if (state != null) {
        if (EnumProgress.DOWNLOADING.equals(state) && percent >= 0 && percent <= 100 && percent%10 == 0) {
          logger.info("{} | {}", state, "Progress " + percent + "%");
        } else if (!EnumProgress.DOWNLOADING.equals(state)) {
          logger.info("{} | {}", state, "In progress...");
        }
      }
    });
    // builder.addJcefArgs("--disable-gpu"); //Just an example
    builder.getCefSettings().windowless_rendering_enabled = false;
    UserPreferences prefs = PreferencesFactory.getInstance(AppConfig.get());
    builder.getCefSettings().locale = prefs.getLanguage();
    builder.getCefSettings().log_severity = LOGSEVERITY_FATAL;
    Path jcefRootPath = AppConfig.get().getAppProcessDirectory().resolve("jcef_cache");
    builder.getCefSettings().root_cache_path = jcefRootPath.toFile().getAbsolutePath();
    builder.getCefSettings().cache_path = jcefRootPath.resolve("Cache Data").toFile().getAbsolutePath();
    CefApp.addAppHandler(new LanguageAwareCefAppHandlerAdapter());

    // build CefApp
    CefApp cefApp = builder.build();
    cefClient = cefApp.createClient();

    // create JFrame to hold JCEF browser
    browserWindow = new JFrame();
    try (InputStream in = AppConfig.get().getIconLogoStream()) {
      browserWindow.setIconImage(Toolkit.getDefaultToolkit().createImage(IOUtils.toByteArray(in)));
    } catch (IOException e) {
      logger.error("Unable to set browser window icon: {}", e.getMessage());
    }
    // URL bar
    addressBar = new JTextField(100);
    addressBar.setMargin(new Insets(0, 2, 0, 0));
    addressBar.setEditable(false);
    addressBar.addActionListener(e -> cefBrowser.loadURL(addressBar.getText()));

    // add request handler - handling SSL errors and cross-checking them with Java config
    cefClient.addRequestHandler(new CefRequestHandlerAdapter() {
      @Override
      public boolean onCertificateError(CefBrowser browser, CefLoadHandler.ErrorCode cert_error, String request_url, CefCallback callback) {
        System.out.println(request_url);
        // cross-check invalid SSL with Java SSL context configuration
        logger.error("Certificate error: {}", cert_error.name());
        if (sslPoke(request_url, api)) {
          logger.info("SSL provided trust");
          callback.Continue();
          return true;
        }
        return false;
      }
    });

    // add load handler - execute Javascript to obtain HTML of loaded page
    cefClient.addLoadHandler(new CefLoadHandlerAdapter() {
      @Override
      public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
        // loads HTML content of loaded page
        String jsCode = "window.cefQuery({request: document.documentElement.outerHTML});";
        browser.executeJavaScript(jsCode, browser.getURL(), 0);
      }
    });

    // add context menu handler - remove context menu buttons to simplify UI
    cefClient.addContextMenuHandler(new CefContextMenuHandlerAdapter() {

      @Override
      public void onBeforeContextMenu(CefBrowser browser, CefFrame frame, CefContextMenuParams params, CefMenuModel model) {
        model.clear();
      }

      @Override
      public boolean onContextMenuCommand(CefBrowser browser, CefFrame frame, CefContextMenuParams params, int commandId, int eventFlags) {
        return true;
      }
    });

    // add lifespan handler - to prevent new tab/window/popup and instead load the page in the browser main window
    cefClient.addLifeSpanHandler(new CefLifeSpanHandler() {
      @Override
      public boolean onBeforePopup(CefBrowser browser, CefFrame frame, String target_url, String target_frame_name) {
        browser.loadURL(target_url);
        return true;
      }

      @Override
      public void onAfterCreated(CefBrowser browser) {}

      @Override
      public void onAfterParentChanged(CefBrowser browser) {}

      @Override
      public boolean doClose(CefBrowser browser) {
        return false;
      }

      @Override
      public void onBeforeClose(CefBrowser browser) {}
    });

    // add display handler - to change URL in address bar and window title to correspond with the page title
    cefClient.addDisplayHandler(new CefDisplayHandlerAdapter() {
      @Override
      public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
        logger.info("Address change: {}", url);
        addressBar.setText(url);
      }

      @Override
      public void onTitleChange(CefBrowser browser, String title) {
        browserWindow.setTitle(title);
      }
    });

    // add focus listener
    addressBar.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        if (!browserFocus) return;
        browserFocus = false;
        KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
        addressBar.requestFocus();
      }
    });

    // add focus handler
    cefClient.addFocusHandler(new CefFocusHandlerAdapter() {
      @Override
      public void onGotFocus(CefBrowser browser) {
        if (browserFocus) return;
        browserFocus = true;
        KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
        browser.setFocus(true);
      }

      @Override
      public void onTakeFocus(CefBrowser browser, boolean next) {
        browserFocus = false;
      }
    });
  }

  public void load(Object sync, ApacheCookieStore cookieStore, String url) {
    logger.info("Loading webview URL: {}", url);
    // initialize listeners
    if (!initialized) {
      cefBrowser = cefClient.createBrowser(url, false, false);
      logger.info("Initializing JCEF window close handlers");
      CefMessageRouter msgRouter = CefMessageRouter.create();
      msgRouter.addHandler(new CefMessageRouterHandlerAdapter() {
        @Override
        public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
          // auto-hide window when the browser reaches recognizable OBELISK endpoint
          if (request != null && HttpUtils.isDiscoveryEndpoint(request)) {
            hideWindow();
            processCookies(cookieStore);
            synchronized (sync) {
              sync.notify();
            }
          }
          callback.success("Received successfully");
          return true;
        }
      }, true);
      cefClient.addMessageRouter(msgRouter);

      browserWindow.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          hideWindow();
          processCookies(cookieStore);
          synchronized (sync) {
            sync.notify();
          }
        }
      });

      browserWindow.getContentPane().add(addressBar, BorderLayout.NORTH);
      browserWindow.getContentPane().add(cefBrowser.getUIComponent(), BorderLayout.CENTER);
      initialized = true;
    } else {
      cefBrowser.loadURL(url);
    }

    addressBar.setText(url);
    browserWindow.pack();
    browserWindow.setSize(800, 600);
    browserWindow.setResizable(true);
    browserWindow.setVisible(true);
  }

  public void dispose() {
    logger.info("Dispose JCEF application");
    CefApp.getInstance().dispose();
  }

  /**
   * Hide browser window
   */
  private void hideWindow() {
    logger.info("Hide browser window");
    browserWindow.setVisible(false);
  }

  /**
   * Process all JCEF cookies and map them to Apache cookie store
   */
  private void processCookies(ApacheCookieStore cookieStore) {
    CefCookieManager cookieManager = CefCookieManager.getGlobalManager();
    cookieManager.visitAllCookies((cookie, count, total, deleteCookie) -> {
      BasicClientCookie c = new BasicClientCookie(cookie.name, cookie.value);
      c.setDomain(cookie.domain);
      c.setPath(cookie.path);
      c.setSecure(cookie.secure);
      c.setHttpOnly(cookie.httponly);
      c.setExpiryDate(cookie.hasExpires ? DateUtils.toInstant(cookie.expires) : null);
      c.setCreationDate(DateUtils.toInstant(cookie.creation));
      cookieStore.addCookie(c);
      return true; // return true to continue visiting, false to stop
    });
  }

  /**
   * SSL poke given URL to determine if the SSL context is working
   * @param url URL to poke
   * @param api PlatformAPI
   * @return True if SSL communication with given URL works
   */
  private boolean sslPoke(String url, PlatformAPI api) {
    URI uri = URI.create(url);
    SSLCertificateProvider sslProvider = api.getSslCertificateProvider();
    try (SSLSocket sslsocket = (SSLSocket) sslProvider.getSSLContext().getSocketFactory().createSocket(uri.getHost(), uri.getPort());
         InputStream in = sslsocket.getInputStream();
         OutputStream out = sslsocket.getOutputStream();) {
      out.write(1);
      while (in.available() > 0) {
        in.read();
      }
      logger.info("SSL poke succeeded");
      return true;
    } catch (Exception e) {
      logger.error("SSL poke failed: {}", e.getMessage());
    }
    return false;
  }

  /**
   * CEF AppHandler that sets accept-language to a value from UserPreferences
   */
  private static class LanguageAwareCefAppHandlerAdapter extends CefAppHandlerAdapter {

    public LanguageAwareCefAppHandlerAdapter() {
      super(null);
    }

    @Override
    public void stateHasChanged(org.cef.CefApp.CefAppState state) {
      // Shutdown the app if the native CEF part is terminated
      if (state == CefApp.CefAppState.TERMINATED) {
        logger.info("CefApp terminated");
        System.exit(0);
      }
    }

    @Override
    public void onBeforeCommandLineProcessing(String process_type, CefCommandLine command_line) {
      command_line.appendSwitchWithValue("accept-lang", buildAcceptLanguageHeader());
      super.onBeforeCommandLineProcessing(process_type, command_line);
    }


    private String buildAcceptLanguageHeader() {
      UserPreferences prefs = PreferencesFactory.getInstance(AppConfig.get());
      String lang = prefs.getLanguage();
      if (lang == null) {
        lang = Locale.getDefault().getLanguage();
      }
      String country;
      switch (lang) {
        case "en" -> country = "US";
        case "cs" -> country = "CZ";
        case "sk" -> country = "SK";
        default -> {
          lang = "en";
          country = "US";
        }
      }
      // Most specific: lang-COUNTRY
      StringBuilder sb = new StringBuilder();
      sb.append(lang).append("-").append(country);
      if (!"en".equals(lang)) {
        sb.append(",").append("en-US"); // fallback to english
      }
      return sb.toString();
    }
  }

}

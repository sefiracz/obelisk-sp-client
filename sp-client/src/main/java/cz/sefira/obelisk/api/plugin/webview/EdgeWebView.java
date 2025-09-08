package cz.sefira.obelisk.api.plugin.webview;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.model.OS;
import cz.sefira.obelisk.api.plugin.InitErrorMessage;
import cz.sefira.obelisk.api.plugin.webview.model.CookieDefinition;
import cz.sefira.obelisk.api.ws.ApacheCookieStore;
import cz.sefira.obelisk.prefs.PreferencesFactory;
import cz.sefira.obelisk.prefs.UserPreferences;
import cz.sefira.obelisk.util.HttpUtils;
import cz.sefira.obelisk.util.ResourceUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class EdgeWebView implements WebView {

  private static final Logger logger = LoggerFactory.getLogger(EdgeWebView.class.getName());

  private static final int FRAME_WIDTH = 1024;
  private static final int FRAME_HEIGHT = 768;

  private Display display;
  private Shell shell;
  private Browser browser;
  private Text addressBar;
  private Image image;

  private Map<String, CookieDefinition> cookieDefs = new HashMap<>();

  private boolean initialized;

  public void init(PlatformAPI api) throws IOException {
    Path webViewFixedVersion;
    if (OS.isWindows() && System.getProperty("force.dynamic.libs") == null) {
      webViewFixedVersion = Paths.get(AppConfig.get().getWindowsInstalledPath(), "Microsoft.WebView2.FixedVersionRuntime");
    } else {
      webViewFixedVersion = Paths.get("C:\\tmp\\Microsoft.WebView2.FixedVersionRuntime");
    }
    System.setProperty("org.eclipse.swt.browser.EdgeDir", webViewFixedVersion.toString());
    Path profile = AppConfig.get().getAppProcessDirectory().resolve("webview2_profile");
    System.setProperty("org.eclipse.swt.browser.EdgeDataDir", profile.toFile().getAbsolutePath());
    // System.setProperty("org.eclipse.swt.browser.EdgeArgs", "--disable-gpu");
    System.setProperty("org.eclipse.swt.browser.EdgeLanguage", buildAcceptLanguageHeader());
    System.setProperty("org.eclipse.swt.browser.Edge.allowSingleSignOnUsingOSPrimaryAccount", "true");

    // load cookie definitions
    cookieDefs = processCookiesDef();

    ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.execute(() -> {
      try {
        logger.info("Initialization of Edge WebView2 started...");
        display = new Display();
        // shell
        shell = new Shell(display);
        shell.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - FRAME_WIDTH) / 2;
        int y = (screenSize.height - FRAME_HEIGHT) / 2;
        shell.setLocation(x, y);
        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        gridLayout.verticalSpacing = 0;
        shell.setLayout(gridLayout);

        // Address bar
        addressBar = new Text(shell, SWT.BORDER);
        addressBar.setEditable(false);
        addressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Browser
        browser = new Browser(shell, SWT.EDGE);
        browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        browser.addTitleListener(event -> shell.setText(event.title));
        browser.addOpenWindowListener(event -> event.browser = browser);

        // set icon
        try (InputStream in = AppConfig.get().getIconLogoStream()) {
          image = new Image(display, in);
          shell.setImage(image);
        } catch (IOException e) {
          logger.error("Unable to set browser window icon: {}", e.getMessage());
        }

        // loading engine placeholder html page
        try (InputStream loading = Thread.currentThread().getContextClassLoader().getResourceAsStream("loading.html")) {
          assert loading != null;
          String loadingMessage = ResourceUtils.getBundle().getString("webview.load.init");
          String loadingPage = IOUtils.toString(loading, StandardCharsets.UTF_8);
          loadingPage = loadingPage.replace("##webview.load.init##", loadingMessage);
          browser.setText(loadingPage);
        } catch (IOException e) {
          logger.error(e.getMessage(), e);
          // fallback page
          browser.setText("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">" +
              "<title>OBELISK Signing Portal Client | Initializing browser engine...</title></head>" +
              "<body><div class=\"message\">Probíhá inicializace prohlížeče, čekejte prosím...</div></body></html>");
        }

        shell.open();
        shell.setVisible(false);

        logger.info("Initialization of Edge WebView2 finished...");

        while (!shell.isDisposed()) {
          if (!display.readAndDispatch()) {
            display.sleep();
          }
        }

      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    });
  }

  @Override
  public FutureTask<List<InitErrorMessage>> futureInit(PlatformAPI api) {
    throw new UnsupportedOperationException("Not supported.");
  }

  public void dispose() {
    logger.info("Edge dispose");
    // clean up
    Display.getDefault().asyncExec(() -> {
      if (image != null) {
        image.dispose();
      }
      browser.dispose();
      shell.dispose();
      display.dispose();
    });
  }

  public void load(Object sync, PlatformAPI api, ApacheCookieStore cookieStore, String url) {
    Display.getDefault().asyncExec(() -> {
      try {
        if (!initialized) {
          shell.addListener(SWT.Close, event -> {
            event.doit = false; // cancel the dispose
            logger.info("User closed the browser");
            synchronized (sync) {
              sync.notify();
              shell.setVisible(false);
            }
          });

          browser.addProgressListener(new ProgressAdapter() {
            public void completed(ProgressEvent event) {
              Browser browser = (Browser) event.widget;
              String html = browser.getText();
              String url = browser.getUrl();
              addressBar.setText(url);

              // process defined cookies
              getAllCookies(url, cookieStore);

              // auto-hide window when the browser reaches recognizable OBELISK endpoint
              if (HttpUtils.isDiscoveryEndpoint(html)) {
                synchronized (sync) {
                  sync.notify();
                  shell.setVisible(false);
                }
              }

            }
          });

          initialized = true;
        }
        logger.info("Loading webview URL: {}", url);
        browser.setUrl(url);
        shell.setVisible(true);
        Thread.sleep(100);
        shell.forceFocus();
        shell.forceActive();
      } catch (Exception e) {
        logger.error("Webview failed: " + e.getMessage(), e);
      }
    });
  }

  private void getAllCookies(String url, ApacheCookieStore cookieStore) {
    try {
      String cookieValue = Browser.getCookies(url); // custom patched method, fails if not present, fallback to json defs
      if (cookieValue == null || cookieValue.isEmpty())
        return;
      String[] cookies = cookieValue.split("\n");
      for (String c : cookies) {
        List<Cookie> cookieList = ApacheCookieStore.ApacheCookieManager.parseCookie(url, c);
        for (Cookie cookie : cookieList) {
          logger.info("Obtained cookie: {}", cookie.getName());
          cookieStore.addCookie(cookie);
        }
      }
    } catch (Exception e) {
      logger.error("Unable to process cookies for URL '"+url+"': " + e.getMessage(), e);
      // fallback, try to use manually created cookie definitions
      logger.warn("Using fallback cookie definitions.");
      cookieDefs.forEach((name, value) -> addCookie(name, url, value, cookieStore));
    }
  }

  private void addCookie(String cookieName, String url, CookieDefinition def, ApacheCookieStore cookieStore) {
    try {
      String cookieValue = Browser.getCookie(cookieName, url);
      if (cookieValue == null || cookieValue.isEmpty())
        return;
      logger.info("Obtained cookie: {}", cookieValue);
      URL u = new URL(url);
      String host = u.getHost();
      String protocol = u.getProtocol();
      BasicClientCookie c = new BasicClientCookie(cookieName, cookieValue);
      c.setDomain(def.getDomain() != null ? def.getDomain() : host);
      c.setPath(def.getPath() != null ? def.getPath() : "/");
      c.setSecure(def.getSecure() != null ? def.getSecure() : (protocol != null && protocol.equalsIgnoreCase("https")));
      c.setHttpOnly(def.getHttpOnly() != null ? def.getHttpOnly() : true);
      cookieStore.addCookie(c);
    } catch (Exception e) {
      logger.error("Unable to process cookie '" + cookieName + "' for URL '"+url+"': " + e.getMessage(), e);
    }
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
    return lang + "-" + country;
  }

  private Map<String, CookieDefinition> processCookiesDef() {
    // load default definition values
    Path defaultCookiesDef = AppConfig.get().getDefaultUserConfigDir().resolve("cookies_definition.json");
    Map<String, CookieDefinition> cookies = new HashMap<>(loadCookieJson(defaultCookiesDef));
    // load user defined values
    Path userCookiesDef = Paths.get(AppConfig.get().getAppUserHome().getAbsolutePath()).resolve("cookies_definition.json");
    cookies.putAll(loadCookieJson(userCookiesDef));
    // if empty, load fallback values
    if (cookies.isEmpty()) {
      try (InputStream jsonDef = Thread.currentThread().getContextClassLoader().getResourceAsStream("cookies_definition_fallback.json")) {
        assert jsonDef != null;
        byte[] json = jsonDef.readAllBytes();
        Type mapType = new TypeToken<Map<String, CookieDefinition>>() {
        }.getType();
        return new Gson().fromJson(new String(json, StandardCharsets.UTF_8), mapType);
      } catch (IOException e) {
        logger.error("Unable to read fallback cookie definitions: " + e.getMessage(), e);
      }
    }
    return cookies;
  }

  private Map<String, CookieDefinition> loadCookieJson(Path json) {
    if (json.toFile().exists()) {
      try (InputStream in = Files.newInputStream(json)) {
        Type mapType = new TypeToken<Map<String, CookieDefinition>>() {
        }.getType();
        return new Gson().fromJson(new String(in.readAllBytes(), StandardCharsets.UTF_8), mapType);
      } catch (Exception e) {
        logger.error("Unable to load cookies definition json: " + e.getMessage(), e);
      }
    }
    return new HashMap<>();
  }

}

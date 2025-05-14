package cz.sefira.obelisk.api.ws;

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.model.OS;
import cz.sefira.obelisk.api.plugin.DataProtectionPlugin;
import org.apache.hc.client5.http.cookie.*;
import org.apache.hc.client5.http.impl.cookie.RFC6265LaxSpec;
import org.apache.hc.core5.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApacheCookieManager extends CookieManager {

  private static final Logger logger = LoggerFactory.getLogger(ApacheCookieManager.class.getName());

  private static final String SET_COOKIE = "set-cookie";
  private static final String SET_COOKIE2 = "set-cookie2";

  private static final String LOGIN_MICROSOFT = "https://login.microsoftonline.com/";
  private static final String SESSION_COOKIE = "ESTSAUTHPERSISTENT";

  private final CookieStore basicCookieStore = new BasicCookieStore();
  private final CookieSpec cookieSpec = new RFC6265LaxSpec();
  private final Path persistentStore;

  private PlatformAPI api = null;

  public ApacheCookieManager(CookiePolicy cookiePolicy) {
    super(null, cookiePolicy);
    try {
      this.persistentStore = AppConfig.get().getAppStorageDirectory().resolve(".local_storage");
      try {
        if (!persistentStore.toFile().exists()) {
          boolean created = persistentStore.toFile().createNewFile();
          if (OS.isWindows()) {
            Files.setAttribute(persistentStore, "dos:hidden", true, LinkOption.NOFOLLOW_LINKS);
          }
        }
      } catch (Exception e) {
        logger.warn("Could not create persistent store at: "+persistentStore, e);
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) throws IOException {
    secureLoad(uri); // load raw cookie headers for given URI

    return super.get(uri, requestHeaders);
  }

  @Override
  public void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException {
    super.put(uri, responseHeaders);
    basicCookieStore.clearExpired(Instant.now());

    for (String hKey : responseHeaders.keySet()) {
      if (hKey == null || !(hKey.equalsIgnoreCase(SET_COOKIE2) || hKey.equalsIgnoreCase(SET_COOKIE))) {
        continue;
      }
      for (String headerValue : responseHeaders.get(hKey)) {
        secureStore(uri, headerValue); // store raw cookie header

        List<Cookie> cookies = parseCookie(uri, headerValue);
        for (Cookie cookie : cookies) {
          if (logger.isDebugEnabled()) {
            logger.debug("Adding to cookie store - {}", cookie.toString());
          }
          basicCookieStore.addCookie(cookie);
        }
      }
    }
  }

  private void secureStore(URI uri, String headerValue) {
    // store cookies for MSO login page
    if (api != null && api.getPlugin(DataProtectionPlugin.class) != null) {
      DataProtectionPlugin dataProtection = (DataProtectionPlugin) api.getPlugin(DataProtectionPlugin.class);
      if (headerValue.startsWith(SESSION_COOKIE) && uri.toString().startsWith(LOGIN_MICROSOFT)
          && persistentStore.toFile().exists()) {
        try (OutputStream out = Files.newOutputStream(persistentStore)) {
          // encrypt and store cookie securely
          out.write(dataProtection.encryptData(headerValue.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
        }
      }
    }
  }

  private void secureLoad(URI uri) {
    // load cookies for MSO login page
    if (api != null && api.getPlugin(DataProtectionPlugin.class) != null) {
      DataProtectionPlugin dataProtection = (DataProtectionPlugin) api.getPlugin(DataProtectionPlugin.class);
      if (uri.toString().startsWith(LOGIN_MICROSOFT) && persistentStore.toFile().exists()) {
        try (InputStream in = Files.newInputStream(persistentStore)) {
          // decrypt protected cookie
          String cookie = new String(dataProtection.decryptData(in.readAllBytes()), StandardCharsets.UTF_8);
          if (cookie.startsWith(SESSION_COOKIE)) {
            Map<String, List<String>> setHeaders = new HashMap<>();
            setHeaders.put(SET_COOKIE, List.of(cookie));
            super.put(uri, setHeaders);
          }
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
        }
      }
    }
  }

  private List<Cookie> parseCookie(URI uri, String cookieHeader) {
    boolean secure = "https".equalsIgnoreCase(uri.getScheme());
    int port = (uri.getPort() < 0) ? (secure ? 443 : 80) : uri.getPort();
    CookieOrigin origin = new CookieOrigin(uri.getHost(), port, uri.getPath(), secure);
    BasicHeader header = new BasicHeader(SET_COOKIE, cookieHeader);
    try {
      return cookieSpec.parse(header, origin);
    } catch (MalformedCookieException e) {
      throw new RuntimeException(e);
    }
  }

  public void setPlatformAPI(PlatformAPI api) {
    this.api = api;
  }

  public CookieStore getApacheCookieStore() {
    return basicCookieStore;
  }
}
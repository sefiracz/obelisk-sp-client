package cz.sefira.obelisk.api.ws;

import org.apache.hc.client5.http.cookie.*;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.cookie.RFC6265LaxSpec;
import org.apache.hc.core5.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static java.net.CookiePolicy.ACCEPT_ALL;

public class ApacheCookieStore {

  private static final Logger logger = LoggerFactory.getLogger(ApacheCookieStore.class.getName());

  private final CookieStore basicCookieStore = new BasicCookieStore();

  public ApacheCookieStore() {
    ApacheCookieManager manager = new ApacheCookieManager(basicCookieStore, ACCEPT_ALL);
    CookieHandler.setDefault(manager);
  }

  public void addCookie(Cookie cookie) {
    if (logger.isDebugEnabled()) {
      logger.debug("Adding to cookie store - {}", cookie.toString());
    }
    basicCookieStore.addCookie(cookie);
  }

  public CookieStore getCookieStore() {
    return basicCookieStore;
  }

  public static class ApacheCookieManager extends CookieManager {

    private static final Logger logger = LoggerFactory.getLogger(ApacheCookieManager.class.getName());
    private static final CookieSpec cookieSpec = new RFC6265LaxSpec();

    private static final String SET_COOKIE = "set-cookie";
    private static final String SET_COOKIE2 = "set-cookie2";

    private final CookieStore basicCookieStore;

    public ApacheCookieManager(CookieStore basicCookieStore, CookiePolicy cookiePolicy) {
      super(null, cookiePolicy);
      this.basicCookieStore = basicCookieStore;
    }

    @Override
    public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) throws IOException {
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

    private static List<Cookie> parseCookie(URI uri, String cookieHeader) {
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

    public static List<Cookie> parseCookie(String url, String cookieHeader) {
      try {
        URL uri = new URL(url);
        return parseCookie(uri.toURI(), cookieHeader);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

}
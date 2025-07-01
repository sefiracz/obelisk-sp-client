package cz.sefira.obelisk.api.plugin;

import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.ws.ApacheCookieStore;
import cz.sefira.obelisk.api.ws.HttpMethod;
import cz.sefira.obelisk.api.ws.SpApiClient;
import cz.sefira.obelisk.api.ws.ssl.HttpResponse;
import cz.sefira.obelisk.api.ws.ssl.SSLCommunicationException;
import cz.sefira.obelisk.util.HttpUtils;
import org.apache.hc.core5.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Abstract plugin for obtaining cookies for communication using {@link cz.sefira.obelisk.api.ws.ssl.HttpsClient}
 * Apache HTTP client
 */
public abstract class CookiesPlugin implements AppPlugin {

  private static final Logger logger = LoggerFactory.getLogger(CookiesPlugin.class.getName());

  protected final ApacheCookieStore cookieStore;

  public CookiesPlugin() {
    this.cookieStore = new ApacheCookieStore();
  }

  /**
   * Check if the statusPage is reachable
   * @param client SP-API HTTP client
   * @param statusPage Status page URL to reach
   * @return True if URL can be reached
   * @throws SSLCommunicationException
   */
  public boolean checkAccess(SpApiClient client, String statusPage) throws SSLCommunicationException {
    try {
      logger.info("Checking status page accessibility");
      HttpResponse response = client.call(HttpMethod.GET, statusPage);
      int responseCode = response.getCode();
      if (responseCode == HttpStatus.SC_OK) {
        String html = new String(response.getContent(), StandardCharsets.UTF_8);
        if (HttpUtils.isDiscoveryEndpoint(html)) {
          return true;
        }
      }
      logger.info("Status page not accessible: {}", responseCode);
      return false;
    } catch (SSLCommunicationException e) {
      logger.error("Status page SSL error: {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      logger.error("Status page access check failed: "+e.getMessage(), e);
    }
    return false;
  }

  /**
   * Abstract method that loads the cookies into the cookie manager from given URL
   * @param sync Sync object which the main threads waits on, this method has to implement notify()
   *             after is does all the work and wishes the main thread to continue with obtained cookies.
   *             Call {@code synchronized(sync) { sync.notify(); } } when this method done its job.
   * @param api PlatformAPI
   * @param url URL from where to obtain cookies
   */
  public abstract void load(Object sync, PlatformAPI api, String url);

  public ApacheCookieStore getCookieStore() {
    return cookieStore;
  }

  public abstract void dispose();
}

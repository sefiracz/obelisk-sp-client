package cz.sefira.obelisk.api.plugin;

import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.ws.ApacheCookieManager;
import cz.sefira.obelisk.api.ws.SpApiClient;
import cz.sefira.obelisk.api.ws.ssl.SSLCommunicationException;

import java.net.CookieHandler;

import static java.net.CookiePolicy.ACCEPT_ALL;

/**
 * Abstract plugin for obtaining cookies for communication using {@link cz.sefira.obelisk.api.ws.ssl.HttpsClient}
 * Apache HTTP client
 */
public abstract class CookiesPlugin implements AppPlugin {

  protected final ApacheCookieManager manager;

  public CookiesPlugin() {
    this.manager = new ApacheCookieManager(ACCEPT_ALL);
    CookieHandler.setDefault(manager);
  }

  public abstract boolean checkAccess(SpApiClient client, String statusPage) throws SSLCommunicationException;

  /**
   * Abstract method that loads the cookies into the cookie manager from given URL
   * @param sync Sync object which the main threads waits on, this method has to implement notify()
   *             after is does all the work and wishes the main thread to continue with obtained cookies.
   *             Call {@code synchronized(sync) { sync.notify(); } } when this method done its job.
   * @param api PlatformAPI
   * @param url URL from where to obtain cookies
   */
  public abstract void load(Object sync, PlatformAPI api, String url);

  protected void setPlatformAPI(PlatformAPI api) {
    manager.setPlatformAPI(api);
  }

  public ApacheCookieManager getCookieManager() {
    return manager;
  }

}

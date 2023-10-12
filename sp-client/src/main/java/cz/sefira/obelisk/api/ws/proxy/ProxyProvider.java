/**
 * © SEFIRA spol. s r.o., 2020-2023
 * <p>
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 * <p>
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 * <p>
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package cz.sefira.obelisk.api.ws.proxy;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.ws.ProxyProvider
 *
 * Created: 16/08/2023
 * Author: hlavnicka
 */

import com.github.markusbernhardt.proxy.ProxySearch;
import com.github.markusbernhardt.proxy.ProxySearchStrategy;
import com.github.markusbernhardt.proxy.search.browser.firefox.FirefoxProxySearchStrategy;
import com.github.markusbernhardt.proxy.search.browser.ie.IEProxySearchStrategy;
import com.github.markusbernhardt.proxy.search.desktop.gnome.GnomeDConfProxySearchStrategy;
import com.github.markusbernhardt.proxy.search.desktop.gnome.GnomeProxySearchStrategy;
import com.github.markusbernhardt.proxy.search.desktop.kde.KdeProxySearchStrategy;
import com.github.markusbernhardt.proxy.search.desktop.osx.OsxProxySearchStrategy;
import com.github.markusbernhardt.proxy.search.desktop.win.WinProxySearchStrategy;
import com.github.markusbernhardt.proxy.search.env.EnvProxySearchStrategy;
import com.github.markusbernhardt.proxy.search.java.JavaProxySearchStrategy;
import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.model.OS;
import cz.sefira.obelisk.prefs.PreferencesFactory;
import cz.sefira.obelisk.prefs.UserPreferences;
import cz.sefira.obelisk.util.LogUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.core5.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Proxy provider
 */
public class ProxyProvider {

  private static final Logger logger = LoggerFactory.getLogger(ProxyProvider.class.getName());
  private static final Object sync = new Object();

  private static final ProxySearchStrategy[] WIN_STRATS = {
      new IEProxySearchStrategy(),
      new FirefoxProxySearchStrategy(),
      new WinProxySearchStrategy(),
      new EnvProxySearchStrategy(),
      new JavaProxySearchStrategy()
  };

  private static final ProxySearchStrategy[] MACOS_STRATS = {
      new OsxProxySearchStrategy(),
      new FirefoxProxySearchStrategy(),
      new EnvProxySearchStrategy(),
      new JavaProxySearchStrategy()
  };

  private static final ProxySearchStrategy[] LINUX_STRATS = {
      new KdeProxySearchStrategy(),
      new GnomeProxySearchStrategy(),
      new GnomeDConfProxySearchStrategy(),
      new FirefoxProxySearchStrategy(),
      new EnvProxySearchStrategy(),
      new JavaProxySearchStrategy()
  };

  private final Set<Proxy> proxies = new LinkedHashSet<>();

  private UserPreferences prefs;
  private ProxySetup setup = null;
  private boolean initFlag = false;

  public ProxyProvider() {}

  public void setupProxy(HttpUriRequestBase request, HttpClientBuilder clientBuilder) throws URISyntaxException {
    synchronized (sync) {
      this.prefs = PreferencesFactory.getInstance(AppConfig.get());
      if (setup == null || !initFlag) {
        if (prefs.isUseSystemProxy()) {
          setup = detectSystemProxy(request.getUri());
        }
        if (setup == null || (Proxy.Type.HTTP.equals(setup.getType()) && (StringUtils.isBlank(setup.getProxyHost()) || setup.getProxyPort() == null || setup.getUseHttps() == null))) {
          setup = preferencesProxy();
        }
      }
      if (setup != null && StringUtils.isNotBlank(setup.getProxyHost())) {
        HttpHost proxy = new HttpHost(setup.getUseHttps() ? "https" : "http", setup.getProxyHost(), setup.getProxyPort());
        logger.info("Using proxy: "+proxy);
        clientBuilder.setRoutePlanner(new DefaultProxyRoutePlanner(proxy));
        CredentialsProvider credentialsProvider = getProxyCredentialsProvider(proxy);
        if (credentialsProvider != null) {
          clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }
      }
    }
  }

  /**
   * Set flag when proxy settings are confirmed working
   * @param initFlag True when setup is working, false when proxy setup needs to reinitialize
   */
  public void setInitFlag(boolean initFlag) {
    synchronized (sync) {
      if (setup != null && !initFlag) {
        logger.info("Resetting proxy setup");
        this.setup = null;
      }
      this.initFlag = initFlag;
    }
  }

  private ProxySetup detectSystemProxy(URI uri) {
    proxies.clear(); // clear proxy list
    ProxySearchStrategy[] strategies = OS.isWindows() ? WIN_STRATS : OS.isMacOS() ? MACOS_STRATS : LINUX_STRATS;
    for (ProxySearchStrategy strategy : strategies) {
      try (LogUtils.Time time = new LogUtils.Time("Proxy strategy "+strategy.getName()+" loaded in", true)) {
        final ProxySearch ps = new ProxySearch();
        ps.addStrategy(strategy, false);
        final ProxySelector proxySelector = ps.getProxySelector();
        if (proxySelector == null) {
          if (logger.isDebugEnabled()) {
            logger.debug("Proxy strategy: " + strategy.getName() + " - no settings available");
          }
          continue;
        }
        List<Proxy> proxyList = proxySelector.select(uri);
        logger.info("Proxy strategy: " + strategy.getName() + " - found: "+proxyList);
        proxies.addAll(proxyList);
      } catch (Exception e) {
        logger.error("Failed to process proxy settings for strategy "+ strategy.getName()+": "+e.getMessage(), e);
      }
    }
    if (proxies.isEmpty()){
      return null;
    } else {
      if (proxies.size() > 1) {
        logger.warn("Multiple different proxy settings found.");
        proxies.forEach(p -> logger.warn("Proxy: " + p.toString()));
      }
      Proxy proxy = proxies.stream().findFirst().get(); // TODO - heuristic to choose the best?
      return selectProxy(proxy);
    }
  }

  private ProxySetup selectProxy(Proxy proxy) {
    if (Proxy.Type.DIRECT.equals(proxy.type())) {
      return new ProxySetup();
    } else if (Proxy.Type.HTTP.equals(proxy.type())) {
      if (proxy.address() instanceof InetSocketAddress) {
        InetSocketAddress address = (InetSocketAddress) proxy.address();
        return new ProxySetup(address.getHostName(), address.getPort(), prefs.isProxyUseHttps());
      }
    } else {
      logger.error("SOCKS proxy not supported");
    }
    return null;
  }

  private ProxySetup preferencesProxy() {
    if (StringUtils.isNotBlank(prefs.getProxyServer())) {
      try {
        String proxyServer = prefs.getProxyServer();
        Integer proxyPort = prefs.getProxyPort();
        if (!proxyServer.contains("//")) // fill in protocol separator
          proxyServer = "//" + proxyServer;
        if (proxyPort != null) // fill in port
          proxyServer += ":" + proxyPort;
        final URI proxyUri = URI.create(proxyServer);
        if (proxyUri.getHost() == null) {
          logger.error("Proxy server " + proxyServer + " is not valid URI");
          return null;
        }
        // http/https
        boolean useHttps = false;
        if (proxyUri.getScheme() == null) {
          useHttps = prefs.isProxyUseHttps();
        } else if ("https".equalsIgnoreCase(proxyUri.getScheme())) {
          useHttps = true;
        }
        return new ProxySetup(proxyUri.getHost(), proxyUri.getPort(), useHttps);
      } catch (Exception e) {
        logger.error("Ignoring proxy config: "+e.getMessage(), e);
      }
    }
    return null;
  }

  private CredentialsProvider getProxyCredentialsProvider(HttpHost proxy) {
    BasicCredentialsProvider credsProvider = null;
    if(proxy != null && prefs.isProxyAuthentication()) {
      credsProvider = new BasicCredentialsProvider();
      String username = prefs.getProxyUsername();
      char[] password = prefs.getProxyPassword() != null ? prefs.getProxyPassword().toCharArray() : null;
      credsProvider.setCredentials(
          new AuthScope(proxy.getHostName(), proxy.getPort()),
          new UsernamePasswordCredentials(username, password)
      );
    }
    return credsProvider;
  }

  private static class ProxySetup {

    private final Proxy.Type type;
    private final String proxyHost;
    private final Integer proxyPort;
    private final Boolean useHttps;

    public ProxySetup() {
      this.type = Proxy.Type.DIRECT;
      this.proxyHost = null;
      this.proxyPort = null;
      this.useHttps = null;
    }

    public ProxySetup(String proxyHost, Integer proxyPort, Boolean useHttps) {
      this.type = Proxy.Type.HTTP;
      this.proxyHost = proxyHost;
      this.proxyPort = proxyPort;
      this.useHttps = useHttps;
    }

    public Proxy.Type getType() {
      return type;
    }

    public String getProxyHost() {
      return proxyHost;
    }

    public Integer getProxyPort() {
      return proxyPort;
    }

    public Boolean getUseHttps() {
      return useHttps;
    }
  }

}

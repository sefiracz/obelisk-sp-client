package cz.sefira.obelisk.api.plugin;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.plugin.SSLPlugin
 *
 * Created: 07.02.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.ws.ssl.SSLCertificateProvider;
import cz.sefira.obelisk.storage.SSLCacheStorage;
import cz.sefira.obelisk.storage.model.CertificateChain;
import cz.sefira.obelisk.util.X509Utils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * SSL trust plugin
 */
public class SSLPlugin implements AppPlugin {

  private static final Logger logger = LoggerFactory.getLogger(SSLPlugin.class.getName());

  @Override
  public List<InitErrorMessage> init(String pluginId, PlatformAPI api) {
    SSLCacheStorage cache = api.getStorageHandler().getSslCacheStorage();
    SSLCertificateProvider sslProvider = new SSLCertificateProvider(cache);
    try {
      // load application local truststore
      KeyStore truststore = KeyStore.getInstance("JKS");
      try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("cacerts.jks")) {
        InputStream byteStream = null;
        if (in != null) {
          byte[] bytes = IOUtils.toByteArray(in);
          byteStream = new ByteArrayInputStream(bytes);
        } else {
          logger.error("Unable to load cacerts.jks");
        }
        truststore.load(byteStream, "zx9h6$Cs39CV7DSf#@6d".toCharArray());
      }
      int count = 0;
      Enumeration<String> aliases = truststore.aliases();
      while (aliases.hasMoreElements()) {
        sslProvider.put((X509Certificate) truststore.getCertificate(aliases.nextElement()));
        count++;
      }
      logger.info("Loaded "+count+" SSL certificates from embedded truststore");

      // load SSL certificates from OS specific stores
      X509Utils.loadSSLCertificates(truststore, sslProvider);

      // establish trust store as SSL cert source
      sslProvider.setTrustStore(truststore);

      // add intermediate certificates from cache that complete chains to trusted anchors
      try {
        List<CertificateChain> remove = new ArrayList<>();
        for (CertificateChain chain : cache.getAll()) {
          if (!sslProvider.addTrustedChain(chain.getCertificateChain(), false)) {
            remove.add(chain);
          }
        }
        if (remove.size() > 0) {
          logger.info("Removing " + remove.size() + " from SSL cache");
        }
        // remove untrusted certificates from cache
        for (CertificateChain r : remove) {
          cache.remove(r);
        }
      } catch (Exception e) {
        logger.error("Unable to load SSL cache: "+e.getMessage(), e);
      }

      api.setSslCertificateProvider(sslProvider);
      logger.info("Trusted SSL certificates in total: "+sslProvider.getUnique().size());
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return List.of(new InitErrorMessage(this.getClass().getSimpleName(), "error.install.ssl.cert.message", e));
    }
    return Collections.emptyList();
  }

}

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

import cz.sefira.crypto.MSCryptoStore;
import cz.sefira.crypto.StoreType;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.ws.ssl.SSLCertificateProvider;
import cz.sefira.obelisk.api.model.OS;
import cz.sefira.obelisk.storage.SSLCacheStorage;
import cz.sefira.obelisk.storage.model.CertificateChain;
import cz.sefira.obelisk.util.X509Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * SSL trust plugin
 */
public class SSLPlugin implements AppPlugin {

  private static final Logger logger = LoggerFactory.getLogger(SSLPlugin.class.getName());

  @Override
  public List<InitErrorMessage> init(String pluginId, PlatformAPI api) {
    SSLCacheStorage cache = api.getStorageHandler().getSslCacheStorage();
    SSLCertificateProvider sslCertProvider = new SSLCertificateProvider(cache);
    try {
      // load application local truststore
      KeyStore truststore = KeyStore.getInstance("JKS");
      try (InputStream in = SSLPlugin.class.getResourceAsStream("cacerts.jks")) {
        truststore.load(in, "zx9h6$Cs39CV7DSf#@6d".toCharArray());
      }
      Enumeration<String> aliases = truststore.aliases();
      while (aliases.hasMoreElements()) {
        sslCertProvider.put((X509Certificate) truststore.getCertificate(aliases.nextElement()));
      }
      KeyStore systemStore = null;

      // load up Windows trusted certificates
      if (OS.isWindows()) {
        // load Java MSCAPI - ROOT store
        systemStore = KeyStore.getInstance("Windows-ROOT");

        // load native MSCAPI - CA store
        try {
          List<Certificate> caList = MSCryptoStore.getCertificates(StoreType.CA);
          for (Certificate ca : caList) {
            X509Utils.addToTrust((X509Certificate) ca, truststore, sslCertProvider);
          }
        }
        catch (Exception e) {
          logger.error(e.getMessage(), e);
        }
      }
      // load up MacOS trusted certificates
      if (OS.isMacOS()) {
        systemStore = KeyStore.getInstance("KeychainStore");
      }

      // load up Linux trusted certificates
      if (OS.isLinux()) {
        try (Stream<Path> list = Files.list(Paths.get("/etc/ssl/certs"))) {
          List<Path> certificates = list.filter(Files::isRegularFile).collect(Collectors.toList());
          for (Path certPath : certificates) {
            try (InputStream in = Files.newInputStream(certPath)) {
              X509Certificate certificate = X509Utils.getCertificateFromStream(in);
              X509Utils.addToTrust(certificate, truststore, sslCertProvider);
            } catch (Exception e) {
              logger.error(e.getMessage());
            }
          }
        } catch (Exception e) {
          logger.error("Unable to load /etc/ssl/certs: "+e.getMessage());
        }
      }

      // load SSL certificates from system store (Win/Mac)
      if (systemStore != null) {
        systemStore.load(null, null);
        Enumeration<String> trustAliases = systemStore.aliases();
        while (trustAliases.hasMoreElements()) {
          String alias = trustAliases.nextElement();
          Certificate ca = systemStore.getCertificate(alias);
          X509Utils.addToTrust((X509Certificate) ca, truststore, sslCertProvider);
        }
      }
      // establish trust store as SSL cert source
      sslCertProvider.setTrustStore(truststore);

      // add intermediate certificates from cache that complete chains to trusted anchors
      try {
        List<CertificateChain> remove = new ArrayList<>();
        for (CertificateChain chain : cache.getAll()) {
          if (!sslCertProvider.addTrustedChain(chain.getCertificateChain(), false)) {
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

      api.setSslCertificateProvider(sslCertProvider);
      logger.info("Added trusted SSL certificates: "+sslCertProvider.getUnique().size());
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return List.of(new InitErrorMessage(this.getClass().getSimpleName(), "error.install.ssl.cert.message", e));
    }
    return Collections.emptyList();
  }

}

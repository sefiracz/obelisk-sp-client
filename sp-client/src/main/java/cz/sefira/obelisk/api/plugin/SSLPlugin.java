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

import com.sun.jna.Platform;
import cz.sefira.crypto.MSCryptoStore;
import cz.sefira.crypto.StoreType;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.ws.ssl.SSLCertificateProvider;
import cz.sefira.obelisk.api.model.OS;
import org.apache.hc.client5.http.utils.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * SSL trust plugin
 */
public class SSLPlugin implements AppPlugin {

  private static final Logger logger = LoggerFactory.getLogger(SSLPlugin.class.getName());

  @Override
  public List<InitErrorMessage> init(String pluginId, PlatformAPI api) {
    SSLCertificateProvider sslCertProvider = new SSLCertificateProvider();
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
            String alias = Hex.encodeHexString(MessageDigest.getInstance("SHA-1").digest(ca.getEncoded()));
            sslCertProvider.put((X509Certificate) ca);
            truststore.setCertificateEntry(alias, ca);
          }
        }
        catch (Exception e) {
          logger.error(e.getMessage(), e);
        }
      }
      // load up MacOS trusted certificates
      if (Platform.isMac()) {
        systemStore = KeyStore.getInstance("KeychainStore");
      }

      // load SSL certificates from system store
      if (systemStore != null) {
        systemStore.load(null, null);
        Enumeration<String> trustAliases = systemStore.aliases();
        while (trustAliases.hasMoreElements()) {
          String alias = trustAliases.nextElement();
          Certificate ca = systemStore.getCertificate(alias);
          sslCertProvider.put((X509Certificate) ca);
          truststore.setCertificateEntry(alias, ca);
        }
      }

      // TODO - get intermediate certificates from DB
      // TODO - re-check trusted chain
      // TODO - add to trustStore/certificatePool

      sslCertProvider.setTrustStore(truststore);
      api.setSslCertificateProvider(sslCertProvider);
      logger.info("Added trusted SSL certificates: "+sslCertProvider.getUnique().size());
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return List.of(new InitErrorMessage(this.getClass().getSimpleName(), "error.install.ssl.cert.message", e));
    }
    return Collections.emptyList();
  }
}

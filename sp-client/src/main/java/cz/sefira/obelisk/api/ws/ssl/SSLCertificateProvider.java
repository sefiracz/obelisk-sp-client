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
package cz.sefira.obelisk.api.ws.ssl;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.ssl.CertificatePool
 *
 * Created: 07.02.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.dss.DigestAlgorithm;
import cz.sefira.obelisk.storage.SSLCacheStorage;
import cz.sefira.obelisk.util.DSSUtils;
import cz.sefira.obelisk.util.X509Utils;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.utils.Hex;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.security.auth.x500.X500Principal;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Pool of trusted certificates
 */
public class SSLCertificateProvider {

  private static final Logger logger = LoggerFactory.getLogger(SSLCertificateProvider.class);

  private final SSLCacheStorage cache;
  private final Map<String, List<X509Certificate>> certsBySubject = new HashMap<>();
  private final Set<X509Certificate> unique = new HashSet<>();

  private KeyStore trustStore;
  private DelegatedTrustManager delegatedTrustManager;
  private Registry<ConnectionSocketFactory> socketFactory;

  public SSLCertificateProvider(SSLCacheStorage cache) {
    this.cache = cache;
  }

  public boolean put(X509Certificate certificate) {
    if (!unique.contains(certificate)) {
      final String subjectName = certificate.getSubjectX500Principal().getName(X500Principal.CANONICAL);
      List<X509Certificate> certs = certsBySubject.computeIfAbsent(subjectName, k -> new ArrayList<>());
      certs.add(certificate);
      unique.add(certificate);
      return true;
    }
    return false;
  }

  public List<X509Certificate> getBySubject(X500Principal subjectName) {
    String canonicalSubjectName = subjectName.getName(X500Principal.CANONICAL);
    return certsBySubject.get(canonicalSubjectName);
  }

  public KeyStore getTrustStore() {
    return trustStore;
  }

  public void unregisterSocketFactory() {
    socketFactory = null;
  }

  public void setTrustStore(KeyStore trustStore) {
    this.trustStore = trustStore;
  }

  public Set<X509Certificate> getUnique() {
    return unique;
  }

  public List<X509Certificate> getCertificateChain() {
    return delegatedTrustManager != null ? delegatedTrustManager.getCertificateChain() : null;
  }

  public boolean addTrustedChain(List<X509Certificate> chain, boolean addToCache)
      throws CertificateException, KeyStoreException {
    boolean trustedChain = false;
    X509Certificate chainEnd = chain.get(chain.size()-1);
    List<X509Certificate> anchors = getBySubject(chainEnd.getIssuerX500Principal());
    if (anchors != null) {
      for (X509Certificate anchor : anchors) {
        // find trusted-anchor that signs the chain
        if (X509Utils.validateCertificateIssuer(chainEnd, anchor)) {
          trustedChain = true;
        }
      }
    }
    if (trustedChain) {
      addToRuntimeTruststore(chain);
      // trusted chain found - add to cache if needed
      if (addToCache) {
        cache.add(chain);
      }
      // used as trusted chain
      return true;
    } else {
      // no longer able to find path to trusted anchor - remove from cache
      return false;
    }
  }

  public void addToRuntimeTruststore(List<X509Certificate> chain)
      throws KeyStoreException, CertificateEncodingException {
    for (X509Certificate certificate : chain) {
      if (put(certificate)) {
        String alias = Hex.encodeHexString(DSSUtils.digest(DigestAlgorithm.SHA1, certificate.getEncoded()));
        logger.info("Add certificate to runtime trust: " + certificate.getSubjectX500Principal().toString() + " (" + alias + ")");
        getTrustStore().setCertificateEntry(alias, certificate);
      }
    }
    unregisterSocketFactory();
  }

  public Registry<ConnectionSocketFactory> getSocketFactory()
      throws KeyStoreException, KeyManagementException, NoSuchAlgorithmException {
    if (socketFactory != null) {
      return socketFactory;
    }
    logger.info("Creating new socket factory");
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(trustStore);
    delegatedTrustManager = new DelegatedTrustManager((X509ExtendedTrustManager) tmf.getTrustManagers()[0]);
    TrustManager[] trustManagers = {delegatedTrustManager};
    SSLContext sc = SSLContext.getInstance("TLS");
    sc.init(null, trustManagers, new SecureRandom());
    SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sc, new DefaultHostnameVerifier());
    socketFactory = RegistryBuilder.<ConnectionSocketFactory>create()
        .register("http", PlainConnectionSocketFactory.getSocketFactory())
        .register("https", sslSocketFactory)
        .build();
    return socketFactory;
  }
}

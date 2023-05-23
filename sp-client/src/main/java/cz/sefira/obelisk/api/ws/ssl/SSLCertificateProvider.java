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

import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
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
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Pool of trusted certificates
 */
public class SSLCertificateProvider {

  private static final Logger logger = LoggerFactory.getLogger(SSLCertificateProvider.class);

  private final Map<String, List<X509Certificate>> certsBySubject = new HashMap<>();
  private final Set<X509Certificate> unique = new HashSet<>();
  private KeyStore trustStore;
  private DelegatedTrustManager delegatedTrustManager;
  private Registry<ConnectionSocketFactory> socketFactory;

  public void put(X509Certificate certificate) {
    final String subjectName = certificate.getSubjectX500Principal().getName(X500Principal.CANONICAL);
    List<X509Certificate> certs = certsBySubject.computeIfAbsent(subjectName, k -> new ArrayList<>());
    certs.add(certificate);
    unique.add(certificate);
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

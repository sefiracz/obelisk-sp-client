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

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.security.auth.x500.X500Principal;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pool of trusted certificates
 */
public class SSLCertificateProvider {

  private final Map<String, List<X509Certificate>> certsBySubject = new HashMap<>();
  private KeyStore trustStore;
  private DelegatedTrustManager delegatedTrustManager;
  private Registry<ConnectionSocketFactory> socketFactory;

  public void put(X509Certificate certificate) {
    final String subjectName = certificate.getSubjectX500Principal().getName(X500Principal.CANONICAL);
    List<X509Certificate> certs = certsBySubject.computeIfAbsent(subjectName, k -> new ArrayList<>());
    certs.add(certificate);
  }

  public List<X509Certificate> getBySubject(X500Principal subjectName) {
    String canonicalSubjectName = subjectName.getName(X500Principal.CANONICAL);
    return certsBySubject.get(canonicalSubjectName);
  }

  public KeyStore getTrustStore() {
    return trustStore;
  }

  public void setTrustStore(KeyStore trustStore) {
    this.trustStore = trustStore;
  }

  public List<X509Certificate> getCertificateChain() {
    return delegatedTrustManager != null ? delegatedTrustManager.getCertificateChain() : null;
  }

  public Registry<ConnectionSocketFactory> getSocketFactory()
      throws KeyStoreException, KeyManagementException, NoSuchAlgorithmException {
    if (socketFactory != null) {
      return socketFactory;
    }
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

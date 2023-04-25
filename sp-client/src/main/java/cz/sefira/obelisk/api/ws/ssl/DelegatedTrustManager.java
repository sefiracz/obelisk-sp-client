package cz.sefira.obelisk.api.ws.ssl;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.ws.ssl.DelegateTrustedManager
 *
 * Created: 07.02.2023
 * Author: hlavnicka
 */

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Delegated TrustManager that saves SSL certificates in case the chain cannot be evaluated as trusted
 */
public class DelegatedTrustManager extends X509ExtendedTrustManager implements X509TrustManager {

  private List<X509Certificate> chainList = new ArrayList<>();
  private final X509ExtendedTrustManager delegate;

  public DelegatedTrustManager(X509ExtendedTrustManager delegate) {
    this.delegate = delegate;
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
      throws CertificateException {
    delegate.checkClientTrusted(chain, authType, socket);
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
      throws CertificateException {
    delegate.checkClientTrusted(chain, authType, engine);
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    delegate.checkClientTrusted(chain, authType);
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
      throws CertificateException {
    chainList = new ArrayList<>();
    Collections.addAll(chainList, chain);
    delegate.checkServerTrusted(chain, authType, socket);
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
      throws CertificateException {
    chainList = new ArrayList<>();
    Collections.addAll(chainList, chain);
    delegate.checkServerTrusted(chain, authType, engine);
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    chainList = new ArrayList<>();
    Collections.addAll(chainList, chain);
    delegate.checkServerTrusted(chain, authType);
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return delegate.getAcceptedIssuers();
  }

  public List<X509Certificate> getCertificateChain() {
    return chainList;
  }
}
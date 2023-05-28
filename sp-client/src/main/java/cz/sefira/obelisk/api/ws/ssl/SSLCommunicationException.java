package cz.sefira.obelisk.api.ws.ssl;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.ws.ssl.SSLCommunicationException
 *
 * Created: 14.03.2023
 * Author: hlavnicka
 */

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * SSL communication failure exception
 */
public class SSLCommunicationException extends IOException {

  private final SSLException exception;
  private final String hostname;
  private final List<X509Certificate> certificateChain;

  public SSLCommunicationException(SSLException exception, String hostname, List<X509Certificate> certificateChain) {
    super(exception);
    this.exception = exception;
    this.hostname = hostname;
    this.certificateChain = certificateChain;
  }

  public SSLException getSSLException() {
    return exception;
  }

  public String getHostname() {
    return hostname;
  }

  public List<X509Certificate> getCertificateChain() {
    return certificateChain;
  }

}

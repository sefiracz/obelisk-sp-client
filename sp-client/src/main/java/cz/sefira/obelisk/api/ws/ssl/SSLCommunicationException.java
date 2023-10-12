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

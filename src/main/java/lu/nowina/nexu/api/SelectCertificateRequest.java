package lu.nowina.nexu.api;

/*
 * Copyright 2020 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.api.SelectCertificateRequest
 *
 * Created: 16.12.2020
 * Author: hlavnicka
 */

import eu.europa.esig.dss.x509.CertificateToken;

public class SelectCertificateRequest extends NexuRequest {

  private CertificateToken certificate;
  private boolean closeToken = true;

  // GuardedString?

  public CertificateToken getCertificate() {
    return certificate;
  }

  public void setCertificate(CertificateToken certificate) {
    this.certificate = certificate;
  }

  public boolean isCloseToken() {
    return this.closeToken;
  }

  public void setCloseToken(final boolean closeToken) {
    this.closeToken = closeToken;
  }
}

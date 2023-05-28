package cz.sefira.obelisk.storage.model;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.storage.CertificateChain
 *
 * Created: 27.05.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.util.annotation.NotNull;

import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Certificate chain
 */
public class CertificateChain {

  private final List<X509Certificate> certificateChain;

  public CertificateChain(@NotNull List<X509Certificate> certificateChain) {
    this.certificateChain = certificateChain;
  }

  public List<X509Certificate> getCertificateChain() {
    return certificateChain;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CertificateChain that = (CertificateChain) o;

    return getCertificateChain().equals(that.getCertificateChain());
  }

  @Override
  public int hashCode() {
    return getCertificateChain().hashCode();
  }
}

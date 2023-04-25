package cz.sefira.obelisk.token.keystore;

/*
 * Copyright 2022 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.generic.EmptyKeyEntry
 *
 * Created: 06.01.2022
 * Author: hlavnicka
 */

import cz.sefira.obelisk.dss.DSSException;
import cz.sefira.obelisk.dss.EncryptionAlgorithm;
import cz.sefira.obelisk.dss.token.DSSPrivateKeyEntry;
import cz.sefira.obelisk.dss.x509.CertificateToken;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Empty wrapper for entry without a private key
 */
public class EmptyKeyEntry implements DSSPrivateKeyEntry {

  private final String alias;
  private final CertificateToken certificateToken;

  public EmptyKeyEntry(String alias, X509Certificate certificate) {
    this.alias = alias;
    this.certificateToken = new CertificateToken(certificate);
  }

  @Override
  public CertificateToken getCertificateToken() {
    return certificateToken;
  }

  @Override
  public CertificateToken[] getCertificateChain() {
    return new CertificateToken[] {certificateToken};
  }

  @Override
  public EncryptionAlgorithm getEncryptionAlgorithm() throws DSSException {
    PublicKey publicKey = certificateToken.getCertificate().getPublicKey();
    if (publicKey instanceof RSAPublicKey) {
      return EncryptionAlgorithm.RSA;
    }
    else if (publicKey instanceof DSAPublicKey) {
      return EncryptionAlgorithm.DSA;
    }
    else {
      return publicKey instanceof ECPublicKey ? EncryptionAlgorithm.ECDSA :
          EncryptionAlgorithm.forName(publicKey.getAlgorithm());
    }
  }

  public String getAlias() {
    return alias;
  }
}

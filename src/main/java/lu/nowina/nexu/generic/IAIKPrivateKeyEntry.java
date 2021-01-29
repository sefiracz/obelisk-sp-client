package lu.nowina.nexu.generic;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.generic.IAIKPrivateKeyEntry
 *
 * Created: 28.01.2021
 * Author: hlavnicka
 */

import eu.europa.esig.dss.DSSException;
import eu.europa.esig.dss.EncryptionAlgorithm;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.x509.CertificateToken;
import iaik.pkcs.pkcs11.TokenException;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * IAIK PKCS11 private key entry
 */
public class IAIKPrivateKeyEntry implements DSSPrivateKeyEntry {

  private final String keyLabel;
  private final CertificateToken certificate;
  private final CertificateToken[] certificateChain;
  private final EncryptionAlgorithm encryptionAlgorithm;

  public IAIKPrivateKeyEntry(final PKCS11Module pkcs11, final String keyLabel) throws TokenException,
      CertificateException {
    this.keyLabel = keyLabel;

    byte[] encoded = pkcs11.getDEREncodedCertificateFromLabel(keyLabel);

    X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X509")
        .generateCertificate(new ByteArrayInputStream(encoded));

    certificate = new CertificateToken(cert);

    String encryptionAlgo = certificate.getPublicKey().getAlgorithm(); // RSA, EC, DSA
    this.encryptionAlgorithm = EncryptionAlgorithm.forName(encryptionAlgo);

    certificateChain = new CertificateToken[]{ certificate };
  }

  public String getKeyLabel() {
    return keyLabel;
  }

  @Override
  public CertificateToken getCertificate() {
    return certificate;
  }

  @Override
  public CertificateToken[] getCertificateChain() {
    return certificateChain;
  }

  @Override
  public EncryptionAlgorithm getEncryptionAlgorithm() throws DSSException {
    return encryptionAlgorithm;
  }
}

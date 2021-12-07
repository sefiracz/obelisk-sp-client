package cz.sefira.obelisk.macos.keystore;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.macos.keystore.KeychainPrivateKey
 *
 * Created: 06.12.2021
 * Author: hlavnicka
 */

import eu.europa.esig.dss.DSSException;
import eu.europa.esig.dss.EncryptionAlgorithm;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.x509.CertificateToken;

import java.security.*;
import java.security.cert.X509Certificate;

/**
 * Apple Keychain private key wrapper
 */
public class KeychainPrivateKey implements DSSPrivateKeyEntry {

  private final KeyStore keyStore;
  private final String alias;
  private final CertificateToken certificate;
  private final CertificateToken[] certificateChain;
  private final EncryptionAlgorithm encryptionAlgorithm;

  private PrivateKey privateKey;

  public KeychainPrivateKey(final KeyStore keyStore, final String alias) throws KeyStoreException {
    this.keyStore = keyStore;
    this.alias = alias;
    this.certificate = new CertificateToken((X509Certificate) keyStore.getCertificate(alias));
    this.certificateChain = new CertificateToken[]{ certificate };
    String encryptionAlgo = certificate.getPublicKey().getAlgorithm(); // RSA, EC, DSA
    this.encryptionAlgorithm = EncryptionAlgorithm.forName(encryptionAlgo);
  }

  /**
   * Get the private key
   *
   * @return the private key
   */
  public PrivateKey getPrivateKey() throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
    privateKey = privateKey == null ? (PrivateKey) keyStore.getKey(alias, null) : privateKey;
    return privateKey;
  }

  public String getAlias() {
    return alias;
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

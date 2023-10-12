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
package cz.sefira.obelisk.token.macos;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.macos.keystore.KeychainPrivateKey
 *
 * Created: 06.12.2021
 * Author: hlavnicka
 */

import cz.sefira.obelisk.dss.DSSException;
import cz.sefira.obelisk.dss.EncryptionAlgorithm;
import cz.sefira.obelisk.dss.token.DSSPrivateKeyEntry;
import cz.sefira.obelisk.dss.x509.CertificateToken;

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
  public CertificateToken getCertificateToken() {
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

  @Override
  public String toString() {
    return getClass().getSimpleName()+" alias='" + alias + "'";
  }

}

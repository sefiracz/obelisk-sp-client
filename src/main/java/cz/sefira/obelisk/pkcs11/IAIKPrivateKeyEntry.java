/**
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package cz.sefira.obelisk.pkcs11;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.generic.IAIKPrivateKeyEntry
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

  public IAIKPrivateKeyEntry(final TokenHandler token, final String keyLabel) throws TokenException,
      CertificateException {
    this.keyLabel = keyLabel;

    byte[] encoded = token.getCertificate(keyLabel);

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

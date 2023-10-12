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

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
package cz.sefira.obelisk.windows.keystore;

/*
 * Copyright 2022 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.windows.keystore.WindowsKeyStoreTokenConnection
 *
 * Created: 04.01.2022
 * Author: hlavnicka
 */

import cz.sefira.obelisk.flow.exceptions.PKCS11TokenException;
import cz.sefira.obelisk.flow.exceptions.GenericTokenRuntimeException;
import cz.sefira.obelisk.generic.EmptyKeyEntry;
import cz.sefira.obelisk.view.DialogMessage;
import eu.europa.esig.dss.*;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.KSPrivateKeyEntry;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class WindowsSignatureTokenAdapter implements SignatureTokenConnection {

  private static final Logger logger = LoggerFactory.getLogger(WindowsSignatureTokenAdapter.class.getName());

  public KeyStore getKeyStore() {
    try {
      KeyStore keyStore = KeyStore.getInstance("Windows-MY");
      keyStore.load(null, null);
      return keyStore;
    } catch (Exception e) {
      throw new DSSException(e);
    }
  }

  @Override
  public List<DSSPrivateKeyEntry> getKeys() throws DSSException {
    final List<DSSPrivateKeyEntry> list = new ArrayList<DSSPrivateKeyEntry>();
    try {
      final KeyStore keyStore = getKeyStore();
      final Enumeration<String> aliases = keyStore.aliases();
      while (aliases.hasMoreElements()) {
        final String alias = aliases.nextElement();
        try {
          if (keyStore.isKeyEntry(alias)) {
            final KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keyStore
                .getEntry(alias, new KeyStore.PasswordProtection("nimp".toCharArray()));
            list.add(new KSPrivateKeyEntry(alias, entry));
          }
          else if (keyStore.isCertificateEntry(alias)) {
            logger.info("Only certificate found for '{}'", alias);
            Certificate certificate = keyStore.getCertificate(alias);
            list.add(new EmptyKeyEntry(alias, (X509Certificate) certificate));
          }
          else {
            logger.info("No related/supported key found for alias '{}'", alias);
          }
        } catch (Exception e) {
          logger.error("Unable to retrieve key '{}' from keystore: " + e.getMessage(), alias);
        }
      }
    }
    catch (GeneralSecurityException e) {
      throw new DSSException("Unable to retrieve keys from keystore", e);
    }
    return list;
  }

  @Override
  public SignatureValue sign(ToBeSigned toBeSigned, DigestAlgorithm digestAlgorithm, DSSPrivateKeyEntry keyEntry) throws DSSException {
    return sign(toBeSigned, digestAlgorithm, null, keyEntry);
  }

  @Override
  public SignatureValue sign(ToBeSigned toBeSigned, DigestAlgorithm digestAlgorithm, MaskGenerationFunction mgf, DSSPrivateKeyEntry keyEntry)
      throws DSSException {
    if (!(keyEntry instanceof KSPrivateKeyEntry)) {
      throw new PKCS11TokenException("Only KSPrivateKeyEntry are supported");
    }
    // MSCAPI does not support SHA224 throw error
    if (DigestAlgorithm.SHA224.equals(digestAlgorithm)) {
      throw new GenericTokenRuntimeException("MSCAPI does not support "+digestAlgorithm.getName(),
          "key.selection.keystore.unsupported.algorithm", DialogMessage.Level.ERROR, digestAlgorithm.getName());
    }

    final EncryptionAlgorithm encryptionAlgorithm = keyEntry.getEncryptionAlgorithm();
    final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.getAlgorithm(encryptionAlgorithm, digestAlgorithm, mgf);
    final String javaSignatureAlgorithm = signatureAlgorithm.getJCEId();
    logger.info("Signature algorithm : {}", javaSignatureAlgorithm);

    try {
      Signature signature;
      if (mgf != null && EncryptionAlgorithm.RSA.equals(signatureAlgorithm.getEncryptionAlgorithm())) {
        signature = Signature.getInstance("RSASSA-PSS", "SunMSCAPI");
        signature.setParameter(createPSSParam(digestAlgorithm));
      } else {
        signature = Signature.getInstance(javaSignatureAlgorithm);
      }
      signature.initSign(((KSPrivateKeyEntry) keyEntry).getPrivateKey());
      signature.update(toBeSigned.getBytes());
      final byte[] signatureValue = signature.sign();
      SignatureValue value = new SignatureValue();
      value.setAlgorithm(signatureAlgorithm);
      value.setValue(signatureValue);
      return value;
    } catch (Exception e) {
      throw new DSSException(e);
    }

  }

  private AlgorithmParameterSpec createPSSParam(DigestAlgorithm digestAlgo) {
    String digestJavaName = digestAlgo.getJavaName();
    return new PSSParameterSpec(digestJavaName, "MGF1", new MGF1ParameterSpec(digestJavaName), digestAlgo.getSaltLength(), 1);
  }

  @Override
  public void close() {
  }


}

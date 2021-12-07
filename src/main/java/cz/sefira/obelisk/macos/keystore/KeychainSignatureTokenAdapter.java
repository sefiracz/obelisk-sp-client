/**
 * © SEFIRA spol. s r.o., 2020-2021
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
package cz.sefira.obelisk.macos.keystore;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.macos.keystore.KeychainSignatureTokenAdapter
 *
 * Created: 06.12.2021
 * Author: hlavnicka
 */

import cz.sefira.obelisk.CancelledOperationException;
import eu.europa.esig.dss.*;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Apple Keychain Token Adapter
 */
public class KeychainSignatureTokenAdapter implements SignatureTokenConnection {

  private static final Logger logger = LoggerFactory.getLogger(KeychainSignatureTokenAdapter.class.getName());

  private final KeyStore keyStore;

  public KeychainSignatureTokenAdapter() {
    try {
      keyStore = KeyStore.getInstance("KeychainStore");
      keyStore.load(null, null);
    } catch (Exception e) {
      throw new DSSException(e);
    }
  }

  @Override
  public List<DSSPrivateKeyEntry> getKeys() throws DSSException {
    try {
      List<DSSPrivateKeyEntry> keys = new ArrayList<>();
      Enumeration<String> aliases = keyStore.aliases();
      while(aliases.hasMoreElements()) {
        String alias = aliases.nextElement();
        if (keyStore.isKeyEntry(alias)) {
          keys.add(new KeychainPrivateKey(keyStore, alias));
        }
      }
      return keys;
    }
    catch (KeyStoreException e) {
      throw new DSSException(e);
    }
  }

  @Override
  public void close() {
  }

  @Override
  public SignatureValue sign(ToBeSigned toBeSigned, DigestAlgorithm digestAlgorithm, DSSPrivateKeyEntry keyEntry) throws DSSException {
    return sign(toBeSigned, digestAlgorithm, null, keyEntry);
  }

  @Override
  public SignatureValue sign(ToBeSigned toBeSigned, DigestAlgorithm digestAlgorithm, MaskGenerationFunction mgf,
                             DSSPrivateKeyEntry keyEntry) throws DSSException {
    if (!(keyEntry instanceof KeychainPrivateKey)) {
      throw new IllegalArgumentException("Only KeychainPrivateKey are supported");
    }
    final EncryptionAlgorithm encryptionAlgorithm = keyEntry.getEncryptionAlgorithm();
    final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.getAlgorithm(encryptionAlgorithm, digestAlgorithm, mgf);
    final String javaSignatureAlgorithm = signatureAlgorithm.getJCEId();
    logger.info("Signature algorithm: {}", javaSignatureAlgorithm);
    try {
      final Signature signature = Signature.getInstance(javaSignatureAlgorithm);
      PrivateKey pk = ((KeychainPrivateKey) keyEntry).getPrivateKey();
      if (pk == null) {
        throw new CancelledOperationException("User did not obtain private key");
      }
      signature.initSign(pk);
      if (mgf != null) {
        signature.setParameter(createPSSParam(digestAlgorithm));
      }
      signature.update(toBeSigned.getBytes());
      final byte[] signatureValue = signature.sign();
      SignatureValue value = new SignatureValue();
      value.setAlgorithm(signatureAlgorithm);
      value.setValue(signatureValue);
      return value;
    } catch (CancelledOperationException e) {
      throw e;
    } catch (Exception e) {
      throw new DSSException(e);
    }

  }

  private AlgorithmParameterSpec createPSSParam(DigestAlgorithm digestAlgo) {
    String digestJavaName = digestAlgo.getJavaName();
    return new PSSParameterSpec(digestJavaName, "MGF1", new MGF1ParameterSpec(digestJavaName),
        digestAlgo.getSaltLength(), 1);
  }
}

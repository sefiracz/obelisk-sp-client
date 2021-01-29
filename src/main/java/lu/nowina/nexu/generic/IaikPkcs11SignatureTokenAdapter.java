package lu.nowina.nexu.generic;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.generic.IaikPkcs11SignatureTokenAdapter
 *
 * Created: 28.01.2021
 * Author: hlavnicka
 */

import eu.europa.esig.dss.*;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.PasswordInputCallback;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import iaik.pkcs.pkcs11.TokenException;
import iaik.pkcs.pkcs11.wrapper.PKCS11Constants;
import lu.nowina.nexu.CancelledOperationException;
import lu.nowina.nexu.api.DetectedCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.pkcs11.wrapper.PKCS11RuntimeException;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * PKCS11 Token Adapter (using IAIK PKCS11 Wrapper)
 */
public class IaikPkcs11SignatureTokenAdapter implements SignatureTokenConnection {

  private static final Logger logger = LoggerFactory.getLogger(IaikPkcs11SignatureTokenAdapter.class.getName());

  private final String pkcs11Path;
  private final PasswordInputCallback callback;
  private final DetectedCard card;

  private PKCS11Module pkcs11;

  public IaikPkcs11SignatureTokenAdapter(final File pkcs11Lib, final PasswordInputCallback callback, final DetectedCard card)
      throws IOException, TokenException {
    this.pkcs11Path = pkcs11Lib.getAbsolutePath();
    this.callback = callback;
    this.card = card;
    logger.info("Module library: " + pkcs11Path);
    connect();
    // TODO - save token info
  }

  private void connect() throws IOException, TokenException {
    pkcs11 = new PKCS11Module(pkcs11Path, card.getTerminalLabel());
  }

  // TODO ??? (needs analysis / testing)
  private void setMechanism(X509Certificate certificate, MaskGenerationFunction mgf) {
    String algorithm = certificate.getPublicKey().getAlgorithm();
    switch (algorithm) {
      case "RSA":
        if(mgf == null) {
          pkcs11.setMechanism(PKCS11Constants.CKM_RSA_PKCS);
        } else {
          pkcs11.setMechanism(PKCS11Constants.CKM_RSA_PKCS_PSS);
        }
        break;
      case "EC":
        pkcs11.setMechanism(PKCS11Constants.CKM_ECDSA);
        break;
      case "DSA":
        pkcs11.setMechanism(PKCS11Constants.CKM_DSA);
      default:
        throw new IllegalStateException("Unexpected key algorithm: "+algorithm);
    }
  }

  @Override
  public void close() {
    // TODO - rozdelit closeSession a finalize
    // close and finalize only if needed
    if(!isModuleFinalized()) {
      try {
        pkcs11.closeSession();
      } catch (Throwable t) {
        logger.error("PKCS11 CloseSession Error: "+t.getMessage(), t);
      }
      try {
        pkcs11.moduleFinalize();
      } catch (Throwable t) {
        logger.error("PKCS11 Finalize Error: "+t.getMessage(), t);
      }
    }
  }

  public boolean isModuleFinalized() {
    return pkcs11.isModuleFinalized();
  }

  @Override
  public List<DSSPrivateKeyEntry> getKeys() throws DSSException {
    try {
      // TODO - kontrola ze session je funkcni ?
      if(pkcs11.getSession() < 0) {
        pkcs11.openSession(callback.getPassword());
      }
      List<DSSPrivateKeyEntry> keys = new ArrayList<>();
      List<String> labels = pkcs11.getSignatureKeyLabels();
      for(String label : labels) {
        keys.add(new IAIKPrivateKeyEntry(pkcs11, label));
      }
      return keys;
    }
    catch (Exception e) {
      if ("CKR_CANCEL".equals(e.getMessage()) || "CKR_FUNCTION_CANCELED".equals(e.getMessage())) {
        throw new CancelledOperationException(e);
      }
      close(); // TODO - volat pouze closeSession?
      if ("CKR_TOKEN_NOT_PRESENT".equals(e.getMessage()) || "CKR_SESSION_HANDLE_INVALID".equals(e.getMessage()) ||
          "CKR_DEVICE_REMOVED".equals(e.getMessage()) || "CKR_SESSION_CLOSED".equals(e.getMessage())) {
        throw new PKCS11RuntimeException(e);
      }
      throw new DSSException(e);
    }
  }

  @Override
  public SignatureValue sign(ToBeSigned toBeSigned, DigestAlgorithm digestAlgorithm, DSSPrivateKeyEntry keyEntry)
      throws DSSException {
    return sign(toBeSigned, digestAlgorithm, null, keyEntry);
  }

  @Override
  public SignatureValue sign(ToBeSigned toBeSigned, DigestAlgorithm digestAlgorithm, MaskGenerationFunction mgf,
                             DSSPrivateKeyEntry keyEntry) throws DSSException {
    try {
      // TODO - kontrola ze session je funkcni ?
      final EncryptionAlgorithm encryptionAlgorithm = keyEntry.getEncryptionAlgorithm();
      final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.getAlgorithm(encryptionAlgorithm, digestAlgorithm, mgf);
      setMechanism(keyEntry.getCertificate().getCertificate(), mgf); // TODO - vhodne misto ?
      byte[] digest = MessageDigest.getInstance(digestAlgorithm.getJavaName()).digest(toBeSigned.getBytes());
      byte[] sigValue = pkcs11.sign(((IAIKPrivateKeyEntry)keyEntry).getKeyLabel(), digest);
      SignatureValue value = new SignatureValue();
      value.setAlgorithm(signatureAlgorithm);
      value.setValue(sigValue);
      return value;
    } catch (Exception e) {
      if ("CKR_CANCEL".equals(e.getMessage()) || "CKR_FUNCTION_CANCELED".equals(e.getMessage())) {
        throw new CancelledOperationException(e);
      }
      close(); // TODO - volat pouze closeSession?
      if ("CKR_TOKEN_NOT_PRESENT".equals(e.getMessage()) || "CKR_SESSION_HANDLE_INVALID".equals(e.getMessage()) ||
          "CKR_DEVICE_REMOVED".equals(e.getMessage()) || "CKR_SESSION_CLOSED".equals(e.getMessage())) {
        throw new PKCS11RuntimeException(e);
      }
      throw new DSSException(e);
    }
  }


}

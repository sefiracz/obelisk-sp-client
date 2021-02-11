package lu.nowina.nexu.pkcs11;

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
import iaik.pkcs.pkcs11.TokenException;
import lu.nowina.nexu.CancelledOperationException;
import lu.nowina.nexu.api.DetectedCard;
import lu.nowina.nexu.api.NexuAPI;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.pkcs11.wrapper.PKCS11RuntimeException;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * PKCS11 Token Adapter (using IAIK PKCS11 Wrapper)
 */
public class IaikPkcs11SignatureTokenAdapter extends AbstractPkcs11SignatureTokenAdapter {

  private static final Logger logger = LoggerFactory.getLogger(IaikPkcs11SignatureTokenAdapter.class.getName());

  private final String pkcs11Path;
  private final PasswordInputCallback callback;
  private final TokenHandler token;

  public IaikPkcs11SignatureTokenAdapter(final NexuAPI api, final File pkcs11Lib, final PasswordInputCallback callback,
                                         final DetectedCard card)
      throws IOException, TokenException {
    super(pkcs11Lib.getAbsolutePath());
    this.pkcs11Path = pkcs11Lib.getAbsolutePath();
    this.callback = callback;
    // get token handler
    logger.info("Module library: " + pkcs11Path);
    token = api.getPKCS11Manager().getPkcs11TokenHandler(card, pkcs11Path);
    // load token information
    token.setTokenInfo(card);
  }

  private void open() throws TokenException {
    long start = System.currentTimeMillis();
    token.openSession();
    token.login(callback);
    logger.info("OPEN: "+(System.currentTimeMillis()-start)+"ms");
  }

  @Override
  public void close() {
    try {
      long start = System.currentTimeMillis();
      token.logout();
      token.closeSession();
      logger.info("CLOSE: "+(System.currentTimeMillis()-start)+"ms");
    }
    catch (Throwable t) {
      logger.error("PKCS11 CloseSession exception: " + t.getMessage(), t);
    }
  }

  @Override
  public List<DSSPrivateKeyEntry> getKeys() throws DSSException {
    try {
      open();
      List<DSSPrivateKeyEntry> keys = new ArrayList<>();
      List<String> labels = token.getPrivateKeyLabels();
      for (String label : labels) {
        keys.add(new IAIKPrivateKeyEntry(token, label));
      }
      return keys;
    }
    catch (Exception e) {
      if (e instanceof CancelledOperationException || "CKR_CANCEL".equals(e.getMessage())
          || "CKR_FUNCTION_CANCELED".equals(e.getMessage())) {
        throw new CancelledOperationException(e);
      }
      if ("CKR_TOKEN_NOT_PRESENT".equals(e.getMessage()) || "CKR_SESSION_HANDLE_INVALID".equals(e.getMessage()) ||
          "CKR_DEVICE_REMOVED".equals(e.getMessage()) || "CKR_SESSION_CLOSED".equals(e.getMessage()) ||
          "CKR_SLOT_ID_INVALID".equals(e.getMessage())) {
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
//      open();
      final EncryptionAlgorithm encryptionAlgorithm = keyEntry.getEncryptionAlgorithm();
      final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm
          .getAlgorithm(encryptionAlgorithm, digestAlgorithm, mgf);
      // prepare ASN1 signature structure
      MessageDigest md = MessageDigest.getInstance(digestAlgorithm.getJavaName());
      byte[] digest = md.digest(toBeSigned.getBytes());
      ASN1ObjectIdentifier digestOID = new ASN1ObjectIdentifier(digestAlgorithm.getOid());
      AlgorithmIdentifier algID = new AlgorithmIdentifier(digestOID, null);
      DigestInfo digestInfo = new DigestInfo(algID, digest);
      byte[] signatureData = digestInfo.getEncoded();
      // sign data
      IAIKPrivateKeyEntry key = ((IAIKPrivateKeyEntry) keyEntry);
      byte[] sigValue = token.sign(key.getKeyLabel(), key.getCertificate().getCertificate(), signatureData);
      SignatureValue value = new SignatureValue();
      value.setAlgorithm(signatureAlgorithm);
      value.setValue(sigValue);
      return value;
    }
    catch (Exception e) {
      if (e instanceof CancelledOperationException || "CKR_CANCEL".equals(e.getMessage())
          || "CKR_FUNCTION_CANCELED".equals(e.getMessage())) {
        throw new CancelledOperationException(e);
      }
      if ("CKR_TOKEN_NOT_PRESENT".equals(e.getMessage()) || "CKR_SESSION_HANDLE_INVALID".equals(e.getMessage()) ||
          "CKR_DEVICE_REMOVED".equals(e.getMessage()) || "CKR_SESSION_CLOSED".equals(e.getMessage()) ||
          "CKR_SLOT_ID_INVALID".equals(e.getMessage())) {
        throw new PKCS11RuntimeException(e);
      }
      throw new DSSException(e);
    }
  }

  public String getPkcs11Library() {
    return pkcs11Path;
  }

}

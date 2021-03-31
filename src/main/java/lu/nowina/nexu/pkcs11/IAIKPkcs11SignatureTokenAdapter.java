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
package lu.nowina.nexu.pkcs11;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.pkcs11.IaikPkcs11SignatureTokenAdapter
 *
 * Created: 28.01.2021
 * Author: hlavnicka
 */

import eu.europa.esig.dss.*;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.PasswordInputCallback;
import iaik.pkcs.pkcs11.TokenException;
import iaik.pkcs.pkcs11.wrapper.PKCS11Exception;
import lu.nowina.nexu.CancelledOperationException;
import lu.nowina.nexu.api.DetectedCard;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.flow.exceptions.PKCS11ModuleException;
import lu.nowina.nexu.flow.exceptions.PKCS11TokenException;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PKCS11 Token Adapter (using IAIK PKCS11 Wrapper)
 */
public class IAIKPkcs11SignatureTokenAdapter extends AbstractPkcs11SignatureTokenAdapter {

  private static final Logger logger = LoggerFactory.getLogger(IAIKPkcs11SignatureTokenAdapter.class.getName());

  private final String pkcs11Path;
  private final PasswordInputCallback callback;
  private final TokenHandler token;

  private boolean loggedIn = false;

  public IAIKPkcs11SignatureTokenAdapter(final NexuAPI api, final File pkcs11Lib, final PasswordInputCallback callback,
                                         final DetectedCard detectedCard) {
    super(pkcs11Lib.getAbsolutePath());
    this.pkcs11Path = pkcs11Lib.getAbsolutePath();
    logger.info("Module library: " + pkcs11Path);
    this.callback = callback;
    try {
      // check state
      if(!detectedCard.isInitialized()) {
        detectedCard.initializeToken(api, pkcs11Path);
      }
      // check session
      if(!detectedCard.isOpened()) {
        detectedCard.openToken();
      }
      this.token = detectedCard.getTokenHandler();
    } catch (TokenException e) {
      throw new PKCS11TokenException("Token not present or unable to connect", e);
    } catch (IOException e) {
      throw new PKCS11ModuleException("Unable to initialize module", e);
    }
  }

  public void login() throws PKCS11Exception {
    if(!loggedIn) {
      token.login(callback);
      loggedIn = true;
    }
  }

  @Override
  public void close() {
    try {
      if(loggedIn) {
        loggedIn = false;
        token.logout();
      }
    }
    catch (PKCS11Exception e) {
      logger.warn("Unable to logout: "+e.getMessage(), e);
    }
  }

  @Override
  public List<DSSPrivateKeyEntry> getKeys() throws DSSException {
    try {
      login();
      List<DSSPrivateKeyEntry> keys = new ArrayList<>();
      List<String> labels = token.getPrivateKeyLabels();
      for (String label : labels) {
        keys.add(new IAIKPrivateKeyEntry(token, label));
      }
      return keys;
    }
    catch (Exception e) {
      throw processTokenExceptions(e);
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
      login();
      final EncryptionAlgorithm encryptionAlgorithm = keyEntry.getEncryptionAlgorithm();
      final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm
          .getAlgorithm(encryptionAlgorithm, digestAlgorithm, mgf);
      // prepare ASN1 signature structure
      byte[] digest = DSSUtils.digest(digestAlgorithm, toBeSigned.getBytes());
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
      throw processTokenExceptions(e);
    }
  }

  private RuntimeException processTokenExceptions(Exception e) {
    if (e instanceof CancelledOperationException || "CKR_CANCEL".equals(e.getMessage())
            || "CKR_FUNCTION_CANCELED".equals(e.getMessage())) {
      return new CancelledOperationException(e);
    }
    if ("CKR_TOKEN_NOT_PRESENT".equals(e.getMessage()) || "CKR_SESSION_HANDLE_INVALID".equals(e.getMessage()) ||
            "CKR_DEVICE_REMOVED".equals(e.getMessage()) || "CKR_SESSION_CLOSED".equals(e.getMessage()) ||
            "CKR_SLOT_ID_INVALID".equals(e.getMessage()) || "CKR_USER_ALREADY_LOGGED_IN".equals(e.getMessage())) {
      return new PKCS11TokenException(e);
    }
    return new DSSException(e);
  }

  public String getPkcs11Library() {
    return pkcs11Path;
  }

}

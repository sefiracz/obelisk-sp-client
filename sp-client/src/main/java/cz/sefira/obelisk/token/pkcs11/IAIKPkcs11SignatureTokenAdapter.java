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
package cz.sefira.obelisk.token.pkcs11;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.pkcs11.IaikPkcs11SignatureTokenAdapter
 *
 * Created: 28.01.2021
 * Author: hlavnicka
 */

import cz.sefira.obelisk.CancelledOperationException;
import cz.sefira.obelisk.util.DSSUtils;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.ReauthCallback;
import cz.sefira.obelisk.dss.token.SignatureTokenConnection;
import cz.sefira.obelisk.flow.exceptions.PKCS11ModuleException;
import cz.sefira.obelisk.flow.exceptions.PKCS11TokenException;
import cz.sefira.obelisk.dss.*;
import cz.sefira.obelisk.dss.token.DSSPrivateKeyEntry;
import cz.sefira.obelisk.dss.token.PasswordInputCallback;
import iaik.pkcs.pkcs11.TokenException;
import iaik.pkcs.pkcs11.wrapper.PKCS11Exception;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.smartcardio.CardException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PKCS11 Token Adapter (using IAIK PKCS11 Wrapper)
 */
public class IAIKPkcs11SignatureTokenAdapter implements SignatureTokenConnection {

  private static final Logger logger = LoggerFactory.getLogger(IAIKPkcs11SignatureTokenAdapter.class.getName());

  private final PasswordInputCallback callback;
  private final ReauthCallback reauthCallback;
  private final TokenHandler token;
  private final DetectedCard detectedCard;

  private boolean loggedIn = false;

  public IAIKPkcs11SignatureTokenAdapter(final PlatformAPI api, final File pkcs11Lib, final PasswordInputCallback callback,
                                         final DetectedCard detectedCard) {
    String pkcs11Path = pkcs11Lib.getAbsolutePath();
    this.detectedCard = detectedCard;
    logger.info("Initializing token ATR: "+detectedCard.getAtr() + " with module library: " + pkcs11Path);
    this.callback = callback;
    this.reauthCallback = api.getDisplay().getReauthCallback();
    try {
      // check state
      if(!detectedCard.isInitialized()) {
        detectedCard.initializeToken(api, pkcs11Path, true);
      }
      // check session
      if(!detectedCard.isOpened()) {
        detectedCard.openToken();
      }
      this.token = detectedCard.getTokenHandler();
    } catch (TokenException e) {
      logger.error(e.getMessage(), e);
      throw new PKCS11TokenException("Token not present or unable to connect", e);
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
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
  public SignatureValue sign(byte[] toBeSigned, DigestAlgorithm digestAlgorithm, DSSPrivateKeyEntry keyEntry)
      throws DSSException {
    return sign(toBeSigned, digestAlgorithm, null, keyEntry);
  }

  @Override
  public SignatureValue sign(byte[] toBeSigned, DigestAlgorithm digestAlgorithm, MaskGenerationFunction mgf,
                             DSSPrivateKeyEntry keyEntry) throws DSSException {
    try {
      login();
      final EncryptionAlgorithm encryptionAlgorithm = keyEntry.getEncryptionAlgorithm();
      SignatureAlgorithm signatureAlgorithm;
      // prepare data to be signed
      byte[] signatureData;
      if (EncryptionAlgorithm.RSA.equals(encryptionAlgorithm)) {
        if (mgf != null && token.isRsaPssSupportedMechanism(digestAlgorithm)) {
          signatureAlgorithm = SignatureAlgorithm.getAlgorithm(encryptionAlgorithm, digestAlgorithm, mgf);
          // RSA-PSS PKCS#1 v2.1
          signatureData = toBeSigned;
        }
        else {
          signatureAlgorithm = SignatureAlgorithm.getAlgorithm(encryptionAlgorithm, digestAlgorithm);
          // prepare ASN1 signature structure
          // RSA PKCS#1 v1.5
          byte[] digest = DSSUtils.digest(digestAlgorithm, toBeSigned);
          ASN1ObjectIdentifier digestOID = new ASN1ObjectIdentifier(digestAlgorithm.getOid());
          AlgorithmIdentifier algID = new AlgorithmIdentifier(digestOID, DERNull.INSTANCE);
          DigestInfo digestInfo = new DigestInfo(algID, digest);
          signatureData = digestInfo.getEncoded();
        }
      } else {
        signatureAlgorithm = SignatureAlgorithm.getAlgorithm(encryptionAlgorithm, digestAlgorithm);
        // TODO ? sign for non-RSA algorithms ?
        signatureData = toBeSigned;
      }

      // sign data
      IAIKPrivateKeyEntry key = ((IAIKPrivateKeyEntry) keyEntry);
      byte[] sigValue = token.sign(key.getKeyLabel(), signatureAlgorithm, signatureData, reauthCallback);
      SignatureValue value = new SignatureValue();
      value.setAlgorithm(signatureAlgorithm);
      value.setValue(sigValue);
      return value;
    } catch (PKCS11Exception e) {
      // check that the failure is not result of user disconnecting the device during signing process
      try {
        if (!detectedCard.isOpened() || detectedCard.getTerminal() == null ||
            !detectedCard.getTerminal().isCardPresent()) {
          logger.warn("Seems user disconnected device during signing: "+e.getMessage(), e);
          throw new PKCS11TokenException(e);
        }
      } catch (CardException ce) {
        logger.warn("Seems user disconnected device during signing: "+e.getMessage(), e);
        throw new PKCS11TokenException(e);
      }
      throw processTokenExceptions(e);
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

}

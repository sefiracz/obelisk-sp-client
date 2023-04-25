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
 * cz.sefira.obelisk.pkcs11.PKCS11Module
 *
 * Created: 28.01.2021
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.ReauthCallback;
import cz.sefira.obelisk.dss.token.PasswordInputCallback;
import iaik.pkcs.pkcs11.TokenException;
import iaik.pkcs.pkcs11.wrapper.*;
import org.apache.commons.codec.binary.Base64;
import org.identityconnectors.common.security.GuardedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import static iaik.pkcs.pkcs11.wrapper.PKCS11Constants.*;

public class PKCS11Module {

  private static final Logger log = LoggerFactory.getLogger(PKCS11Module.class.getName());

  private final String pkcs11ModulePath;
  private final PKCS11 pkcs11Module;

  private boolean moduleFinalized = false;

  public PKCS11Module(String pkcs11ModulePath) throws IOException, TokenException {
    this.pkcs11ModulePath = pkcs11ModulePath;
    log.info("PKCS#11 module initialization");

    log.info("Connecting to PKCS#11 module: '" + pkcs11ModulePath + "'");
    pkcs11Module = PKCS11Connector.connectToPKCS11Module(pkcs11ModulePath);
    log.info("Connected");

    log.info("Initializing module");
    pkcs11Module.C_Initialize(null, true);
    log.info("Initialized");

    CK_INFO ckInfo = pkcs11Module.C_GetInfo();
    log.info("Cryptoki info:\n"+ckInfo.toString());
  }

  /**
   * Get token handle value from token in given terminal
   * @param terminalLabel Terminal name
   * @return Token handle
   * @throws PKCS11Exception
   */
  public synchronized long getTokenInTerminal(String terminalLabel) throws PKCS11Exception {
    if (log.isDebugEnabled())
      log.debug("Get token in terminal: "+terminalLabel);
    long[] tokens = getSlotList(true);
    for (long token : tokens) {
      String slotTerminal = getSlotDescription(token);
      if(slotTerminal != null)
        slotTerminal = slotTerminal.trim();
      if(terminalLabel.trim().equals(slotTerminal)) {
        return token;
      }
    }
    return -1;
  }

  /**
   * Get list of tokens in slot
   * @param tokenPresent True if only present tokens are to be listed
   * @return List of found token handles
   * @throws PKCS11Exception
   */
  public synchronized long[] getSlotList(boolean tokenPresent) throws PKCS11Exception {
    if (log.isDebugEnabled())
      log.debug("Call C_GetSlotList("+tokenPresent+")");
    return pkcs11Module.C_GetSlotList(tokenPresent);
  }

  /**
   * Returns CK_SLOT_INFO slot information
   * @param slotId Slot ID
   * @return Slot information
   * @throws PKCS11Exception
   */
  public synchronized CK_SLOT_INFO getSlotInfo(long slotId) throws PKCS11Exception {
    if (log.isDebugEnabled())
      log.debug("Call C_GetSlotInfo("+slotId+")");
    return pkcs11Module.C_GetSlotInfo(slotId);
  }

  /**
   * Returns slot description (terminal name)
   * @param slotID Slot ID
   * @return Slot description (terminal name)
   */
  private String getSlotDescription(long slotID) {
    try {
      if (log.isDebugEnabled())
        log.debug("Entering getSlotDescription()");
      CK_SLOT_INFO slotInfo = getSlotInfo(slotID);
      return new String(slotInfo.slotDescription);
    } catch (PKCS11Exception e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  /**
   * Returns CK_TOKEN_INFO token information
   * @param tokenHandle Token handle
   * @return Token information
   * @throws PKCS11Exception
   */
  public synchronized CK_TOKEN_INFO getTokenInfo(long tokenHandle) throws PKCS11Exception {
    if (log.isDebugEnabled())
      log.debug("Call C_GetTokenInfo("+tokenHandle+")");
    if(tokenHandle < 0) {
      throw new PKCS11Exception(CKR_TOKEN_NOT_PRESENT);
    }
    return pkcs11Module.C_GetTokenInfo(tokenHandle);
  }

  /**
   * Open session for given token
   * @param tokenHandle Token handle
   * @return Session handle
   * @throws TokenException
   */
  public synchronized long openSession(long tokenHandle) throws TokenException {
    if (log.isDebugEnabled())
      log.debug("Call C_OpenSession("+tokenHandle+")");
    long sessionHandle = pkcs11Module.C_OpenSession(tokenHandle, PKCS11Constants.CKF_SERIAL_SESSION, null, null);
    if (log.isDebugEnabled())
      log.debug("Session with handle: " + sessionHandle + " opened on token with handle: " + tokenHandle);
    return sessionHandle;
  }

  /**
   * Close session given session handle
   * @param sessionHandle Session handle to be closed
   * @throws PKCS11Exception
   */
  public synchronized void closeSession(long sessionHandle) throws PKCS11Exception {
    if (log.isDebugEnabled())
      log.debug("Call C_CloseSession("+sessionHandle+")");
    pkcs11Module.C_CloseSession(sessionHandle);
  }

  /**
   * Log into user session
   * @param passwordCallback Password callback
   * @param sessionHandle Session handle
   * @throws PKCS11Exception
   */
  public synchronized void login(PasswordInputCallback passwordCallback, long sessionHandle) throws PKCS11Exception {
    if (sessionHandle < 0) {
      throw new PKCS11Exception(CKR_SESSION_HANDLE_INVALID);
    }
    // log in as normal user
    if (log.isDebugEnabled())
      log.debug("Call C_Login("+sessionHandle+")");
    pkcs11Module.C_Login(sessionHandle, PKCS11Constants.CKU_USER, passwordCallback.getPassword(), true);
    if (log.isDebugEnabled())
      log.debug("User logged into session: " + sessionHandle);
  }

  /**
   * Log out of user session
   * @param sessionHandle Session handle
   * @throws PKCS11Exception
   */
  public synchronized void logout(long sessionHandle) throws PKCS11Exception {
    if (sessionHandle < 0) {
      throw new PKCS11Exception(CKR_SESSION_HANDLE_INVALID);
    }
    // log out
    if (log.isDebugEnabled())
      log.debug("Call C_Logout("+sessionHandle+")");
    pkcs11Module.C_Logout(sessionHandle);
    if (log.isDebugEnabled())
      log.debug("User logged out from session: " + sessionHandle);
  }

  /**
   * Get list of private key labels or identifications
   * Each value is Base64 encoded and prefixed with {CKA_ID}, but if key does not have CKA_ID
   * the CKA_LABEL is returned instead
   *
   * @param sessionHandle Session handle
   * @return List of key identifications/labels
   * @throws TokenException
   */
  public synchronized List<String> getPrivateKeyLabels(long sessionHandle) throws TokenException {
    if (log.isDebugEnabled())
      log.debug("Entering getPrivateKeyLabels()");
    List<String> keyLabels = new ArrayList<>();
    if (sessionHandle < 0) {
      throw new PKCS11Exception(CKR_SESSION_HANDLE_INVALID);
    }
    CK_ATTRIBUTE[] attributeTemplateList = {new CK_ATTRIBUTE()};
    attributeTemplateList[0].type = PKCS11Constants.CKA_CLASS;
    attributeTemplateList[0].pValue = PKCS11Constants.CKO_PRIVATE_KEY;
    if (log.isDebugEnabled())
      log.debug("Call C_FindObjectsInit("+sessionHandle+", {CKA_CLASS, CKO_PRIVATE_KEY})");
    pkcs11Module.C_FindObjectsInit(sessionHandle, attributeTemplateList, true);
    long[] objects = pkcs11Module.C_FindObjects(sessionHandle, 100);
    if (objects == null || objects.length == 0) {
      log.info("No signature key found");
    } else {
      if (log.isDebugEnabled())
        log.debug("Found " + objects.length + " signature keys");
      for (long signatureKeyHandle : objects) {
        String keyLabel = getPrivateKeyLabel(sessionHandle, signatureKeyHandle);
        if(keyLabel != null) {
          keyLabels.add(keyLabel);
        }
      }
    }
    if (log.isDebugEnabled())
      log.debug("Call C_FindObjectsFinal("+sessionHandle+")");
    pkcs11Module.C_FindObjectsFinal(sessionHandle);
    removeKeysWithoutCertificates(sessionHandle, keyLabels);
    return keyLabels;
  }

  /**
   * Returns key identification or label for given key handle
   * Value is Base64 encoded prefixed with {CKA_ID}, but if key does not have CKA_ID
   * the CKA_LABEL is returned instead
   *
   * @param sessionHandle Session handle
   * @param signatureKeyHandle Key handle
   * @return Key identification or label
   * @throws PKCS11Exception
   */
  public synchronized String getPrivateKeyLabel(long sessionHandle, long signatureKeyHandle) throws PKCS11Exception {
    if (log.isDebugEnabled())
      log.debug("Entering getPrivateKeyLabel()");
    if (sessionHandle < 0) {
      throw new PKCS11Exception(CKR_SESSION_HANDLE_INVALID);
    }
    if (signatureKeyHandle < 0) {
      throw new PKCS11Exception(CKR_KEY_HANDLE_INVALID);
    }
    CK_ATTRIBUTE[] labelTemplate = {new CK_ATTRIBUTE(),new CK_ATTRIBUTE()};
    labelTemplate[0].type = PKCS11Constants.CKA_ID;
    labelTemplate[1].type = PKCS11Constants.CKA_LABEL;
    if (log.isDebugEnabled())
      log.debug("Call C_GetAttributeValue("+sessionHandle+", "+signatureKeyHandle+", {CKA_ID, CKA_LABEL}, true)");
    pkcs11Module.C_GetAttributeValue(sessionHandle, signatureKeyHandle, labelTemplate, true);
    // get key object identification
    if (labelTemplate[0].pValue != null) {
      String ckaId = Base64.encodeBase64String((byte[]) labelTemplate[0].pValue);
      if (log.isDebugEnabled())
        log.debug("Found {CKA_ID}" + ckaId);
      return "{CKA_ID}" + ckaId;
    } else if (labelTemplate[1].pValue != null) {
      String label = new String((char[]) labelTemplate[1].pValue);
      if (log.isDebugEnabled())
        log.debug("Found {CKA_LABEL}" + label);
      return label;
    } else {
      return null;
    }
  }

  /**
   * Returns key handle
   * @param sessionHandle Session handle
   * @param label Key label (CKA_LABEL) or identification (CKA_ID)
   * @return PKCS11PrivateKey handler
   * @throws PKCS11Exception
   */
  public synchronized PKCS11PrivateKey getPrivateKey(long sessionHandle, String label) throws PKCS11Exception {
    if (log.isDebugEnabled())
      log.debug("Entering getPrivateKey()");
    long signatureKeyHandle = -1L;
    if (sessionHandle < 0) {
      throw new PKCS11Exception(CKR_SESSION_HANDLE_INVALID);
    }
    log.debug("Looking for signature key with label: '" + label + "'");
    CK_ATTRIBUTE[] attributeTemplateList = {new CK_ATTRIBUTE(), new CK_ATTRIBUTE()};
    attributeTemplateList[0].type = PKCS11Constants.CKA_CLASS;
    attributeTemplateList[0].pValue = PKCS11Constants.CKO_PRIVATE_KEY;

    // search key object via identification
    attributeTemplateList[1] = setLabelOrId(label);

    if (log.isDebugEnabled())
      log.debug("Call C_FindObjectsInit("+sessionHandle+", [{CKA_CLASS, CKO_PRIVATE_KEY}, "+label+"])");
    pkcs11Module.C_FindObjectsInit(sessionHandle, attributeTemplateList, true);

    if (log.isDebugEnabled())
      log.debug("Call C_FindObjects("+sessionHandle+", 100)");
    long[] availableSignatureKeys = pkcs11Module.C_FindObjects(sessionHandle, 100);
    //maximum of 100 at once

    if (availableSignatureKeys == null || availableSignatureKeys.length == 0) {
      log.info("No signature key found");
    } else {
      signatureKeyHandle = availableSignatureKeys[0];
    }
    if (log.isDebugEnabled())
      log.debug("Call C_FindObjectsFinal("+sessionHandle+")");
    pkcs11Module.C_FindObjectsFinal(sessionHandle);

    // get attributes
    CK_ATTRIBUTE[] authTemplate = {new CK_ATTRIBUTE()};
    authTemplate[0].type = PKCS11Constants.CKA_ALWAYS_AUTHENTICATE;
    if (log.isDebugEnabled())
      log.debug("Call C_GetAttributeValue("+sessionHandle+", "+signatureKeyHandle+", CKA_ALWAYS_AUTHENTICATE, true)");
    pkcs11Module.C_GetAttributeValue(sessionHandle, signatureKeyHandle, authTemplate, true);
    PKCS11PrivateKey key = new PKCS11PrivateKey(signatureKeyHandle, (Boolean)authTemplate[0].pValue);
    if (log.isDebugEnabled())
      log.debug("Found private key: "+key);
    return key;
  }

  /**
   * Returns byte array of encoded X509 certificate
   * @param sessionHandle Session handle
   * @param label Certificate label (CKA_LABEL) or identification (CKA_ID)
   * @return Byte array of encoded X509 certificate
   * @throws TokenException
   */
  public synchronized byte[] getCertificate(long sessionHandle, String label) throws TokenException {
    if (log.isDebugEnabled())
      log.debug("Entering getCertificate()");
    if (sessionHandle < 0) {
      throw new PKCS11Exception(CKR_SESSION_HANDLE_INVALID);
    }
    long certificateHandle = getCertificateHandle(sessionHandle, label);
    if(certificateHandle < 0){
      return null;
    }
    return getCertificateBytes(sessionHandle, certificateHandle);
  }

  /**
   * Returns byte array of encoded X509 certificate
   * @param sessionHandle Session handle
   * @param certificateHandle Certificate object handle
   * @return Byte array of encoded X509 certificate
   * @throws PKCS11Exception
   */
  public synchronized byte[] getCertificateBytes(long sessionHandle, long certificateHandle) throws PKCS11Exception {
    if (log.isDebugEnabled())
      log.debug("Entering getCertificateBytes()");
    if(certificateHandle < 0) {
      throw new PKCS11Exception(CKR_OBJECT_HANDLE_INVALID);
    }
    CK_ATTRIBUTE[] template = {new CK_ATTRIBUTE()};
    template[0].type = PKCS11Constants.CKA_VALUE;
    if (log.isDebugEnabled())
      log.debug("Call C_GetAttributeValue("+sessionHandle+", "+certificateHandle+", CKA_VALUE, true)");
    pkcs11Module.C_GetAttributeValue(sessionHandle, certificateHandle, template, true);
    return (byte[]) template[0].pValue;
  }

  /**
   * Returns certificate object handle
   * @param sessionHandle Session handle
   * @param label Certificate label (CKA_LABEL) or identification (CKA_ID)
   * @return Certificate object handle
   * @throws PKCS11Exception
   */
  public synchronized long getCertificateHandle(long sessionHandle, String label) throws PKCS11Exception {
    if (log.isDebugEnabled())
      log.debug("Entering getCertificateHandle()");
    long certificateHandle = -1L;
    if (sessionHandle < 0) {
      throw new PKCS11Exception(CKR_SESSION_HANDLE_INVALID);
    }
    CK_ATTRIBUTE[] attributeTemplateList = {new CK_ATTRIBUTE(), new CK_ATTRIBUTE()};
    attributeTemplateList[0].type = PKCS11Constants.CKA_CLASS;
    attributeTemplateList[0].pValue = PKCS11Constants.CKO_CERTIFICATE;
    // set cert object identification
    attributeTemplateList[1] = setLabelOrId(label);
    if (log.isDebugEnabled())
      log.debug("Call C_FindObjectsInit("+sessionHandle+", [{CKA_CLASS, CKO_CERTIFICATE}, "+label+"])");
    pkcs11Module.C_FindObjectsInit(sessionHandle, attributeTemplateList, true);
    if (log.isDebugEnabled())
      log.debug("Call C_FindObjects("+sessionHandle+", 100)");
    long[] objects = pkcs11Module.C_FindObjects(sessionHandle, 100);
    if (objects == null || objects.length == 0) {
      log.info("No certificate found");
    } else {
      certificateHandle = objects[0];
    }
    if (log.isDebugEnabled())
      log.debug("Call C_FindObjectsFinal("+sessionHandle+")");
    pkcs11Module.C_FindObjectsFinal(sessionHandle);
    return certificateHandle;
  }

  /**
   * Sign digested data
   * @param key Private key handle
   * @param sessionHandle Session handle
   * @param signatureMechanism Signature mechanism
   * @param data Data digest
   * @return Signature value
   * @throws PKCS11Exception
   */
  public synchronized byte[] signData(PKCS11PrivateKey key, long sessionHandle, CK_MECHANISM signatureMechanism,
                                      byte[] data, ReauthCallback callback) throws PKCS11Exception {
    byte[] signature = null;
    if (sessionHandle < 0) {
      return null;
    }
    if (log.isDebugEnabled())
      log.debug("Initialize signature operation");
    try {
      if (log.isDebugEnabled())
        log.debug("Call C_SignInit("+sessionHandle+", "+signatureMechanism+", "+key.getSignatureKeyHandle()+", true)");
      pkcs11Module.C_SignInit(sessionHandle, signatureMechanism, key.getSignatureKeyHandle(), true);
    } catch (PKCS11Exception e) {
      // check if operation is already active (user might have cancelled re-authentication in previous attempt)
      // and leave the sign operation active
      if (e.getErrorCode() != CKR_OPERATION_ACTIVE) {
        throw e;
      }
    }
    if (key.isAlwaysAuthenticate()) {
      log.info("CKA_ALWAYS_AUTHENTICATE is set to true. User needs to re-authenticate for current context.");
      reAuthenticate(sessionHandle, callback);
    }
    try {
      log.info("Signing");
      if (log.isDebugEnabled())
        log.debug("Call C_Sign("+sessionHandle+", #data#)");
      signature = pkcs11Module.C_Sign(sessionHandle, data);
      log.info("Signed");
    } catch (PKCS11Exception e) {
      log.error("Signature failed: "+e.getMessage());
      // redo the signature, maybe the attribute was not set properly
      if (e.getErrorCode() == CKR_USER_NOT_LOGGED_IN && !key.isAlwaysAuthenticate()) {
        log.warn("CKA_ALWAYS_AUTHENTICATE is set to false, but received CKR_USER_NOT_LOGGED_IN");
        key.setAlwaysAuthenticate(true); // set the attribute manually and try again
        return signData(key, sessionHandle, signatureMechanism, data, callback);
      } else {
        throw e;
      }
    }
    return signature;
  }

  /**
   * Get PKCS11 module library path
   * @return PKCS11 module library path
   */
  public String getPkcs11ModulePath() {
    return pkcs11ModulePath;
  }

  /**
   * Finalize this PKCS11 module
   * @throws Throwable
   */
  public synchronized void moduleFinalize() throws Throwable {
    log.info("Finalizing module: '" + pkcs11ModulePath + "'");
    if (log.isDebugEnabled())
      log.debug("Call C_Finalize(null)");
    pkcs11Module.C_Finalize(null);
    moduleFinalized = true;
    log.info("Finalized");
  }

  /**
   * Returns true if PKCS11 has been finalized
   * @return True if PKCS11 has been finalized
   */
  public boolean isModuleFinalized() {
    return moduleFinalized;
  }

  private CK_ATTRIBUTE setLabelOrId(String label) {
    // set cert object identification
    CK_ATTRIBUTE attribute = new CK_ATTRIBUTE();
    if(label.startsWith("{CKA_ID}")) {
      String id = label.substring("{CKA_ID}".length());
      byte[] ckaId = Base64.decodeBase64(id);
      attribute.type = PKCS11Constants.CKA_ID;
      attribute.pValue = ckaId;
    } else {
      attribute.type = PKCS11Constants.CKA_LABEL;
      attribute.pValue = label.toCharArray();
    }
    return attribute;
  }

  /**
   * Removes items from list of key labels that do not have their X509 certificate
   * @param sessionHandle Session handle
   * @param keyLabels Key/certificate label (CKA_LABEL) or identification (CKA_ID)
   * @throws TokenException
   */
  private void removeKeysWithoutCertificates(long sessionHandle, List<String> keyLabels) throws TokenException {
    if(!keyLabels.isEmpty()) {
      ListIterator<String> iterator = keyLabels.listIterator();
      while(iterator.hasNext()) {
        String keyLabel = iterator.next();
        if(getCertificateHandle(sessionHandle, keyLabel) == -1) {
          iterator.remove();
        }
      }
    }
  }

  /**
   * Re-authenticates user session for secure cryptographic operation
   *
   * @param sessionHandle Session handle
   * @param callback Callback providing a user inputted re-authentication (Q)PIN
   */
  private void reAuthenticate(long sessionHandle, ReauthCallback callback) {
    GuardedString gs = callback.getReauth();
    gs.access(chars -> {
      try {
        log.info("Private key re-authentication");
        if (log.isDebugEnabled())
          log.debug("Call C_Login("+sessionHandle+", CKU_CONTEXT_SPECIFIC)");
        pkcs11Module.C_Login(sessionHandle, CKU_CONTEXT_SPECIFIC, chars, true);
        log.info("Re-authentication applied");
      }
      catch (PKCS11Exception e) {
        throw new PKCS11RuntimeException(e.getMessage(), e);
      }
    });
  }

}

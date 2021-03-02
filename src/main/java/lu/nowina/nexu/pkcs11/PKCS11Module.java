/**
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Licensed under EUPL Version 1.1 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 *
 * http://ec.europa.eu/idabc/eupl5
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
 * lu.nowina.nexu.pkcs11.PKCS11Module
 *
 * Created: 28.01.2021
 * Author: hlavnicka
 */

import eu.europa.esig.dss.token.PasswordInputCallback;
import iaik.pkcs.pkcs11.TokenException;
import iaik.pkcs.pkcs11.wrapper.*;
import org.apache.commons.codec.binary.Base64;
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
    log.info("Initialization");

    log.info("Trying to connect to PKCS#11 module: '" + pkcs11ModulePath + "'");
    pkcs11Module = PKCS11Connector.connectToPKCS11Module(pkcs11ModulePath);
    log.info("Connected");

    log.debug("Initializing module: '" + pkcs11ModulePath + "'");
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
    return pkcs11Module.C_GetSlotList(tokenPresent);
  }

  /**
   * Returns CK_SLOT_INFO slot information
   * @param slotId Slot ID
   * @return Slot information
   * @throws PKCS11Exception
   */
  public synchronized CK_SLOT_INFO getSlotInfo(long slotId) throws PKCS11Exception {
    return pkcs11Module.C_GetSlotInfo(slotId);
  }

  /**
   * Returns slot description (terminal name)
   * @param slotID Slot ID
   * @return Slot description (terminal name)
   */
  private String getSlotDescription(long slotID) {
    try {
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
    long sessionHandle = pkcs11Module.C_OpenSession(tokenHandle, PKCS11Constants.CKF_SERIAL_SESSION, null, null);
    log.debug("Session with handle: " + sessionHandle + " opened on token with handle: " + tokenHandle);
    return sessionHandle;
  }

  /**
   * Close session given session handle
   * @param sessionHandle Session handle to be closed
   * @throws PKCS11Exception
   */
  public synchronized void closeSession(long sessionHandle) throws PKCS11Exception {
    log.debug("Closing session with handle: " + sessionHandle);
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
    pkcs11Module.C_Login(sessionHandle, PKCS11Constants.CKU_USER, passwordCallback.getPassword(), true);
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
    pkcs11Module.C_Logout(sessionHandle);
    log.debug("User logged out from session: " + sessionHandle);
  }

  /**
   * Get list of private key labels or identifications
   * If key does not have CKA_LABEL, the CKA_ID is returned instead and the value is prefixed with {CKA_ID}
   * @param sessionHandle Session handle
   * @return List of key labels/identifications
   * @throws TokenException
   */
  public synchronized List<String> getPrivateKeyLabels(long sessionHandle) throws TokenException {
    List<String> keyLabels = new ArrayList<>();
    if (sessionHandle < 0) {
      throw new PKCS11Exception(CKR_SESSION_HANDLE_INVALID);
    }
    CK_ATTRIBUTE[] attributeTemplateList = {new CK_ATTRIBUTE()};
    attributeTemplateList[0].type = PKCS11Constants.CKA_CLASS;
    attributeTemplateList[0].pValue = PKCS11Constants.CKO_PRIVATE_KEY;
    pkcs11Module.C_FindObjectsInit(sessionHandle, attributeTemplateList, true);
    long[] objects = pkcs11Module.C_FindObjects(sessionHandle, 100);
    if (objects == null || objects.length == 0) {
      log.info("No signature key found");
    } else {
      log.debug("Found " + objects.length + " signature keys");
      for (long signatureKeyHandle : objects) {
        String keyLabel = getPrivateKeyLabel(sessionHandle, signatureKeyHandle);
        if(keyLabel != null) {
          keyLabels.add(keyLabel);
        }
      }
    }
    pkcs11Module.C_FindObjectsFinal(sessionHandle);
    removeKeysWithoutCertificates(sessionHandle, keyLabels);
    return keyLabels;
  }

  /**
   * Returns key label or identification for given key handle
   * If key does not have CKA_LABEL, the CKA_ID is returned instead and the value is prefixed with {CKA_ID}
   * @param sessionHandle Session handle
   * @param signatureKeyHandle Key handle
   * @return Key label/identification
   * @throws PKCS11Exception
   */
  public synchronized String getPrivateKeyLabel(long sessionHandle, long signatureKeyHandle) throws PKCS11Exception {
    if (sessionHandle < 0) {
      throw new PKCS11Exception(CKR_SESSION_HANDLE_INVALID);
    }
    if (signatureKeyHandle < 0) {
      throw new PKCS11Exception(CKR_KEY_HANDLE_INVALID);
    }
    CK_ATTRIBUTE[] labelTemplate = {new CK_ATTRIBUTE(),new CK_ATTRIBUTE()};
    labelTemplate[0].type = PKCS11Constants.CKA_ID;
    labelTemplate[1].type = PKCS11Constants.CKA_LABEL;
    pkcs11Module.C_GetAttributeValue(sessionHandle, signatureKeyHandle, labelTemplate, true);
    // get key object identification
    // TODO - evidovat oba identifikatory? v evidenci asi nema smysl ukazovat CKA_ID jako nazev klice
    if (labelTemplate[0].pValue != null)
      return "{CKA_ID}" + Base64.encodeBase64String((byte[])labelTemplate[0].pValue);
    else if (labelTemplate[1].pValue != null)
      return new String((char[]) labelTemplate[1].pValue);
    else
      return null;
  }

  /**
   * Returns key handle
   * @param sessionHandle Session handle
   * @param label Key label (CKA_LABEL) or identification (CKA_ID)
   * @return Key handle
   * @throws PKCS11Exception
   */
  public synchronized long getPrivateKey(long sessionHandle, String label) throws PKCS11Exception {
    long signatureKeyHandle = -1L;
    if (sessionHandle < 0) {
      throw new PKCS11Exception(CKR_SESSION_HANDLE_INVALID);
    }
    log.debug("Finding signature key with label: '" + label + "'");
    CK_ATTRIBUTE[] attributeTemplateList = {new CK_ATTRIBUTE(), new CK_ATTRIBUTE()};
    attributeTemplateList[0].type = PKCS11Constants.CKA_CLASS;
    attributeTemplateList[0].pValue = PKCS11Constants.CKO_PRIVATE_KEY;

    // search key object via identification
    attributeTemplateList[1] = setLabelOrId(label);

    pkcs11Module.C_FindObjectsInit(sessionHandle, attributeTemplateList, true);
    long[] availableSignatureKeys = pkcs11Module.C_FindObjects(sessionHandle, 100);
    //maximum of 100 at once

    if (availableSignatureKeys == null || availableSignatureKeys.length == 0) {
      log.info("No signature key found");
    } else {
      signatureKeyHandle = availableSignatureKeys[0];
    }
    pkcs11Module.C_FindObjectsFinal(sessionHandle);
    return signatureKeyHandle;
  }

  /**
   * Returns byte array of encoded X509 certificate
   * @param sessionHandle Session handle
   * @param label Certificate label (CKA_LABEL) or identification (CKA_ID)
   * @return Byte array of encoded X509 certificate
   * @throws TokenException
   */
  public synchronized byte[] getCertificate(long sessionHandle, String label) throws TokenException {
    if (sessionHandle < 0) {
      throw new PKCS11Exception(CKR_SESSION_HANDLE_INVALID);
    }
    long certificateHandle = getCertificateHandle(sessionHandle, label);
    if(certificateHandle < 0){
      return null;
    }
    return getCertificate(sessionHandle, certificateHandle);
  }

  /**
   * Returns byte array of encoded X509 certificate
   * @param sessionHandle Session handle
   * @param certificateHandle Certificate object handle
   * @return Byte array of encoded X509 certificate
   * @throws PKCS11Exception
   */
  public synchronized byte[] getCertificate(long sessionHandle, long certificateHandle) throws PKCS11Exception {
    if(certificateHandle < 0) {
      throw new PKCS11Exception(CKR_OBJECT_HANDLE_INVALID);
    }
    CK_ATTRIBUTE[] template = {new CK_ATTRIBUTE()};
    template[0].type = PKCS11Constants.CKA_VALUE;
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
    long certificateHandle = -1L;
    if (sessionHandle < 0) {
      throw new PKCS11Exception(CKR_SESSION_HANDLE_INVALID);
    }
    CK_ATTRIBUTE[] attributeTemplateList = {new CK_ATTRIBUTE(), new CK_ATTRIBUTE()};
    attributeTemplateList[0].type = PKCS11Constants.CKA_CLASS;
    attributeTemplateList[0].pValue = PKCS11Constants.CKO_CERTIFICATE;
    // set cert object identification
    // TODO - mozna zkusit obe hodnoty postupne (kdyby nahodou certifikat nemel stejne CKA_ID, ale CKA_LABEL ano?)
    attributeTemplateList[1] = setLabelOrId(label);

    pkcs11Module.C_FindObjectsInit(sessionHandle, attributeTemplateList, true);
    long[] objects = pkcs11Module.C_FindObjects(sessionHandle, 100);
    if (objects == null || objects.length == 0) {
      log.info("No certificate found");
    } else {
      certificateHandle = objects[0];
    }
    pkcs11Module.C_FindObjectsFinal(sessionHandle);
    return certificateHandle;
  }

  /**
   * Sign digested data
   * @param signatureKeyHandle Signature key handle
   * @param sessionHandle Session handle
   * @param signatureMechanism Signature mechanism
   * @param data Data digest
   * @return Signature value
   * @throws PKCS11Exception
   */
  public synchronized byte[] signData(long signatureKeyHandle, long sessionHandle, CK_MECHANISM signatureMechanism, byte[] data) throws PKCS11Exception {
    byte[] signature = null;
    if (sessionHandle < 0) {
      return null;
    }
    log.debug("Initialize signature operation");
    pkcs11Module.C_SignInit(sessionHandle, signatureMechanism, signatureKeyHandle, true);
    if ((data.length > 0) && (data.length < 1024)) {
      log.debug("Signing");
      signature = pkcs11Module.C_Sign(sessionHandle, data);
      log.debug("Signed");
    } else {
      throw new PKCS11Exception(CKR_DATA_LEN_RANGE);
    }
    return signature;
  }

  /**
   * Finalize this PKCS11 module
   * @throws Throwable
   */
  public synchronized void moduleFinalize() throws Throwable {
    log.debug("Finalizing module: '" + pkcs11ModulePath + "'");
    pkcs11Module.C_Finalize(null);
    moduleFinalized = true;
    log.debug("Finalized");
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

}

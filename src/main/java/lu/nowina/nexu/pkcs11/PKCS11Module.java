package lu.nowina.nexu.pkcs11;

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
    log.debug("Initialization");

    log.debug("Trying to connect to PKCS#11 module: '" + pkcs11ModulePath + "'");
    pkcs11Module = PKCS11Connector.connectToPKCS11Module(pkcs11ModulePath);
    log.debug("Connected");

    log.debug("Initializing module: '" + pkcs11ModulePath + "'");
    pkcs11Module.C_Initialize(null, true);
    log.debug("Initialized");
  }

  public synchronized long getTokenInReader(String reader) throws PKCS11Exception {
    long[] tokens = getSlotList(true);
    for (long token : tokens) {
      String slotTerminal = getSlotDescription(token);
      if(slotTerminal != null)
        slotTerminal = slotTerminal.trim();
      // TODO proc takto slozite?
/*
      String slotTerminalTrimmed = slotTerminal.replaceAll(" ", "");
      String readerTrimmed = reader.replaceAll(" ", "");
      slotTerminal = slotTerminal.substring(0, slotTerminal.length() - 1);
      if ((slotTerminal.startsWith(reader)) || (readerTrimmed.endsWith(slotTerminalTrimmed))) {
        return token;
      }
*/
      if(reader.trim().equals(slotTerminal)) {
        return token;
      }
    }
    return -1;
  }

  public synchronized long[] getSlotList(boolean tokenPresent) throws PKCS11Exception {
    return pkcs11Module.C_GetSlotList(tokenPresent);
  }

  public synchronized CK_SLOT_INFO getSlotInfo(long slotId) throws PKCS11Exception {
    return pkcs11Module.C_GetSlotInfo(slotId);
  }

  public String getSlotDescription(long slotID) {
    try {
      CK_SLOT_INFO slotInfo = getSlotInfo(slotID);
      return new String(slotInfo.slotDescription);
    } catch (PKCS11Exception e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  public synchronized long openSession(long tokenHandle) throws TokenException {
    long sessionHandle = pkcs11Module.C_OpenSession(tokenHandle, PKCS11Constants.CKF_SERIAL_SESSION, null, null);
    log.debug("Session with handle: " + sessionHandle + " opened on token with handle: " + tokenHandle);
    return sessionHandle;
  }

  public synchronized boolean login(PasswordInputCallback passwordCallback, long sessionHandle) throws PKCS11Exception {
    if (sessionHandle < 0) {
      throw new PKCS11Exception(CKR_SESSION_HANDLE_INVALID);
    }
    // log in as normal user
    pkcs11Module.C_Login(sessionHandle, PKCS11Constants.CKU_USER, passwordCallback.getPassword(), true);
    log.debug("User logged into session: " + sessionHandle);
    return true;
  }

  public synchronized void logout(long sessionHandle) throws PKCS11Exception {
    if (sessionHandle < 0) {
      throw new PKCS11Exception(CKR_SESSION_HANDLE_INVALID);
    }
    // log out
    pkcs11Module.C_Logout(sessionHandle);
    log.debug("User logged out from session: " + sessionHandle);
  }

  public synchronized void closeSession(long sessionHandle) throws PKCS11Exception {
    log.debug("Closing session with handle: " + sessionHandle);
    pkcs11Module.C_CloseSession(sessionHandle);
  }

  public synchronized CK_TOKEN_INFO getTokenInfo(long tokenHandle) throws PKCS11Exception {
    if(tokenHandle < 0) {
      throw new PKCS11Exception(CKR_TOKEN_NOT_PRESENT);
    }
    return pkcs11Module.C_GetTokenInfo(tokenHandle);
  }

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
        if(keyLabel != null) { // TODO CKA_ID?
          keyLabels.add(keyLabel);
        }
      }
    }
    pkcs11Module.C_FindObjectsFinal(sessionHandle);
    removeKeysWithoutCertificates(sessionHandle, keyLabels);
    return keyLabels;
  }

  public synchronized String getPrivateKeyLabel(long sessionHandle, long signatureKeyHandle) throws PKCS11Exception {
    if (sessionHandle < 0) {
      throw new PKCS11Exception(CKR_SESSION_HANDLE_INVALID);
    }
    if (signatureKeyHandle < 0) {
      throw new PKCS11Exception(CKR_KEY_HANDLE_INVALID);
    }
    CK_ATTRIBUTE[] labelTemplate = {new CK_ATTRIBUTE(),new CK_ATTRIBUTE()};
    labelTemplate[0].type = PKCS11Constants.CKA_LABEL;
    labelTemplate[1].type = PKCS11Constants.CKA_ID;
    pkcs11Module.C_GetAttributeValue(sessionHandle, signatureKeyHandle, labelTemplate, true);
    // get key object identification
    if (labelTemplate[0].pValue != null)
      return new String((char[]) labelTemplate[0].pValue);
    else if (labelTemplate[1].pValue != null)
      return "{CKA_ID}" + Base64.encodeBase64String((byte[])labelTemplate[1].pValue);
    else
      return null;
  }

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

  public synchronized byte[] getCertificate(long sessionHandle, long certificateHandle) throws PKCS11Exception {
    if(certificateHandle < 0) {
      throw new PKCS11Exception(CKR_OBJECT_HANDLE_INVALID);
    }
    CK_ATTRIBUTE[] template = {new CK_ATTRIBUTE()};
    template[0].type = PKCS11Constants.CKA_VALUE;
    pkcs11Module.C_GetAttributeValue(sessionHandle, certificateHandle, template, true);
    return (byte[]) template[0].pValue;
  }

  public synchronized long getCertificateHandle(long sessionHandle, String label) throws PKCS11Exception {
    long certificateHandle = -1L;
    if (sessionHandle < 0) {
      throw new PKCS11Exception(CKR_SESSION_HANDLE_INVALID);
    }
    CK_ATTRIBUTE[] attributeTemplateList = {new CK_ATTRIBUTE(), new CK_ATTRIBUTE()};
    attributeTemplateList[0].type = PKCS11Constants.CKA_CLASS;
    attributeTemplateList[0].pValue = PKCS11Constants.CKO_CERTIFICATE;
    // set cert object identification
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

  public boolean isModuleFinalized() {
    return moduleFinalized;
  }

  public synchronized void moduleFinalize() throws Throwable {
    log.debug("Finalizing module: '" + pkcs11ModulePath + "'");
    pkcs11Module.C_Finalize(null);
    moduleFinalized = true;
    log.debug("Finalized");
  }

  public synchronized long waitForSlotEvent() throws PKCS11Exception {
    return pkcs11Module.C_WaitForSlotEvent(CKF_DONT_BLOCK, null);
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

  private void removeKeysWithoutCertificates(long sessionHandle, List<String> keyLabels) throws TokenException {
    if(!keyLabels.isEmpty()) {
      ListIterator<String> iterator = keyLabels.listIterator();
      while(iterator.hasNext()) {
        String keyLabel = iterator.next();
        /* TODO - remove */
        try {
          byte[] c = getCertificate(sessionHandle, keyLabel);
        }catch (Exception e) {
          e.printStackTrace();
        }
        try {
          byte[] c = getCertificate(sessionHandle, keyLabel);
        }catch (Exception e) {
          e.printStackTrace();
        }
        /**/
        if(getCertificateHandle(sessionHandle, keyLabel) == -1) {
          iterator.remove();
        }
      }
    }
  }

}

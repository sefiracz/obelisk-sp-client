package lu.nowina.nexu.pkcs11;

import eu.europa.esig.dss.token.PasswordInputCallback;
import iaik.pkcs.pkcs11.TokenException;
import iaik.pkcs.pkcs11.wrapper.CK_MECHANISM;
import iaik.pkcs.pkcs11.wrapper.CK_TOKEN_INFO;
import iaik.pkcs.pkcs11.wrapper.PKCS11Constants;
import iaik.pkcs.pkcs11.wrapper.PKCS11Exception;
import lu.nowina.nexu.api.DetectedCard;
import lu.nowina.nexu.generic.ProductPasswordManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.X509Certificate;
import java.util.List;

public class TokenHandler {

  private static final Logger log = LoggerFactory.getLogger(TokenHandler.class.getName());

  private final PKCS11Module pkcs11Module;
  private final String reader;

  private CK_TOKEN_INFO tokenInfo;

  private long sessionHandle = -1;
  private long tokenHandle = -1;

  public TokenHandler(PKCS11Module pkcs11Module, String reader) {
    this.pkcs11Module = pkcs11Module;
    this.reader = reader;
  }

  public void openSession() throws TokenException {
    checkSessionState();
    if(sessionHandle < 0) {
      try {
        this.tokenHandle = pkcs11Module.getTokenInReader(reader);
        this.sessionHandle = pkcs11Module.openSession(tokenHandle);
        this.tokenInfo = pkcs11Module.getTokenInfo(tokenHandle);
      } catch (Exception e) {
        this.tokenHandle = -1;
        this.sessionHandle = -1;
        throw e;
      }
    }
  }

  public void closeSession() {
    if(sessionHandle > 0) {
      // close session
      try {
        pkcs11Module.closeSession(sessionHandle);
      } catch (PKCS11Exception e) {
        log.error(e.getMessage());
      }
    }
    this.tokenHandle = -1;
    this.sessionHandle = -1;
  }


  // TODO - prepsat logiku logovani
  public void login(PasswordInputCallback callback) throws PKCS11Exception {
    if (callback != null)
      pkcs11Module.login(callback, sessionHandle);
//    try {
//
//    } catch (PKCS11Exception e) {
//      log.warn(e.getMessage());
//    }
  }

  public void logout() {
    try {
      pkcs11Module.logout(sessionHandle);
    }
    catch (PKCS11Exception e) {
      log.warn(e.getMessage());
    }
  }

  public void checkSessionState() {
    try {
      long slotId = pkcs11Module.waitForSlotEvent();
      if(slotId == tokenHandle) {
        ProductPasswordManager.getInstance().destroy();
        closeSession();
      }
    } catch (PKCS11Exception e) {
      log.debug(e.getMessage());
    }
  }

  /**
   * Returns list of labels/identifications of private keys on device that also have X509 certificates.
   * Lone keys are skipped.
   * @return List of labels/IDs
   * @throws TokenException
   */
  public List<String> getPrivateKeyLabels() throws TokenException {
    return pkcs11Module.getPrivateKeyLabels(sessionHandle);
  }

  /**
   * Returns certificated that is identified by given label/identification
   * @param label Used label/identification
   * @return X509 certificate
   * @throws TokenException
   */
  public byte[] getCertificate(String label) throws TokenException {
    return pkcs11Module.getCertificate(sessionHandle, label);
  }

  /**
   * Sign data using a key with given label
   * @param keyLabel Used private key label
   * @param x509Certificate X509 certificate that is public part of private key
   * @param data Digest data to be signed
   * @return Signature value
   * @throws PKCS11Exception
   */
  public byte[] sign(String keyLabel, X509Certificate x509Certificate, byte[] data) throws PKCS11Exception {
    long signatureKeyHandle = pkcs11Module.getPrivateKey(sessionHandle, keyLabel);
    CK_MECHANISM signatureMechanism;
    String algorithm = x509Certificate != null ? x509Certificate.getPublicKey().getAlgorithm() : "RSA";
    switch (algorithm) {
      case "RSA":
        signatureMechanism = getMechanism(PKCS11Constants.CKM_RSA_PKCS);
        break;
      case "EC":
        signatureMechanism = getMechanism(PKCS11Constants.CKM_ECDSA);
        break;
      case "DSA":
        signatureMechanism = getMechanism(PKCS11Constants.CKM_DSA);
        break;
      default:
        throw new IllegalStateException("Unexpected key algorithm: "+algorithm);
    }
    return pkcs11Module.signData(signatureKeyHandle, sessionHandle, signatureMechanism, data);
  }

  public String getTokenLabel() {
    return new String(tokenInfo.label).trim();
  }

  public String getTokenSerial() {
    return new String(tokenInfo.serialNumber).trim();
  }

  public String getTokenManufacturer() {
    return new String(tokenInfo.manufacturerID).trim();
  }

  // TODO
  public void setTokenInfo(DetectedCard card) throws TokenException {
    openSession();
    card.setTokenLabel(getTokenLabel());
    card.setTokenSerial(getTokenSerial());
    card.setTokenManufacturer(getTokenManufacturer());
    closeSession();
  }

  private CK_MECHANISM getMechanism(long mechanism) {
    CK_MECHANISM signatureMechanism = new CK_MECHANISM();
    signatureMechanism.mechanism = mechanism;
    signatureMechanism.pParameter = null;
    return signatureMechanism;
  }

}

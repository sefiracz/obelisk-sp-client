package lu.nowina.nexu.pkcs11;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.pkcs11.TokenHandler
 *
 * Created: 28.01.2021
 * Author: hlavnicka
 */

import eu.europa.esig.dss.token.PasswordInputCallback;
import iaik.pkcs.pkcs11.TokenException;
import iaik.pkcs.pkcs11.wrapper.CK_MECHANISM;
import iaik.pkcs.pkcs11.wrapper.CK_TOKEN_INFO;
import iaik.pkcs.pkcs11.wrapper.PKCS11Constants;
import iaik.pkcs.pkcs11.wrapper.PKCS11Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.X509Certificate;
import java.util.List;

public class TokenHandler {

  private static final Logger log = LoggerFactory.getLogger(TokenHandler.class.getName());

  private final PKCS11Module pkcs11Module;
  private final String terminalLabel;

  private CK_TOKEN_INFO tokenInfo;
  private long sessionHandle = -1;
  private long tokenHandle = -1;

  public TokenHandler(PKCS11Module pkcs11Module, String terminalLabel) {
    this.pkcs11Module = pkcs11Module;
    this.terminalLabel = terminalLabel;
  }

  /**
   * Initializes token in terminal and gets token information
   * @throws PKCS11Exception
   */
  public void initialize() throws PKCS11Exception {
    try {
      this.tokenHandle = pkcs11Module.getTokenInTerminal(terminalLabel);
      this.tokenInfo = pkcs11Module.getTokenInfo(tokenHandle);
    } catch (Exception e) {
      this.tokenHandle = -1;
      this.sessionHandle = -1;
      throw e;
    }
  }

  /**
   * Open new session
   * @return Returns session handle or -1 if session has not been opened
   */
  public long openSession() {
    if(sessionHandle < 0) {
      try {
        this.sessionHandle = pkcs11Module.openSession(tokenHandle);
      } catch (TokenException e) {
        log.error("Unable to open session: "+e.getMessage(), e);
      }
    }
    return sessionHandle;
  }

  /**
   * Closes session and token handle
   */
  public void closeSession() {
    if(sessionHandle > 0) {
      // close session
      try {
        pkcs11Module.closeSession(sessionHandle);
      } catch (PKCS11Exception e) {
        log.warn("Unable to close session: "+e.getMessage());
      }
    }
    this.tokenHandle = -1;
    this.sessionHandle = -1;
  }

  /**
   * Log in user session
   * @param callback PasswordInput callback
   * @throws PKCS11Exception Login failed
   */
  public void login(PasswordInputCallback callback) throws PKCS11Exception {
    if (callback != null && sessionHandle > 0) {
      pkcs11Module.login(callback, sessionHandle);
    }
  }

  /**
   * Log out of user session
   */
  public void logout() throws PKCS11Exception {
    pkcs11Module.logout(sessionHandle);
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
   * @param x509Certificate X509 certificate that is public part of this private key
   * @param data Digest data (wrapped in ASN1 structure) to be signed
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

  /**
   * Get token label
   * @return Token label
   */
  public String getTokenLabel() {
    return new String(tokenInfo.label).trim();
  }

  /**
   * Get token serial number
   * @return Token serial number
   */
  public String getTokenSerial() {
    return new String(tokenInfo.serialNumber).trim();
  }


  /**
   * Get token manufacturer name
   * @return Token manufacturer name
   */
  public String getTokenManufacturer() {
    return new String(tokenInfo.manufacturerID).trim();
  }

  /**
   * Creates CK_MECHANISM object
   * @param mechanism PKCS11Constants mechanism constant
   * @return CK_MECHANISM object
   */
  private CK_MECHANISM getMechanism(long mechanism) {
    CK_MECHANISM signatureMechanism = new CK_MECHANISM();
    signatureMechanism.mechanism = mechanism;
    signatureMechanism.pParameter = null;
    return signatureMechanism;
  }

}

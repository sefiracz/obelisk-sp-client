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
package cz.sefira.obelisk.pkcs11;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.pkcs11.TokenHandler
 *
 * Created: 28.01.2021
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.ReauthCallback;
import eu.europa.esig.dss.DigestAlgorithm;
import eu.europa.esig.dss.SignatureAlgorithm;
import eu.europa.esig.dss.token.PasswordInputCallback;
import iaik.pkcs.pkcs11.TokenException;
import iaik.pkcs.pkcs11.wrapper.*;
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
      if(log.isDebugEnabled())
        log.debug(tokenInfo.toString());
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
   * @param signatureAlgorithm Used signature algorithm
   * @param data Digest data (wrapped in ASN1 structure) to be signed
   * @return Signature value
   * @throws PKCS11Exception
   */
  public byte[] sign(String keyLabel, SignatureAlgorithm signatureAlgorithm, byte[] data, ReauthCallback callback)
      throws PKCS11Exception {
    PKCS11PrivateKey key = pkcs11Module.getPrivateKey(sessionHandle, keyLabel);
    // determine signature algorithm mechanism
    CK_MECHANISM signatureMechanism = getMechanism(signatureAlgorithm);
    return pkcs11Module.signData(key, sessionHandle, signatureMechanism, data, callback);
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
   * @param signatureAlgorithm signature algorithm
   * @return CK_MECHANISM object
   */
  private CK_MECHANISM getMechanism(SignatureAlgorithm signatureAlgorithm) {
    DigestAlgorithm digestAlgorithm = signatureAlgorithm.getDigestAlgorithm();
    CK_MECHANISM signatureMechanism = new CK_MECHANISM();
    signatureMechanism.pParameter = null;
    switch (signatureAlgorithm.getEncryptionAlgorithm()) {
      // RSA
      case RSA:
        if (signatureAlgorithm.getMaskGenerationFunction() == null) {
          // RSA PKCS#1 v1.5
          signatureMechanism.mechanism = PKCS11Constants.CKM_RSA_PKCS;
        } else {
          // RSA-PSS PKCS#1 v2.1
          CK_RSA_PKCS_PSS_PARAMS pssParams = new CK_RSA_PKCS_PSS_PARAMS();
          pssParams.sLen = digestAlgorithm.getSaltLength();
          switch (digestAlgorithm) {
            case SHA1:
              signatureMechanism.mechanism = PKCS11Constants.CKM_SHA1_RSA_PKCS_PSS;
              pssParams.hashAlg = PKCS11Constants.CKM_SHA_1;
              pssParams.mgf = PKCS11Constants.CKG_MGF1_SHA1;
              break;
            case SHA224:
            case SHA3_224:
              signatureMechanism.mechanism = PKCS11Constants.CKM_SHA224_RSA_PKCS_PSS;
              pssParams.hashAlg = PKCS11Constants.CKM_SHA224;
              pssParams.mgf = PKCS11Constants.CKG_MGF1_SHA224;
              break;
            case SHA256:
            case SHA3_256:
              signatureMechanism.mechanism = PKCS11Constants.CKM_SHA256_RSA_PKCS_PSS;
              pssParams.hashAlg = PKCS11Constants.CKM_SHA256;
              pssParams.mgf = PKCS11Constants.CKG_MGF1_SHA256;
              break;
            case SHA384:
            case SHA3_384:
              signatureMechanism.mechanism = PKCS11Constants.CKM_SHA384_RSA_PKCS_PSS;
              pssParams.hashAlg = PKCS11Constants.CKM_SHA384;
              pssParams.mgf = PKCS11Constants.CKG_MGF1_SHA384;
              break;
            case SHA512:
            case SHA3_512:
              signatureMechanism.mechanism = PKCS11Constants.CKM_SHA512_RSA_PKCS_PSS;
              pssParams.hashAlg = PKCS11Constants.CKM_SHA512;
              pssParams.mgf = PKCS11Constants.CKG_MGF1_SHA512;
              break;
            default:
              throw new IllegalStateException("Unexpected digest algorithm: "+digestAlgorithm.getName());
          }
          signatureMechanism.pParameter = pssParams;
          break;
        }
        break;
      // ECDSA
      case ECDSA:
        switch (digestAlgorithm) {
          case SHA1:
            signatureMechanism.mechanism = PKCS11Constants.CKM_ECDSA_SHA1;
            break;
          case SHA224:
            signatureMechanism.mechanism = PKCS11Constants.CKM_ECDSA_SHA224;
            break;
          case SHA256:
            signatureMechanism.mechanism = PKCS11Constants.CKM_ECDSA_SHA256;
            break;
          case SHA384:
            signatureMechanism.mechanism = PKCS11Constants.CKM_ECDSA_SHA384;
            break;
          case SHA512:
            signatureMechanism.mechanism = PKCS11Constants.CKM_ECDSA_SHA512;
            break;
          default:
            throw new IllegalStateException("Unexpected digest algorithm: "+digestAlgorithm.getName());
        }
        break;
      // DSA
      case DSA:
        switch (digestAlgorithm) {
          case SHA1:
            signatureMechanism.mechanism = PKCS11Constants.CKM_DSA_SHA1;
            break;
          case SHA224:
            signatureMechanism.mechanism = PKCS11Constants.CKM_DSA_SHA224;
            break;
          case SHA256:
            signatureMechanism.mechanism = PKCS11Constants.CKM_DSA_SHA256;
            break;
          case SHA384:
            signatureMechanism.mechanism = PKCS11Constants.CKM_DSA_SHA384;
            break;
          case SHA512:
            signatureMechanism.mechanism = PKCS11Constants.CKM_DSA_SHA512;
            break;
          default:
            throw new IllegalStateException("Unexpected digest algorithm: "+digestAlgorithm.getName());
        }
        break;
      default:
        throw new IllegalStateException("Unexpected key algorithm: "+signatureAlgorithm.getEncryptionAlgorithm());
    }
    return signatureMechanism;
  }

}

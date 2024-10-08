/**
 * © SEFIRA spol. s r.o., 2020-2023
 * <p>
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 * <p>
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 * <p>
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package cz.sefira.obelisk.generic;

import cz.sefira.obelisk.util.DSSUtils;
import cz.sefira.obelisk.api.AbstractProduct;
import cz.sefira.obelisk.dss.DigestAlgorithm;
import cz.sefira.obelisk.dss.token.SignatureTokenConnection;
import org.apache.commons.codec.binary.Base64;
import org.identityconnectors.common.security.GuardedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class SessionManager {

  private static final Logger logger = LoggerFactory.getLogger(SessionManager.class.getName());

  private static volatile SessionManager manager;

  private AbstractProduct product = null;
  private TempTokenStorage tokenStorage = null;
  private TempSecretStorage secretStorage = null;

  private Certificate trustCert;
  private byte[] sessionDigest;

  private SessionManager() {
    try {
      KeyStore ks = KeyStore.getInstance("JKS");
      ks.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("session-trust.jks"),
          "password".toCharArray());
      trustCert = ks.getCertificate("client-session");
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      logger.error("Unable to access session truststore and certificate \"client-session\": "+e.getMessage(), e);
    }
  }

  public synchronized static SessionManager getManager() {
    if(manager == null) {
      manager = new SessionManager();
    }
    return manager;
  }

  public SignatureTokenConnection getInitializedTokenForProduct(AbstractProduct product) {
    if(product.getCertificateId() == null || !product.matchToken(this.product)) {
      destroy(); // close previous token
      return null; // different product
    }
    return tokenStorage.getToken();
  }

  public boolean isCurrentlyInitializedToken(AbstractProduct product) {
    return product != null && product.getCertificateId() != null && product.matchToken(this.product);
  }

  public void setToken(AbstractProduct product, SignatureTokenConnection token) {
    this.product = product;
    if(this.tokenStorage != null) {
      this.tokenStorage.cancel(); // cancel previous timer
    }
    this.tokenStorage = new TempTokenStorage(token);
    this.tokenStorage.startTimer(); // start new timer
  }

  public void setSecret(GuardedString secret, int duration) {
    if (product != null) {
      if(this.secretStorage != null) {
        this.secretStorage.cancel(); // cancel previous timer
      }
      this.secretStorage = new TempSecretStorage(secret, duration);
      this.secretStorage.startTimer();
    }
  }

  public GuardedString getSecret() {
    if(this.secretStorage != null) {
      return secretStorage.getSecret();
    }
    return null;
  }

  public void destroy() {
    logger.info("Destroying session");
    if(this.tokenStorage != null) {
      this.tokenStorage.destroy();
      this.tokenStorage.cancel();
    }
    this.tokenStorage = null;
    this.product = null;
    destroySecret();
  }

  public void destroySecret() {
    if(this.secretStorage != null) {
      this.secretStorage.destroy();
      this.secretStorage.cancel();
    }
    this.secretStorage = null;
  }

  public void destroy(AbstractProduct product) {
    if(product.equals(this.product)) {
      destroy();
    }
  }

  /**
   * Check if session is valid
   * @param sessionId SessionID value (UUID+UnixTimestamp)
   * @param sessionSignature Base64 SignatureValue of SHA512 digest of SessionID
   * @return True or false if given session value is valid
   * @throws InvalidSessionException When sent session values are invalid and unusable
   */
  public boolean checkSession(String sessionId, String sessionSignature) throws InvalidSessionException {
    try {
      // check mandatory values
      if(sessionId == null || sessionSignature == null)
        throw new InvalidSessionException("Missing SessionID and Signature values.");

      if(trustCert == null)
        throw new InvalidSessionException("Trusted certificate not initialized.");

      // get SessionID values
      String[] sessionIdValues = sessionId.split("\\+"); // sessionID+sessionTimestamp
      if (sessionIdValues.length != 2) {
        throw new InvalidSessionException("Invalid SessionID format.");
      }

      // check timestamp
      String sessionTimestamp = sessionIdValues[1];
      long now = System.currentTimeMillis();
      try {
        long timestamp = Long.parseLong(sessionTimestamp);
        if (now - timestamp > 45000*1000) { // 12.5 hours
          throw new InvalidSessionException("Session expired.");
        }
      } catch (NumberFormatException e) {
        throw new InvalidSessionException("Invalid SessionID timestamp format.");
      }

      // check signature
      byte[] signatureValue = Base64.decodeBase64(sessionSignature);
      byte[] signedDigest = verifySignature(signatureValue, trustCert);
      if(signedDigest == null) // check decrypted value
        throw new InvalidSessionException("Session signature invalid.");

      // check SessionID digest
      byte[] sessionDigest = DSSUtils.digest(DigestAlgorithm.SHA512, sessionId.getBytes(StandardCharsets.UTF_8));
      if(!Arrays.equals(sessionDigest, signedDigest))
        throw new InvalidSessionException("Session values do not correspond.");

      return Arrays.equals(this.sessionDigest, sessionDigest); // check if sessionId value is correct
    } catch (InvalidSessionException e) {
      logger.error(e.getMessage(), e); // invalid session values
      throw e;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new InvalidSessionException(e.getMessage(), e);
    }
  }

  /**
   * Set new SessionID
   * @param sessionId New SessionID value
   */
  public void setSessionId(String sessionId) {
    if(sessionId != null)
      sessionDigest = DSSUtils.digest(DigestAlgorithm.SHA512, sessionId.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Verify signature value with public key from certificate
   * @param signature SignatureValue
   * @param certificate Certificate
   * @return Decrypted data from SignatureValue or null if unable to verify
   */
  private byte[] verifySignature(byte[] signature, Certificate certificate) {
    try {
      Cipher rsa = Cipher.getInstance("RSA");
      rsa.init(Cipher.DECRYPT_MODE, certificate);
      rsa.update(signature);
      return rsa.doFinal();
    } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException |
            IllegalBlockSizeException | BadPaddingException e) {
      logger.error(e.getMessage(), e);
      return null;
    }
  }

  /**
   * Temporary token storage
   */
  private static class TempTokenStorage extends TimerTask {

    private final static long TIMER = TimeUnit.HOURS.toMillis(1); // 1 hour memory since last use

    private final Timer timer;
    private SignatureTokenConnection token;

    public TempTokenStorage(SignatureTokenConnection token) {
      this.token = token;
      this.timer = new Timer("TokenTimer",false);
    }

    /**
     * Timer operation
     */
    @Override
    public void run() {
      destroy();
    }

    public synchronized SignatureTokenConnection getToken() {
      return token;
    }

    public void startTimer() {
      timer.schedule(this, TIMER);
    }

    public synchronized void destroy() {
      timer.cancel();
      timer.purge();
      if (token != null) {
        token.close();
      }
      token = null;
    }

  }

  private static class TempSecretStorage extends TimerTask {

    private final Timer timer;
    private final long TIMER;

    private GuardedString secret;

    /**
     * Temporary secret storage with expiration time set by duration in minutes
     * @param secret Secret to temporarily store
     * @param duration Cache duration in minutes
     */
    public TempSecretStorage(GuardedString secret, int duration) {
      this.secret = secret;
      this.TIMER = TimeUnit.MINUTES.toMillis(duration);
      this.timer = new Timer("SecretTimer",false);
    }

    @Override
    public void run() {
      destroy();
    }

    public synchronized GuardedString getSecret() {
      return secret;
    }

    public void startTimer() {
      timer.schedule(this, TIMER);
    }

    public synchronized void destroy() {
      timer.cancel();
      timer.purge();
      if (secret != null)
        secret.dispose();
      secret = null;
    }

  }

}

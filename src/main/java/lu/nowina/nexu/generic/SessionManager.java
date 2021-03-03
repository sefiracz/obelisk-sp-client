package lu.nowina.nexu.generic;

import eu.europa.esig.dss.DSSUtils;
import eu.europa.esig.dss.DigestAlgorithm;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import lu.nowina.nexu.Utils;
import lu.nowina.nexu.api.AbstractProduct;
import org.apache.commons.codec.binary.Base64;
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

public class SessionManager {

  private static final Logger logger = LoggerFactory.getLogger(SessionManager.class.getName());

  private static volatile SessionManager manager;

  private AbstractProduct product = null;
  private TempStorage tokenStorage = null;

  private Certificate trustCert;
  private byte[] sessionDigest;

  private SessionManager() {
    try {
      KeyStore ks = KeyStore.getInstance("JKS");
      ks.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("obsp-trust.jks"),
              Utils.getDecodedValue("UHVkUnhoQ3Bucjd5amJBZQ"));
      trustCert = ks.getCertificate("obsp-client-session");
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      logger.error("Unable to access session truststore and certificate: "+e.getMessage(), e);
    }
  }

  public synchronized static SessionManager getManager() {
    if(manager == null) {
      manager = new SessionManager();
    }
    return manager;
  }

  public SignatureTokenConnection getInitializedTokenForProduct(AbstractProduct product) {
    if(!product.equals(this.product)) {
      destroy(); // close previous token
      return null; // different product
    }
    return tokenStorage.getToken();
  }

  public void setToken(AbstractProduct product, SignatureTokenConnection token) {
    this.product = product;
    if(this.tokenStorage != null) {
      this.tokenStorage.cancel(); // cancel previous timer
    }
    this.tokenStorage = new TempStorage(token);
    this.tokenStorage.startTimer(); // start new timer
  }

  public void destroy() {
    if(this.tokenStorage != null) {
      this.tokenStorage.destroy();
      this.tokenStorage.cancel();
    }
    this.tokenStorage = null;
    this.product = null;
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
      String[] sessionIdValues = sessionId.split("\\+");
      if (sessionIdValues.length != 2) {
        throw new InvalidSessionException("Invalid SessionID format.");
      }

      // check timestamp
      String sessionTimestamp = sessionIdValues[1];
      long now = System.currentTimeMillis();
      long timestamp;
      try {
        timestamp = Long.parseLong(sessionTimestamp);
      } catch (NumberFormatException e) {
        throw new InvalidSessionException("Invalid SessionID timestamp format.");
      }
      if (now - timestamp > 45000*1000) { // 12.5 hours
        throw new InvalidSessionException("Session expired.");
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
  private static class TempStorage extends TimerTask {

    private final static int TIMER = 3600*1000; // 1 hour memory since last use

    private final Timer timer;
    private SignatureTokenConnection token;

    public TempStorage(SignatureTokenConnection token) {
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

  /**
   * Session value holder
   */
  public static class SessionValue {

    private final String sessionId;
    private final String sessionSignature;

    public SessionValue(String sessionId, String sessionSignature) {
      this.sessionId = sessionId;
      this.sessionSignature = sessionSignature;
    }

    public String getSessionId() {
      return sessionId;
    }

    public String getSessionSignature() {
      return sessionSignature;
    }
  }

}

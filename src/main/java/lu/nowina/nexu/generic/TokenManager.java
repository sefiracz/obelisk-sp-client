package lu.nowina.nexu.generic;

import eu.europa.esig.dss.token.SignatureTokenConnection;
import lu.nowina.nexu.api.AbstractProduct;

import java.util.Timer;
import java.util.TimerTask;

public class TokenManager {

  private static volatile TokenManager manager;

  private AbstractProduct product = null;
  private TempStorage tokenStorage = null;
  private String sessionId = null;

  private TokenManager() {}

  public synchronized static TokenManager getManager() {
    if(manager == null) {
      manager = new TokenManager();
    }
    return manager;
  }

  public SignatureTokenConnection getInitializedTokenForProduct(AbstractProduct product) {
    if(this.sessionId != null && (!this.sessionId.equals(product.getSessionId()) || !product.equals(this.product))) {
      destroy(); // close previous product
      return null; // different browser session or product
    }
    return tokenStorage.getToken();
  }

  public void setToken(AbstractProduct product, SignatureTokenConnection token) {
    this.product = product;
    this.sessionId = product.getSessionId();
    if(this.tokenStorage != null) {
      this.tokenStorage.cancel(); // cancel previous timer
    }
    this.tokenStorage = new TempStorage(token);
    this.tokenStorage.startTimer(); // start new timer
  }

  public void destroy() {
    if(this.tokenStorage != null) {
      this.tokenStorage.cancel();
      this.tokenStorage.destroy();
    }
    this.tokenStorage = null;
    this.product = null;
  }

  public void destroy(AbstractProduct product) {
    if(product.equals(this.product)) {
      destroy();
    }
  }

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
      if (token != null) {
        token.close();
      }
      token = null;
    }

  }

}

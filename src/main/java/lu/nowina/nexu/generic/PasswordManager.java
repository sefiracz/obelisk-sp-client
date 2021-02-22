package lu.nowina.nexu.generic;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.generic.ProductPasswordManager
 *
 * Created: 13.01.2021
 * Author: hlavnicka
 */

import com.sun.istack.internal.NotNull;
import lu.nowina.nexu.api.AbstractProduct;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class PasswordManager {

  private static volatile PasswordManager manager;

  private AbstractProduct product = null;
  private PasswordTemp password = null;
  private String sessionId = null;

  private PasswordManager() {}

  public synchronized static PasswordManager getInstance() {
    if(manager == null) {
      manager = new PasswordManager();
    }
    return manager;
  }

  public char[] getPasswordForProduct(@NotNull AbstractProduct product) {
    if(this.sessionId != null && !this.sessionId.equals(product.getSessionId())) {
      return null; // different browser session
    }
    if(!product.equals(this.product)) {
      return null; // different product
    }
    return password.getPassword();
  }

  public void setProductPassword(AbstractProduct product, char[] password) {
    this.product = product;
    this.sessionId = product.getSessionId();
    this.password = new PasswordTemp(password);
    this.password.startTimer();
  }

  public void destroy() {
    if(this.password != null) {
      this.password.destroy();
    }
    this.password = null;
    this.product = null;
  }

  public void destroy(AbstractProduct product) {
    if(product.equals(this.product)) {
      destroy();
    }
  }

  private static class PasswordTemp extends TimerTask {

    private final static int TIMER = 3600*1000; // 1 hour memory since last use

    private final Object lock = new Object();
    private final Timer timer;
    private char[] password;

    public PasswordTemp(char[] password) {
      this.password = p(password);
      this.timer = new Timer(true);
    }

    /**
     * Timer operation
     */
    @Override
    public void run() {
      destroy();
    }

    public char[] getPassword() {
      synchronized (lock) {
        return password != null ? p(password) : null;
      }
    }

    public void startTimer() {
      timer.schedule(this, TIMER);
    }

    public void destroy() {
      synchronized (lock) {
        if(password != null) {
          Arrays.fill(password, (char) 0);
          password = null;
        }
      }
    }

    private char[] p(char[] s) {
      int n = s.length;
      int n2 = n - 1;
      char[] arrc = new char[n];
      int n3 = 5 << 3 ^ 2;
      int n4 = n2;
      int n5 = (3 ^ 5) << 4 ^ (2 << 2 ^ 1);
      while (n4 >= 0) {
        int n6 = n2--;
        arrc[n6] = (char)(s[n6] ^ n5);
        if (n2 < 0) break;
        int n7 = n2--;
        arrc[n7] = (char)(s[n7] ^ n3);
        n4 = n2;
      }
      return arrc;
    }
  }

}
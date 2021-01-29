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

import lu.nowina.nexu.api.AbstractProduct;
import lu.nowina.nexu.api.ConfiguredKeystore;
import lu.nowina.nexu.api.DetectedCard;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class ProductPasswordManager {

  private static volatile ProductPasswordManager manager;

  private AbstractProduct product = null;
  private PasswordTemp password = null;

  private ProductPasswordManager() {}

  public synchronized static ProductPasswordManager getInstance() {
    if(manager == null) {
      manager = new ProductPasswordManager();
    }
    return manager;
  }

  public char[] getPasswordForProduct(AbstractProduct product) {
    if(this.product != null && product instanceof ConfiguredKeystore && this.product instanceof ConfiguredKeystore &&
        ((ConfiguredKeystore) product).getUrl().equals(((ConfiguredKeystore) this.product).getUrl())) {
        return password.getPassword();
    }
    // TODO - nahradit ATR za lepsi identifikator -> (PKCS11 session, cookie session, token info)
    if(this.product != null && product instanceof DetectedCard && this.product instanceof DetectedCard &&
        ((DetectedCard) product).getAtr().equals(((DetectedCard) this.product).getAtr())) {
      return password.getPassword();
    }
    return null;
  }

  public void setProductPassword(AbstractProduct product, char[] password) {
    this.product = product;
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

  private static class PasswordTemp extends TimerTask {

    private final static int TIMER = 3600*1000; // 1 hour memory since last use

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
      return p(password);
    }

    public void startTimer() {
      timer.schedule(this, TIMER);
    }

    public void destroy() {
      Arrays.fill(password, (char)0);
      password = null;
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

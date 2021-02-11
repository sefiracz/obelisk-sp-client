package lu.nowina.nexu.keystore;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.keystore.KeystoreNotFoundException
 *
 * Created: 04.02.2021
 * Author: hlavnicka
 */

/**
 * description
 */
public class KeystoreNotFoundException extends RuntimeException {

  public KeystoreNotFoundException() {
  }

  public KeystoreNotFoundException(String message) {
    super(message);
  }

  public KeystoreNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public KeystoreNotFoundException(Throwable cause) {
    super(cause);
  }

  public KeystoreNotFoundException(String message, Throwable cause, boolean enableSuppression,
                                   boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}

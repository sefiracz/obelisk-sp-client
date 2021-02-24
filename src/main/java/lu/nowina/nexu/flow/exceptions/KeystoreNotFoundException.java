package lu.nowina.nexu.flow.exceptions;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.flow.exceptions.KeystoreNotFoundException
 *
 * Created: 04.02.2021
 * Author: hlavnicka
 */

public class KeystoreNotFoundException extends AbstractTokenRuntimeException {

  private static final String MSG_CODE = "key.selection.keystore.not.found";

  public KeystoreNotFoundException(String message, String... params) {
    super(message, MSG_CODE, params);
  }

  public KeystoreNotFoundException(String message, Throwable cause, String... params) {
    super(message, cause, MSG_CODE, params);
  }

  public KeystoreNotFoundException(Throwable cause, String... params) {
    super(cause, MSG_CODE, params);
  }

}

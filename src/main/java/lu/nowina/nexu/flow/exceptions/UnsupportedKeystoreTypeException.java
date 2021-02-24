package lu.nowina.nexu.flow.exceptions;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.flow.exceptions.UnknownKeystoreTypeException
 *
 * Created: 17.02.2021
 * Author: hlavnicka
 */

public class UnsupportedKeystoreTypeException extends AbstractTokenRuntimeException {

  private static final String MSG_CODE = "key.selection.keystore.unsupported.type";

  public UnsupportedKeystoreTypeException(String message, String... params) {
    super(message, MSG_CODE, params);
  }

  public UnsupportedKeystoreTypeException(String message, Throwable cause, String... params) {
    super(message, cause, MSG_CODE, params);
  }

  public UnsupportedKeystoreTypeException(Throwable cause, String... params) {
    super(cause, MSG_CODE, params);
  }

}

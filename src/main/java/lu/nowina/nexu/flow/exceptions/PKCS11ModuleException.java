package lu.nowina.nexu.flow.exceptions;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.flow.exceptions.PKCS11ModuleException
 *
 * Created: 04.02.2021
 * Author: hlavnicka
 */

public class PKCS11ModuleException extends AbstractTokenRuntimeException {

  private static final String MSG_CODE = "smartcard.pkcs11.error.init";

  public PKCS11ModuleException(String message, String... params) {
    super(message, MSG_CODE, params);
  }

  public PKCS11ModuleException(String message, Throwable cause, String... params) {
    super(message, cause, MSG_CODE, params);
  }

  public PKCS11ModuleException(Throwable cause, String... params) {
    super(cause, MSG_CODE, params);
  }


}

package lu.nowina.nexu.flow.exceptions;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.flow.exceptions.PKCS11TokenException
 *
 * Created: 17.02.2021
 * Author: hlavnicka
 */

public class PKCS11TokenException extends AbstractTokenRuntimeException {

  private static final String MSG_CODE = "smartcard.pkcs11.not.found";

  public PKCS11TokenException(String message, String... params) {
    super(message, MSG_CODE, params);
  }

  public PKCS11TokenException(String message, Throwable cause, String... params) {
    super(message, cause, MSG_CODE, params);
  }

  public PKCS11TokenException(Throwable cause, String... params) {
    super(cause, MSG_CODE, params);
  }


}

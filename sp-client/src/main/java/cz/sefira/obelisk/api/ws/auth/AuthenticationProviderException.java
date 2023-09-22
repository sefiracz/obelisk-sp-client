package cz.sefira.obelisk.api.ws.auth;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.ws.auth.AuthHttpResponseException
 *
 * Created: 22/09/2023
 * Author: hlavnicka
 */

/**
 * Wrapped authentication exception
 */
public class AuthenticationProviderException extends Exception {

  public AuthenticationProviderException(Throwable cause) {
    super(cause);
  }

}

package cz.sefira.obelisk.api.ws.auth;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.ws.auth.AuthExpirationException
 *
 * Created: 07.06.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.ws.GenericApiException;

/**
 * Authentication credentials expiration (unable to obtain new)
 */
public class AuthExpirationException extends GenericApiException {

 public AuthExpirationException(String message) {
  super(message, "dispatcher.auth.expired.error");
 }

}

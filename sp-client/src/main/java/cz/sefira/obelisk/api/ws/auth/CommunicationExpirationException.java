package cz.sefira.obelisk.api.ws.auth;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.ws.auth.AuthExpiredException
 *
 * Created: 23.03.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.ws.GenericApiException;

/**
 * SP-API communication expiration
 */
public class CommunicationExpirationException extends GenericApiException {

  public CommunicationExpirationException(String message) {
    this(message, null);
  }

  public CommunicationExpirationException(String message, Throwable t) {
    super(message, "dispatcher.expired.error", t);
  }
}

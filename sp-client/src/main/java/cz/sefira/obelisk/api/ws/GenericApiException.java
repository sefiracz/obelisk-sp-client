package cz.sefira.obelisk.api.ws;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.ws.GenericApiException
 *
 * Created: 26.04.2023
 * Author: hlavnicka
 */

/**
 * description
 */
public class GenericApiException extends RuntimeException {

  private final String messageProperty;

  public GenericApiException(String message, String messageProperty) {
    super(message);
    this.messageProperty = messageProperty;
  }

  public GenericApiException(String message, String messageProperty, Throwable t) {
    super(message, t);
    this.messageProperty = messageProperty;
  }

  public String getMessageProperty() {
    return messageProperty;
  }
}

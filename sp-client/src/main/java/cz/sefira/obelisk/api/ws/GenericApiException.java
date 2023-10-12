/**
 * © SEFIRA spol. s r.o., 2020-2023
 * <p>
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 * <p>
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 * <p>
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
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

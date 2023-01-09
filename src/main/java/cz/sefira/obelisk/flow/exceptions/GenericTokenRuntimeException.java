/**
 * © SEFIRA spol. s r.o., 2020-2023
 *
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package cz.sefira.obelisk.flow.exceptions;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.flow.exceptions.UnsupportedAlgorithmException
 *
 * Created: 05.01.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.view.DialogMessage;

/**
 * Generic token runtime exception
 */
public class GenericTokenRuntimeException extends AbstractTokenRuntimeException {

  public GenericTokenRuntimeException(String message, String messageCode,
                                      DialogMessage.Level level, String... params) {
    super(message, messageCode, level, params);
  }

  public GenericTokenRuntimeException(String message, Throwable cause, String messageCode, DialogMessage.Level level,
                                      String... params) {
    super(message, cause, messageCode, level, params);
  }

  public GenericTokenRuntimeException(Throwable cause, String messageCode, DialogMessage.Level level,
                                      String... params) {
    super(cause, messageCode, level, params);
  }
}

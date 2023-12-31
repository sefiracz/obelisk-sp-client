/**
 * © SEFIRA spol. s r.o., 2020-2021
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
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.flow.exceptions.UnknownKeystoreTypeException
 *
 * Created: 17.02.2021
 * Author: hlavnicka
 */

import cz.sefira.obelisk.view.DialogMessage;

public class UnsupportedKeystoreTypeException extends AbstractTokenRuntimeException {

  private static final String MSG_CODE = "key.selection.keystore.unsupported.type";
  private static final DialogMessage.Level level = DialogMessage.Level.ERROR;

  public UnsupportedKeystoreTypeException(String message, String... params) {
    super(message, MSG_CODE, level, params);
  }

  public UnsupportedKeystoreTypeException(String message, Throwable cause, String... params) {
    super(message, cause, MSG_CODE, level, params);
  }

  public UnsupportedKeystoreTypeException(Throwable cause, String... params) {
    super(cause, MSG_CODE, level, params);
  }

}

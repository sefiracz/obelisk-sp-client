/**
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Licensed under EUPL Version 1.1 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package lu.nowina.nexu.flow.exceptions;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.flow.exceptions.KeystoreNotFoundException
 *
 * Created: 04.02.2021
 * Author: hlavnicka
 */

import lu.nowina.nexu.view.DialogMessage;

public class KeystoreNotFoundException extends AbstractTokenRuntimeException {

  private static final String MSG_CODE = "key.selection.keystore.not.found";
  private static final DialogMessage.Level level = DialogMessage.Level.ERROR;

  public KeystoreNotFoundException(String message, String... params) {
    super(message, MSG_CODE, level, params);
  }

  public KeystoreNotFoundException(String message, Throwable cause, String... params) {
    super(message, cause, MSG_CODE, level, params);
  }

  public KeystoreNotFoundException(Throwable cause, String... params) {
    super(cause, MSG_CODE, level, params);
  }

}

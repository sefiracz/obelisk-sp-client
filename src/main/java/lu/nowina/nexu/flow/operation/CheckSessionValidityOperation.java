/**
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package lu.nowina.nexu.flow.operation;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.flow.operation.CheckSessionValidityOperation
 *
 * Created: 28.02.2021
 * Author: hlavnicka
 */

import lu.nowina.nexu.api.SessionValue;
import lu.nowina.nexu.api.flow.OperationResult;
import lu.nowina.nexu.generic.InvalidSessionException;
import lu.nowina.nexu.generic.SessionManager;

public class CheckSessionValidityOperation extends AbstractCompositeOperation<Boolean> {

  private SessionValue sessionValue;

  @Override
  public void setParams(Object... params) {
    this.sessionValue = (SessionValue) params[0];
  }

  @Override
  public OperationResult<Boolean> perform() {
    try {
      boolean sessionValid = SessionManager.getManager()
              .checkSession(sessionValue.getSessionId(), sessionValue.getSessionSignature());
      if(!sessionValid) {
        SessionManager.getManager().destroy();
        SessionManager.getManager().setSessionId(sessionValue.getSessionId());
      }
    } catch (InvalidSessionException e) {
      // I don't talk to strangers and dummies, gimme proof
      return new OperationResult<>(CoreOperationStatus.INVALID_SESSION, e.getMessage());
    }
    return new OperationResult<>(Boolean.TRUE);
  }
}

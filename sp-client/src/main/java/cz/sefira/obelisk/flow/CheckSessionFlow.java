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
package cz.sefira.obelisk.flow;

import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.api.flow.BasicOperationStatus;
import cz.sefira.obelisk.api.flow.OperationResult;
import cz.sefira.obelisk.api.ws.model.SessionValue;
import cz.sefira.obelisk.flow.operation.CheckSessionValidityOperation;
import cz.sefira.obelisk.view.core.UIDisplay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckSessionFlow extends AbstractCoreFlow<SessionValue, Boolean> {

  private static final Logger logger = LoggerFactory.getLogger(CheckSessionFlow.class.getName());

  public CheckSessionFlow(UIDisplay display, PlatformAPI api) {
    super(display, api);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Execution<Boolean> process(PlatformAPI api, SessionValue sessionValue) throws Exception {
    try {
      // check session validity
      final OperationResult<Boolean> sessionValidityResult = this.getOperationFactory().getOperation(CheckSessionValidityOperation.class, sessionValue).perform();
      if (sessionValidityResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
        return new Execution<>(true);
      } else {
        return this.handleErrorOperationResult(sessionValidityResult); // INVALID SESSION
      }
    } catch (Exception e) {
      logger.error("Flow error", e);
      throw handleException(e);
    }
  }


}

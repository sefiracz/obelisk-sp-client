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

import cz.sefira.obelisk.api.Execution;
import cz.sefira.obelisk.api.NexuAPI;
import cz.sefira.obelisk.api.SmartcardListRequest;
import cz.sefira.obelisk.api.SmartcardListResponse;
import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.api.flow.BasicOperationStatus;
import cz.sefira.obelisk.api.flow.OperationResult;
import cz.sefira.obelisk.flow.operation.CheckSessionValidityOperation;
import cz.sefira.obelisk.view.core.UIDisplay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmartcardsInfoFlow extends AbstractCoreFlow<SmartcardListRequest, SmartcardListResponse> {

  private static final Logger logger = LoggerFactory.getLogger(SmartcardsInfoFlow.class.getName());

  public SmartcardsInfoFlow(UIDisplay display, NexuAPI api) {
    super(display, api);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Execution<SmartcardListResponse> process(NexuAPI api, SmartcardListRequest req) throws Exception {
    try {
      // check session validity
      final OperationResult<Boolean> sessionValidityResult = this.getOperationFactory()
              .getOperation(CheckSessionValidityOperation.class, req.getSessionValue()).perform();
      if (sessionValidityResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
        api.supportedSmartcardInfos(req.getSmartcardInfos(), req.getDigest()); // store a list of known supported smartcards
        return new Execution<>(new SmartcardListResponse());
      } else {
        return this.handleErrorOperationResult(sessionValidityResult); // INVALID SESSION
      }
    } catch (Exception e) {
      logger.error("Flow error", e);
      throw handleException(e);
    }
  }


}

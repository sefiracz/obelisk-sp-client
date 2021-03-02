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
package lu.nowina.nexu.flow;

import lu.nowina.nexu.api.*;
import lu.nowina.nexu.api.flow.BasicOperationStatus;
import lu.nowina.nexu.api.flow.OperationResult;
import lu.nowina.nexu.flow.operation.CheckSessionValidityOperation;
import lu.nowina.nexu.view.core.UIDisplay;
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

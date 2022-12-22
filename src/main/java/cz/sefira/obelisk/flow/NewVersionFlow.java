package cz.sefira.obelisk.flow;

/*
 * Copyright 2022 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.flow.NewVersionFlow
 *
 * Created: 04.11.2022
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.api.flow.BasicOperationStatus;
import cz.sefira.obelisk.api.flow.OperationResult;
import cz.sefira.obelisk.flow.operation.CheckSessionValidityOperation;
import cz.sefira.obelisk.view.core.UIDisplay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * New version notification info flow
 */
public class NewVersionFlow extends AbstractCoreFlow<NewVersionRequest, NewVersionResponse> {

  static final Logger logger = LoggerFactory.getLogger(GetTokenFlow.class);

  public NewVersionFlow(final UIDisplay display, final NexuAPI api) {
    super(display, api);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Execution<NewVersionResponse> process(final NexuAPI api, final NewVersionRequest req) throws Exception {
    try {
      // check session validity
      final OperationResult<Boolean> sessionValidityResult = this.getOperationFactory()
          .getOperation(CheckSessionValidityOperation.class, req.getSessionValue()).perform();
      if (sessionValidityResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
        // TODO - process new version info
        return new Execution<>(new NewVersionResponse());
      } else {
        return this.handleErrorOperationResult(sessionValidityResult); // INVALID SESSION
      }
    } catch (Exception e) {
      logger.error("Flow error", e);
      throw handleException(e);
    }
  }
}

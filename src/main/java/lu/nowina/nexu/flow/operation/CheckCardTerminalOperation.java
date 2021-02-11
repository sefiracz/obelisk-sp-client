package lu.nowina.nexu.flow.operation;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.flow.operation.CheckCardTerminalOperation
 *
 * Created: 03.02.2021
 * Author: hlavnicka
 */

import lu.nowina.nexu.api.DetectedCard;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.flow.OperationResult;
import lu.nowina.nexu.view.core.UIOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks the detected card and set its terminal information accordingly to its detected place
 */
public class CheckCardTerminalOperation extends AbstractCompositeOperation<DetectedCard> {

  private static final Logger logger = LoggerFactory.getLogger(CheckCardTerminalOperation.class.getName());

  private NexuAPI api;
  private DetectedCard selectedCard;

  @Override
  public void setParams(Object... params) {
    try {
      this.api = (NexuAPI) params[0];
      this.selectedCard = (DetectedCard) params[1];
    } catch(final ArrayIndexOutOfBoundsException | ClassCastException e) {
      throw new IllegalArgumentException("Expected parameters: NexuAPI, DetectedCard");
    }
  }

  @Override
  public OperationResult<DetectedCard> perform() {
    logger.info("Check smartcard terminal slot");
    if(selectedCard != null) {
      selectedCard = api.detectCard(selectedCard);
    }
    if(selectedCard == null) {
      // card not found connected in any of the terminals
      this.operationFactory.getOperation(UIOperation.class, "/fxml/message.fxml", new Object[] {
          "key.selection.pkcs11.not.found", api.getAppConfig().getApplicationName(), 370, 150
      }).perform();
      return new OperationResult<>(CoreOperationStatus.NO_PRODUCT_FOUND);
    }
    return new OperationResult<>(selectedCard);
  }
}

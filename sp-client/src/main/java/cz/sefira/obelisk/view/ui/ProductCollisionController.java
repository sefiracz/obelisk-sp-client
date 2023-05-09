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
package cz.sefira.obelisk.view.ui;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.view.ui.ProductCollisionController
 *
 * Created: 13.01.2021
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AbstractProduct;
import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.token.pkcs11.DetectedCard;
import cz.sefira.obelisk.api.flow.OperationFactory;
import cz.sefira.obelisk.flow.StageHelper;
import cz.sefira.obelisk.view.StandaloneDialog;
import cz.sefira.obelisk.view.core.AbstractUIOperationController;
import cz.sefira.obelisk.view.core.StageState;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import cz.sefira.obelisk.api.PlatformAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * User choice colliding products controller
 */
public class ProductCollisionController extends AbstractUIOperationController<AbstractProduct> implements PropertyChangeListener, Initializable {

  private static final Logger logger = LoggerFactory.getLogger(ProductCollisionController.class.getName());

  @FXML
  private StackPane productsWindow;

  @FXML
  private BorderPane borderPane;

  @FXML
  private Label message;

  @FXML
  private Pane productsContainer;

  @FXML
  private Button dashButton;

  @FXML
  private Button select;

  @FXML
  private Button refresh;

  @FXML
  private Button manage;

  @FXML
  private Button cancel;

  private ToggleGroup product;
  private PlatformAPI api;

  private List<AbstractProduct> products;
  private VBox overlay;
  private VBox progressIndicator;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    overlay = new VBox();
    overlay.getStyleClass().add("overlay");
    ProgressIndicator indicator = new ProgressIndicator();
    indicator.setPrefSize(150, 150);
    indicator.setMinHeight(150);
    indicator.setMinWidth(150);
    progressIndicator = new VBox(indicator);
    progressIndicator.setAlignment(Pos.CENTER);

    select.setOnAction(e -> signalEnd(getSelectedProduct()));
    cancel.setOnAction(e -> signalUserCancel());

    manage.setOnAction(e ->
        StandaloneDialog.createDialogFromFXML("/fxml/manage-keystores.fxml", null, StageState.BLOCKING, api, products)
    );
    product = new ToggleGroup();
    select.disableProperty().bind(product.selectedToggleProperty().isNull());

    refresh.setOnAction(e -> {
      progressIndicatorVisible(true);
      product.getToggles().clear();
      asyncTask(() -> {
        for (final AbstractProduct p : products) {
          if (p instanceof DetectedCard) {
            api.detectCardTerminal((DetectedCard) p);
          }
        }
      }, true);

    });

  }

  private AbstractProduct getSelectedProduct() {
    return (AbstractProduct) product.getSelectedToggle().getUserData();
  }

  @Override
  public final void init(Object... params) {
    this.api = (PlatformAPI) params[0];
    StageHelper.getInstance().setTitle(AppConfig.get().getApplicationName(), "product.selection.title");
    products = (List<AbstractProduct>) params[1];

    // hook up live card detection support
    api.cardDetection(this, true);

    progressIndicatorVisible(true);
    asyncTask(() -> {
      for (final AbstractProduct p : products) {
        if (p instanceof DetectedCard) {
          api.detectCardTerminal((DetectedCard) p);
        }
      }
    }, true);

    Platform.runLater(() -> {
      message.setText(MessageFormat.format(ResourceBundle.getBundle("bundles/nexu")
                      .getString("product.collision.selection.header"), new Object[]{}));

    });

    // asynchronous window update
    asyncUpdate(() -> {
      message.setText(MessageFormat.format(ResourceBundle.getBundle("bundles/nexu")
              .getString("product.collision.selection.header"), new Object[]{}));

      final List<RadioButton> radioButtons = new ArrayList<>(products.size());

      for (final AbstractProduct p : products) {
        final RadioButton button = new RadioButton(api.getLabel(p));
        // disable disconnected cards
        if (p instanceof DetectedCard && ((DetectedCard) p).getTerminal() == null) {
          button.setDisable(true);
        }
        button.setToggleGroup(product);
        button.setUserData(p);
        button.setMnemonicParsing(false);
        radioButtons.add(button);
      }

      productsContainer.getChildren().clear();
      productsContainer.getChildren().addAll(radioButtons);

      progressIndicatorVisible(false);
    });

    dashButton.setOnAction(e -> StandaloneDialog.createDialogFromFXML("/fxml/main-window.fxml", null, StageState.NONBLOCKING, api));
    setLogoBackground(productsContainer);
  }

  // TODO - rewrite to generic?
  private void progressIndicatorVisible(boolean visible) {
    if(visible) {
      // add progress indicator
      borderPane.setEffect(new GaussianBlur());
      productsWindow.getChildren().add(overlay);
      productsWindow.getChildren().add(progressIndicator);
    } else {
      // remove progress indicator
      borderPane.setEffect(null);
      productsWindow.getChildren().removeIf(node -> node instanceof VBox);
    }
  }

  @Override
  public synchronized void propertyChange(PropertyChangeEvent evt) {
    asyncTask(() -> {
      String propertyChange = evt.getPropertyName();
      DetectedCard changedCard = (DetectedCard) evt.getNewValue();
//      if ("remove".equals(propertyChange)) {
//        try {
//          int removedCardIdx = cards.indexOf(changedCard);
//          if (removedCardIdx == -1) {
//            throw new IllegalStateException("Card not found, user probably already refreshed");
//          }
//          DetectedCard card = cards.get(removedCardIdx);
//          if (card.equals(changedCard)) {
//            card.setTerminal(null);
//            card.setTerminalLabel(null);
//          } else {
//            throw new IllegalStateException("Wrong index, card not found, user probably already refreshed");
//          }
//        } catch (Exception e) {
//          logger.error(e.getMessage(), e);
//          // something happened (user might have already refreshed) - just try to remove it
//          cards.remove(changedCard);
//        }
//      } else if ("add".equals(propertyChange)) {
//        cards.add(changedCard);
//      }
    }, true);
  }

  @Override
  public void close() {
    api.cardDetection(this, false);
  }
}

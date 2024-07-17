/**
 * © SEFIRA spol. s r.o., 2020-2021
 * <p>
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 * <p>
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 * <p>
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

import cz.sefira.obelisk.CardDetector;
import cz.sefira.obelisk.api.AbstractProduct;
import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.Product;
import cz.sefira.obelisk.token.pkcs11.DetectedCard;
import cz.sefira.obelisk.api.flow.OperationFactory;
import cz.sefira.obelisk.flow.StageHelper;
import cz.sefira.obelisk.util.ResourceUtils;
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
  private VBox overlay;

  @FXML
  private VBox progressIndicator;

  @FXML
  private BorderPane borderPane;

  @FXML
  private Label message;

  @FXML
  private Pane productsContainer;

//  @FXML
//  private Button dashButton;

  @FXML
  private Button settings;

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
  private List<DetectedCard> cards;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    select.setOnAction(e -> signalEnd(getSelectedProduct()));
    cancel.setOnAction(e -> signalUserCancel());

    manage.setOnAction(e ->
        StandaloneDialog.createDialogFromFXML("/fxml/manage-keystores.fxml", getDisplay().getStage(true), StageState.NONBLOCKING, api, products)
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
    AbstractProduct p = (AbstractProduct) product.getSelectedToggle().getUserData();
    if (p != null) {
      logger.info("Product selected: " + p.getSimpleLabel());
      if (p instanceof DetectedCard card) {
        logger.info("Selected card ATR: " + card.getAtr());
      }
    }
    return p;
  }

  private List<DetectedCard> getCards(List<AbstractProduct> products) {
    List<DetectedCard> cards = new ArrayList<>();
    for (AbstractProduct abstractProduct : products) {
      if (abstractProduct instanceof DetectedCard) {
        cards.add((DetectedCard) abstractProduct);
      }
    }
    return cards;
  }

  @Override
  public final void init(Object... params) {
    this.api = (PlatformAPI) params[0];
    StageHelper.getInstance().setTitle(AppConfig.get().getApplicationName(), "product.selection.title");
    products = (List<AbstractProduct>) params[1];

    // hook up live card detection support
    if (!getCards(products).isEmpty()) {
      api.cardDetection(this, true);
    }

    progressIndicatorVisible(true);
    asyncTask(() -> {
      for (final AbstractProduct p : products) {
        if (p instanceof DetectedCard) {
          api.detectCardTerminal((DetectedCard) p);
        }
      }
    }, true);

    Platform.runLater(() -> {
      message.setText(MessageFormat.format(ResourceUtils.getBundle()
          .getString("product.collision.selection.header"), new Object[]{}));

    });

    // asynchronous window update
    asyncUpdate(() -> {
      message.setText(MessageFormat.format(ResourceUtils.getBundle()
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

    settings.setOnAction(e -> StandaloneDialog.createDialogFromFXML("/fxml/main-window.fxml", null, StageState.BLOCKING, api));
    setLogoBackground(productsContainer);
  }

  private void progressIndicatorVisible(boolean visible) {
    if (visible) {
      // add progress indicator
      borderPane.setEffect(new GaussianBlur());
      overlay.setVisible(true);
      progressIndicator.setVisible(true);
    } else {
      // remove progress indicator
      borderPane.setEffect(null);
      overlay.setVisible(false);
      progressIndicator.setVisible(false);
    }
  }

  @Override
  public synchronized void propertyChange(PropertyChangeEvent evt) {
    Platform.runLater(() -> {
      progressIndicatorVisible(true);
      asyncTask(() -> {
        String propertyName = evt.getPropertyName();
        DetectedCard changedCard = (DetectedCard) evt.getNewValue();
        List<DetectedCard> cards = getCards(products);
        for (DetectedCard card : cards) {
          if (card.equals(changedCard)) {
            if (CardDetector.ADD_CARD.equals(propertyName)) {
              card.setTerminal(changedCard.getTerminal());
              card.setTerminalLabel(changedCard.getTerminalLabel());
            } else if (CardDetector.REMOVE_CARD.equals(propertyName)) {
              card.setTerminal(null);
              card.setTerminalLabel(null);
            }
          }
        }
      }, true);
    });
  }

  @Override
  public void close() {
    api.cardDetection(this, false);
  }
}

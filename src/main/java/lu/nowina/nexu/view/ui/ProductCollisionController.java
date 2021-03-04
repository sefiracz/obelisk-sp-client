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
package lu.nowina.nexu.view.ui;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.view.ui.ProductCollisionController
 *
 * Created: 13.01.2021
 * Author: hlavnicka
 */

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
import lu.nowina.nexu.api.AbstractProduct;
import lu.nowina.nexu.api.DetectedCard;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.flow.OperationFactory;
import lu.nowina.nexu.flow.StageHelper;
import lu.nowina.nexu.view.core.AbstractUIOperationController;
import lu.nowina.nexu.view.core.NonBlockingUIOperation;
import lu.nowina.nexu.view.core.UIOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * User choice colliding products controller
 */
public class ProductCollisionController extends AbstractUIOperationController<AbstractProduct> implements Initializable {

  private static final Logger logger = LoggerFactory.getLogger(UnknownCertificateMessageController.class.getName());

  @FXML
  private StackPane productsWindow;

  @FXML
  private BorderPane borderPane;

  @FXML
  private Label message;

  @FXML
  private Pane productsContainer;

  @FXML
  private Button select;

  @FXML
  private Button refresh;

  @FXML
  private Button manage;

  @FXML
  private Button cancel;

  private ToggleGroup product;
  private OperationFactory operationFactory;
  private NexuAPI api;

  private List<AbstractProduct> products;
  private VBox overlay;
  private VBox progressIndicator;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    overlay = new VBox();
    overlay.getStyleClass().add("overlay");
    ProgressIndicator indicator = new ProgressIndicator();
    indicator.setPrefSize(150,150);
    progressIndicator = new VBox(indicator);
    progressIndicator.setAlignment(Pos.CENTER);

    select.setOnAction(e -> signalEnd(getSelectedProduct()));
    cancel.setOnAction(e -> signalUserCancel());

    manage.setOnAction(e -> UIOperation.getFutureOperationInvocation(NonBlockingUIOperation.class,
            "/fxml/manage-keystores.fxml", api, products).call(operationFactory));
    product = new ToggleGroup();
    select.disableProperty().bind(product.selectedToggleProperty().isNull());

    refresh.setOnAction(e -> {
      progressIndicatorVisible(true);
      product.getToggles().clear();
      asyncWorkload(() -> {
        for (final AbstractProduct p : products) {
          if (p instanceof DetectedCard) {
            api.detectCardTerminal((DetectedCard) p);
          }
        }
      }, true);

    });

    // asynchronous window update
    asyncUpdate(() -> {
      message.setText(MessageFormat.format(ResourceBundle.getBundle("bundles/nexu")
                      .getString("product.collision.selection.header"), new Object[]{}));

      final List<RadioButton> radioButtons = new ArrayList<>(products.size());

      for (final AbstractProduct p : products) {
        final RadioButton button = new RadioButton(api.getLabel(p));
        button.setToggleGroup(product);
        button.setUserData(p);
        button.setMnemonicParsing(false);
        radioButtons.add(button);
      }

      productsContainer.getChildren().clear();
      productsContainer.getChildren().addAll(radioButtons);

      progressIndicatorVisible(false);
    });
  }

  private AbstractProduct getSelectedProduct() {
    return (AbstractProduct) product.getSelectedToggle().getUserData();
  }

  @Override
  public final void init(Object... params) {
    this.api = (NexuAPI) params[0];
    this.operationFactory = (OperationFactory) params[1];
    StageHelper.getInstance().setTitle(api.getAppConfig().getApplicationName(), "product.selection.title");
    products = (List<AbstractProduct>) params[2];

    progressIndicatorVisible(true);
    asyncWorkload(() -> {
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
}

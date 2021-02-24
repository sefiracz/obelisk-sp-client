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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import lu.nowina.nexu.api.AbstractProduct;
import lu.nowina.nexu.api.DetectedCard;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.flow.OperationFactory;
import lu.nowina.nexu.flow.StageHelper;
import lu.nowina.nexu.flow.operation.CoreOperationStatus;
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
  private BorderPane productsWindow;

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

  private NexuAPI api;

  private List<AbstractProduct> products;

  private OperationFactory operationFactory;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    select.setOnAction(e -> signalEnd(getSelectedProduct()));
    cancel.setOnAction(e -> signalUserCancel());
    refresh.setOnAction(e -> signalEndWithStatus(CoreOperationStatus.BACK));
    manage.setOnAction(e -> UIOperation.getFutureOperationInvocation(NonBlockingUIOperation.class,
            "/fxml/manage-keystores.fxml", api, products).call(operationFactory));
    product = new ToggleGroup();
    select.disableProperty().bind(product.selectedToggleProperty().isNull());
  }

  private AbstractProduct getSelectedProduct() {
    return (AbstractProduct) product.getSelectedToggle().getUserData();
  }

  @Override
  public final void init(Object... params) {
    this.api = (NexuAPI) params[0];
    this.operationFactory = (OperationFactory) params[1];
    StageHelper.getInstance().setTitle(api.getAppConfig().getApplicationName(), "product.selection.title");

    Platform.runLater(() -> {
      message.setText(MessageFormat
          .format(ResourceBundle.getBundle("bundles/nexu").getString("product.collision.selection.header"), new Object[]{}));

      products = (List<AbstractProduct>) params[2];

      final List<RadioButton> radioButtons = new ArrayList<>(products.size());

      int height = 0;
      for (final AbstractProduct p : products) {
        if(p instanceof DetectedCard) {
          api.detectCardTerminal((DetectedCard) p);
        }
        final RadioButton button = new RadioButton(api.getLabel(p));
        button.setToggleGroup(product);
        button.setUserData(p);
        button.setMnemonicParsing(false);
        radioButtons.add(button);
        height+=25;
      }
      height = Math.min(height, 300);
      productsWindow.setPrefHeight(productsWindow.getPrefHeight()+height);

      productsContainer.getChildren().addAll(radioButtons);
    });
  }
}

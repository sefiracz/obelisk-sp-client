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

import eu.europa.esig.dss.x509.CertificateToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import lu.nowina.nexu.api.AbstractProduct;
import lu.nowina.nexu.api.DetectedCard;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.Product;
import lu.nowina.nexu.flow.StageHelper;
import lu.nowina.nexu.view.core.AbstractUIOperationController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * description
 */
public class ProductCollisionController extends AbstractUIOperationController<AbstractProduct> implements Initializable {

  private static final Logger logger = LoggerFactory.getLogger(UnknownCertificateMessageController.class.getName());

  @FXML
  private Label message;

  @FXML
  private Pane productsContainer;

  @FXML
  private Button select;

  @FXML
  private Button cancel;

  private ToggleGroup product;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    select.setOnAction(e -> signalEnd(getSelectedProduct()));
    cancel.setOnAction(e -> signalUserCancel());

    product = new ToggleGroup();
    select.disableProperty().bind(product.selectedToggleProperty().isNull());
  }

  private AbstractProduct getSelectedProduct() {
    return (AbstractProduct) product.getSelectedToggle().getUserData();
  }

  @Override
  public final void init(Object... params) {
    final NexuAPI api = (NexuAPI) params[0];
    StageHelper.getInstance().setTitle(api.getAppConfig().getApplicationName(), "product.selection.title");

    Platform.runLater(() -> {
      message.setText(MessageFormat
          .format(ResourceBundle.getBundle("bundles/nexu").getString("product.collision.selection.header"), new Object[]{}));

      @SuppressWarnings("unchecked")
      final List<AbstractProduct> products = (List<AbstractProduct>) params[1];

      final List<RadioButton> radioButtons = new ArrayList<>(products.size());

      for (final AbstractProduct p : products) {
        final RadioButton button = new RadioButton(api.getLabel(p));
        button.setToggleGroup(product);
        button.setUserData(p);
        button.setMnemonicParsing(false);
        radioButtons.add(button);
      }

      productsContainer.getChildren().addAll(radioButtons);
    });
  }
}

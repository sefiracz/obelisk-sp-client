/**
 * © Nowina Solutions, 2015-2016
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package lu.nowina.nexu.view.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lu.nowina.nexu.api.DetectedCard;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.Product;
import lu.nowina.nexu.flow.StageHelper;
import lu.nowina.nexu.view.core.AbstractUIOperationController;

import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ProductSelectionController extends AbstractUIOperationController<Product> implements Initializable {

  @FXML
  private StackPane productsWindow;

  @FXML
  private BorderPane borderPane;

  @FXML
  private Label message;

  @FXML
  private VBox productsContainer;

  @FXML
  private Button select;

  @FXML
  private Button cancel;

  @FXML
  private Button refresh;

  private ToggleGroup product;

  private String title;
  private NexuAPI api;

  private VBox overlay;
  private VBox progressIndicator;
  private List<DetectedCard> cards;
  private List<Product> products;

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

    product = new ToggleGroup();
    select.disableProperty().bind(product.selectedToggleProperty().isNull());

    refresh.setOnAction(e -> {
      // add progress indicator
      progressIndicatorVisible(true);
      product.getToggles().clear();
      // asynchronous heavy workload
      asyncWorkload(() -> cards = api.detectCards(), true);
    });
  }

  private Product getSelectedProduct() {
    return (Product) product.getSelectedToggle().getUserData();
  }

  @Override
  @SuppressWarnings("unchecked")
  public final void init(Object... params) {
    title = (String) params[0];
    api = (NexuAPI) params[1];
    StageHelper.getInstance().setTitle(api.getAppConfig().getApplicationName(),
            "product.selection.title");

    // add progress indicator
    progressIndicatorVisible(true);
    // asynchronous heavy workload
    asyncWorkload(() -> cards = api.detectCards(), true);

    // show initial content before load
    Platform.runLater(() -> {
      message.setText(MessageFormat
              .format(ResourceBundle.getBundle("bundles/nexu").getString("product.selection.header"), title));
      products = api.detectProducts();

      final List<RadioButton> radioButtons = new ArrayList<>(products.size());

      for (final Product p : products) {
        final RadioButton button = new RadioButton(api.getLabel(p));
        button.setToggleGroup(product);
        button.setUserData(p);
        button.setMnemonicParsing(false);
        radioButtons.add(button);
      }

      productsContainer.getChildren().addAll(radioButtons);
    });

    // asynchronous window content update
    asyncUpdate(() -> {
      message.setText(MessageFormat
              .format(ResourceBundle.getBundle("bundles/nexu").getString("product.selection.header"), title));

      final List<RadioButton> radioButtons = new ArrayList<>(cards.size() + products.size());

      for (final DetectedCard card : cards) {
        final RadioButton button = new RadioButton(api.getLabel(card));
        button.setToggleGroup(product);
        button.setUserData(card);
        button.setMnemonicParsing(false);
        radioButtons.add(button);
      }
      for (final Product p : products) {
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

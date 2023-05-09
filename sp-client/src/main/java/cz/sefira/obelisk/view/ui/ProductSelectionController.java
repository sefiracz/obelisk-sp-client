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
package cz.sefira.obelisk.view.ui;

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.flow.StageHelper;
import cz.sefira.obelisk.token.macos.MacOSKeychain;
import cz.sefira.obelisk.view.StandaloneDialog;
import cz.sefira.obelisk.view.core.NonBlockingUIOperation;
import cz.sefira.obelisk.view.core.StageState;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import cz.sefira.obelisk.token.pkcs11.DetectedCard;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.Product;
import cz.sefira.obelisk.api.flow.OperationFactory;
import cz.sefira.obelisk.view.core.AbstractUIOperationController;
import cz.sefira.obelisk.token.windows.WindowsKeystore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ScheduledExecutorService;

public class ProductSelectionController extends AbstractUIOperationController<Product> implements PropertyChangeListener, Initializable {

  private static final Logger logger = LoggerFactory.getLogger(ProductSelectionController.class.getName());

  @FXML
  private StackPane productsWindow;

  @FXML
  private BorderPane borderPane;

  @FXML
  private ScrollPane scrollPane;

  @FXML
  private Label message;

  @FXML
  private VBox productsContainer;

  @FXML
  private Button dashButton;

  @FXML
  private Button select;

  @FXML
  private Button cancel;

  @FXML
  private Button refresh;

  private ToggleGroup product;

  private String appName;
  private PlatformAPI api;

  private VBox overlay;
  private VBox progressIndicator;
  private List<DetectedCard> cards;
  private List<Product> products;

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

    product = new ToggleGroup();
    select.disableProperty().bind(product.selectedToggleProperty().isNull());

    refresh.setOnAction(e -> {
      // add progress indicator
      progressIndicatorVisible(true);
      product.getToggles().clear();
      // asynchronous heavy workload
      asyncTask(() -> cards = api.detectCards(false), true);
    });
  }

  private Product getSelectedProduct() {
    return (Product) product.getSelectedToggle().getUserData();
  }

  @Override
  @SuppressWarnings("unchecked")
  public final void init(Object... params) {
    api = (PlatformAPI) params[0];
    appName = AppConfig.get().getApplicationName();
    StageHelper.getInstance().setTitle(appName, "product.selection.title");

    // hook up live card detection support
    api.cardDetection(this, true);

    // add progress indicator
    progressIndicatorVisible(true);
    // asynchronous heavy workload
    asyncTask(() -> cards = api.detectCards(false), true);

    // show initial content before load
    Platform.runLater(() -> {
      message.setText(MessageFormat
          .format(ResourceBundle.getBundle("bundles/nexu").getString("product.selection.header"), appName));
      products = api.detectProducts();

      final List<RadioButton> radioButtons = new ArrayList<>(products.size());

      for (final Product p : products) {
        String label = api.getLabel(p);
        if(p instanceof WindowsKeystore || p instanceof MacOSKeychain) {
          label = p.getSimpleLabel();
        }
        final RadioButton button = new RadioButton(label);
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
          .format(ResourceBundle.getBundle("bundles/nexu").getString("product.selection.header"), appName));

      final List<RadioButton> radioButtons = new ArrayList<>(cards.size() + products.size());

      for (final DetectedCard card : cards) {
        final RadioButton button = new RadioButton(api.getLabel(card));
        button.setToggleGroup(product);
        button.setUserData(card);
        button.setMnemonicParsing(false);
        // disable disconnected cards
        if (card.getTerminal() == null) {
          button.setDisable(true);
          button.setText(api.getLabel(card));
        }
        radioButtons.add(button);
      }
      for (final Product p : products) {
        String label = api.getLabel(p);
        if(p instanceof WindowsKeystore || p instanceof MacOSKeychain) {
          label = p.getSimpleLabel();
        }
        final RadioButton button = new RadioButton(label);
        button.setToggleGroup(product);
        button.setUserData(p);
        button.setMnemonicParsing(false);
        radioButtons.add(button);
      }

      productsContainer.getChildren().clear();
      productsContainer.getChildren().addAll(radioButtons);

      progressIndicatorVisible(false);
    });

    // create context menu
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
      if ("remove".equals(propertyChange)) {
        try {
          int removedCardIdx = cards.indexOf(changedCard);
          if (removedCardIdx == -1) {
            throw new IllegalStateException("Card not found, user probably already refreshed");
          }
          DetectedCard card = cards.get(removedCardIdx);
          if (card.equals(changedCard)) {
            card.setTerminal(null);
            card.setTerminalLabel(null);
          } else {
            throw new IllegalStateException("Wrong index, card not found, user probably already refreshed");
          }
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
          // something happened (user might have already refreshed) - just try to remove it
          cards.remove(changedCard);
        }
      } else if ("add".equals(propertyChange)) {
        cards.add(changedCard);
      }
    }, true);
  }

  @Override
  public void close() {
    api.cardDetection(this, false);
  }
}

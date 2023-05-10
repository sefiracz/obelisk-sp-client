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

import cz.sefira.obelisk.CardDetector;
import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.flow.StageHelper;
import cz.sefira.obelisk.token.macos.MacOSKeychain;
import cz.sefira.obelisk.view.StandaloneDialog;
import cz.sefira.obelisk.view.core.StageState;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import cz.sefira.obelisk.token.pkcs11.DetectedCard;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.Product;
import cz.sefira.obelisk.view.core.AbstractUIOperationController;
import cz.sefira.obelisk.token.windows.WindowsKeystore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ProductSelectionController extends AbstractUIOperationController<Product> implements PropertyChangeListener, Initializable {

  private static final Logger logger = LoggerFactory.getLogger(ProductSelectionController.class.getName());

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
  private List<DetectedCard> cards;
  private List<Product> products;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
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

      List<RadioButton> detectedCardButtons = new ArrayList<>();
      for (final DetectedCard card : cards) {
        final RadioButton button = new RadioButton(api.getLabel(card));
        button.setToggleGroup(product);
        button.setUserData(card);
        button.setMnemonicParsing(false);
        if (card.getTerminal() != null) {
          detectedCardButtons.add(0, button); // prioritize connected cards
        } else {
          // disable disconnected cards
          button.setDisable(true);
          button.setText(api.getLabel(card));
          detectedCardButtons.add(button); // add to the end of list
        }
      }
      radioButtons.addAll(detectedCardButtons);

      productsContainer.getChildren().clear();
      productsContainer.getChildren().addAll(radioButtons);

      progressIndicatorVisible(false);
    });

    // create context menu
    dashButton.setOnAction(e -> StandaloneDialog.createDialogFromFXML("/fxml/main-window.fxml", null, StageState.NONBLOCKING, api));
    setLogoBackground(productsContainer);
  }

  private synchronized void progressIndicatorVisible(boolean visible) {
    if(visible) {
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
    Platform.runLater(()-> {
      progressIndicatorVisible(true);
      asyncTask(() -> {
        String propertyName = evt.getPropertyName();
        DetectedCard changedCard = (DetectedCard) evt.getNewValue();
        if (CardDetector.ADD_CARD.equals(propertyName)) {
          cards.remove(changedCard);
          cards.add(changedCard);
        } else if (CardDetector.REMOVE_CARD.equals(propertyName)) {
          try {
            int removedCardIdx = cards.indexOf(changedCard);
            if (removedCardIdx == -1) {
              throw new IllegalStateException("Card not found");
            }
            DetectedCard card = cards.get(removedCardIdx);
            if (card.equals(changedCard)) {
              card.setTerminal(null);
              card.setTerminalLabel(null);
            } else {
              throw new IllegalStateException("Wrong index, card not found");
            }
          } catch (Exception e) {
            logger.error(e.getMessage(), e);
            // something happened (user might have already refreshed) - just try to remove it
            cards.remove(changedCard);
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

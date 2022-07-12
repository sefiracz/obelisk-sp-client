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
package cz.sefira.obelisk.view;

import cz.sefira.obelisk.AppConfigurer;
import cz.sefira.obelisk.UserPreferences;
import cz.sefira.obelisk.view.core.AbstractUIOperationController;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import cz.sefira.obelisk.api.NexuAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class StandaloneDialog {

  private static final Logger logger = LoggerFactory.getLogger(StandaloneDialog.class.getName());

  /**
   * Show welcome message
   * @param api API instance
   */
  public static void showWelcomeMessage(NexuAPI api) {
    DialogMessage dialogMessage = new DialogMessage("welcome.message", DialogMessage.Level.TIMER,
            new String[]{api.getAppConfig().getApplicationName()}, 450, 200);
    dialogMessage.setShowDoNotShowCheckbox(true, true, "welcome-message");
    showDialog(api, dialogMessage, true);
  }

  /**
   * Show SSL installation error message
   * @param api API instance
   * @param browserTypeProperty Property key with browser/keystore type
   * @param certName Name of the certificate file
   */
  public static void showSslError(NexuAPI api, String browserTypeProperty, String browser, String certName) {
    ResourceBundle resources = ResourceBundle.getBundle("bundles/nexu");
    String messageText = MessageFormat.format(resources.getString("install.ca.cert.fail.message"),
            certName, resources.getString(browserTypeProperty));
    if (browser != null) {
      messageText += "\n\n" + MessageFormat.format(resources.getString("install.ca.cert.unused.browser"), browser);
    }
    DialogMessage message = new DialogMessage(DialogMessage.Level.WARNING);
    message.setHeight(250);
    message.setWidth(500);
    message.setMessage(messageText);
    message.setShowDoNotShowCheckbox(true, false, "ssl-warning");
    // add button
    Button cert = new Button();
    cert.setText(resources.getString("install.ca.cert.button.cert.location"));
    cert.getStyleClass().add("btn-secondary");
    message.addButton(new DialogMessage.MessageButton(cert, (action, controller) -> {
      new Thread(() -> {
        try {
          Desktop.getDesktop().open(api.getAppConfig().getNexuHome());
        }
        catch (IOException io) {
          logger.error(io.getMessage(), io);
        }
      }).start();
    }));
    api.getOperationFactory().getMessageDialog(api, message, false);
  }

  public static void showConfirmResetDialog(NexuAPI api, final UserPreferences userPreferences) {
    ResourceBundle resources = ResourceBundle.getBundle("bundles/nexu");
    DialogMessage message = new DialogMessage("preferences.reset.dialog",
        DialogMessage.Level.WARNING, 400, 150);
    message.setShowOkButton(false);

    // add button
    Button cancel = new Button();
    cancel.setText(resources.getString("button.cancel"));
    cancel.getStyleClass().add("btn-default");
    message.addButton(new DialogMessage.MessageButton(cancel, (stage, controller) -> {
      if(stage != null)
        stage.hide();
    }));

    // add confirm button
    Button confirm = new Button();
    confirm.setText(resources.getString("button.ok"));
    confirm.getStyleClass().add("btn-primary");
    message.addButton(new DialogMessage.MessageButton(confirm, (stage, controller) -> {
      userPreferences.clear();
      AppConfigurer.setLocalePreferences(userPreferences);
      AppConfigurer.applyUserPreferences(userPreferences);
      if(stage != null)
        stage.hide();
    }));

    showDialog(api, message, true);
  }

  /**
   * Standalone message dialog implementation
   * @param api API
   * @param dialogMessage Dialog message information
   * @param blockingUI True if blocking UI operation
   */
  public static void showDialog(NexuAPI api, DialogMessage dialogMessage, boolean blockingUI) {
    ResourceBundle resources = ResourceBundle.getBundle("bundles/nexu");

    String appName = "";
    if(api != null) {
      appName = api.getAppConfig().getApplicationName();
      UserPreferences prefs = new UserPreferences(api.getAppConfig());
      // check if message is suppose to be displayed
      String dialogId = dialogMessage.getDialogId();
      if (dialogId != null && prefs.getHiddenDialogIds().contains(dialogId)) {
        return; // do not display message dialog
      }
    }

    // setup stage
    Stage dialogStage = new Stage();
    dialogStage.setAlwaysOnTop(true);
    dialogStage.setTitle((appName.isEmpty() ? "" : appName + " - ")
        + resources.getString(dialogMessage.getLevel().getTitleCode()));
    dialogStage.getIcons().add(new Image(StandaloneDialog.class.getResourceAsStream("/tray-icon.png")));
    // setup scene
    BorderPane borderPane = new BorderPane();
    borderPane.getStylesheets().add(StandaloneDialog.class.getResource("/styles/nexu.css").toString());
    Scene scene = new Scene(borderPane, dialogMessage.getWidth(), dialogMessage.getHeight());

    // left icon box
    VBox leftBox = new VBox();
    leftBox.setAlignment(Pos.CENTER);
    leftBox.getStyleClass().add("icon-primary");
    Region icon = new Region();
    // set dialog icon
    switch (dialogMessage.getLevel()) {
      case SUCCESS:
        icon.getStyleClass().add("icon-success");
        icon.setPrefSize(50, 50);
        leftBox.getChildren().add(icon);
        break;
      case INFORMATION:
        icon.getStyleClass().add("icon-information");
        icon.setPrefSize(50, 50);
        leftBox.getChildren().add(icon);
        break;
      case WARNING:
        icon.getStyleClass().add("icon-warning");
        icon.setPrefSize(54, 50);
        leftBox.getChildren().add(icon);
        break;
      case ERROR:
        icon.getStyleClass().add("icon-error");
        icon.setPrefSize(50, 50);
        leftBox.getChildren().add(icon);
        break;
      case TIMER:
        ProgressIndicator pi = new ProgressIndicator(0.99);
        pi.getStyleClass().add("timerProgress");
        pi.setMinHeight(50);
        pi.setMinWidth(50);
        leftBox.getChildren().add(pi);
        AbstractUIOperationController.TimerService service =
            new AbstractUIOperationController.TimerService(dialogMessage.getTimerLength());
        service.setOnSucceeded(e -> dialogStage.hide());
        pi.progressProperty().bind(service.progressProperty());
        service.start();
      default:
        // no icon
    }
    borderPane.setLeft(leftBox);

    // center message
    VBox centerBox = new VBox();
    centerBox.getStyleClass().add("center");
    centerBox.setAlignment(Pos.CENTER);
    // set message
    String messageText;
    if (dialogMessage.getMessageProperty() != null) {
      // message from property
      messageText = MessageFormat.format(resources.getString(dialogMessage.getMessageProperty()),
              dialogMessage.getMessageParameters());
    } else if (dialogMessage.getMessage() != null) {
      // pre-set message
      messageText = dialogMessage.getMessage();
    } else {
      // default value
      messageText = resources.getString("error");
    }
    Label messageLabel = new Label(messageText);
    messageLabel.setWrapText(true);
    messageLabel.getStyleClass().add("message");
    centerBox.getChildren().add(messageLabel);
    borderPane.setCenter(centerBox);

    // button container
    HBox btnContainer = new HBox();
    btnContainer.setAlignment(Pos.CENTER);
    btnContainer.getStyleClass().add("btn-container");
    Button okButton = new Button(resources.getString("button.ok"));
    okButton.getStyleClass().add("btn-primary");
    // add additional buttons

    for(DialogMessage.MessageButton mb : dialogMessage.getButtons()) {
      Button b = mb.getButton();
      btnContainer.getChildren().add(b);
      b.setOnAction(e -> mb.getButtonAction().action(dialogStage, null));
    }
    // show ok button
    if(dialogMessage.isShowOkButton()) {
      btnContainer.getChildren().add(okButton);
    }

    // do not show checkbox container
    Separator separator = new Separator();
    separator.setStyle("-fx-padding: 0 10 0 10");
    HBox doNotShowBox = new HBox();
    doNotShowBox.getStyleClass().add("do-not-show-container");
    CheckBox doNotShowCheckBox = new CheckBox(resources.getString("checkbox.do.not.show"));
    doNotShowBox.getChildren().add(doNotShowCheckBox);
    VBox doNotShowContainer = new VBox(separator, doNotShowBox);

    // bottom container
    VBox bottomContainer = new VBox(btnContainer);

    // do not show checkbox
    if (api != null && dialogMessage.isShowDoNotShowCheckbox()) {
      bottomContainer.getChildren().add(doNotShowContainer);
      if (dialogMessage.isShowDoNotShowCheckbox() && dialogMessage.isDoNotShowSelected()) {
        doNotShowCheckBox.selectedProperty().setValue(true);
        doNotShowCheckBox.setSelected(true);
      }
    }
    borderPane.setBottom(bottomContainer);

    // OK button action
    okButton.setOnAction(e -> {
      if(doNotShowCheckBox.selectedProperty().getValue()) {
        if (api != null) {
          UserPreferences prefs = new UserPreferences(api.getAppConfig());
          prefs.addHiddenDialogId(dialogMessage.getDialogId());
        }
      }
      dialogStage.hide();
    });

    // show and wait
    dialogStage.setScene(scene);
    if(blockingUI) {
      dialogStage.showAndWait();
    } else {
      dialogStage.show();
    }
  }

}

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
import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.ws.ssl.SSLCommunicationException;
import cz.sefira.obelisk.flow.StageHelper;
import cz.sefira.obelisk.util.TextUtils;
import cz.sefira.obelisk.util.X509Utils;
import cz.sefira.obelisk.util.annotation.NotNull;
import cz.sefira.obelisk.view.core.StageState;
import cz.sefira.obelisk.view.core.TimerService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import cz.sefira.obelisk.api.PlatformAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StandaloneDialog {

  private static final Logger logger = LoggerFactory.getLogger(StandaloneDialog.class.getName());

  private static final Object lock = new Object();

  public static void showConfirmResetDialog(Stage primaryStage, PlatformAPI api, final UserPreferences userPreferences) {
    ResourceBundle resources = ResourceBundle.getBundle("bundles/nexu");
    DialogMessage message = new DialogMessage("preferences.reset.dialog",
        DialogMessage.Level.WARNING, 400, 150);
    message.setShowOkButton(false);
    message.setOwner(primaryStage);

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
      AppConfigurer.applyLocale(null);
      AppConfigurer.applyUserPreferences(userPreferences);
      if(stage != null)
        stage.close();
      if (primaryStage != null)
        primaryStage.close();
    }));

    showDialog(api, message, true);
  }

  /**
   * Standalone message dialog implementation
   * @param api API
   * @param dialogMessage Dialog message information
   * @param blockingUI True if blocking UI operation
   */
  public static void showDialog(PlatformAPI api, DialogMessage dialogMessage, boolean blockingUI) {
    ResourceBundle resources = ResourceBundle.getBundle("bundles/nexu");

    String appName = "";
    if(api != null) {
      appName = AppConfig.get().getApplicationName();
      UserPreferences prefs = new UserPreferences(AppConfig.get());
      // check if message is supposed to be displayed
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
    dialogStage.getIcons().add(new Image(AppConfig.get().getIconLogoStream()));
    if (dialogMessage.getOwner() != null) {
      dialogStage.initOwner(dialogMessage.getOwner());
      dialogStage.initModality(Modality.WINDOW_MODAL);
    } else {
      dialogMessage.setOwner(dialogStage);
    }
    // setup scene
    BorderPane borderPane = new BorderPane();
    borderPane.getStylesheets().add(StandaloneDialog.class.getResource("/styles/nexu.css").toString());
    borderPane.setPrefSize(dialogMessage.getWidth(), dialogMessage.getHeight());
    Scene scene = new Scene(borderPane, dialogMessage.getWidth(), dialogMessage.getHeight());
    StageHelper.getInstance().setMinSize(borderPane, dialogStage);

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
        TimerService service = new TimerService(dialogMessage.getTimerLength());
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
          UserPreferences prefs = new UserPreferences(AppConfig.get());
          prefs.addHiddenDialogId(dialogMessage.getDialogId());
        }
      }
      dialogStage.hide();
    });
    // close with ESCAPE key
    dialogStage.addEventHandler(KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
      if (event.getCode() == KeyCode.ESCAPE) {
        dialogStage.close();
      }
    });
    // show and wait
    dialogStage.setScene(scene);
    if(blockingUI) {
      dialogStage.showAndWait();
    } else {
      dialogStage.show();
    }
  }

  private static final Map<String, StandaloneUIController> BLOCKING_UI = new ConcurrentHashMap<>();

  public static void createDialogFromFXML(@NotNull String fxml, Stage owner, StageState state, Object... params) {
    try {
      if (BLOCKING_UI.get(fxml) != null) {
        BLOCKING_UI.get(fxml).close();
        return;
      }
      FXMLLoader loader = new FXMLLoader();
      loader.setResources(ResourceBundle.getBundle("bundles/nexu"));
      Stage dialogStage = new Stage();
      if (owner != null) {
        dialogStage.initOwner(owner);
        dialogStage.initModality(Modality.WINDOW_MODAL);
      }
      dialogStage.setAlwaysOnTop(true);
      Parent panel = loader.load(StandaloneDialog.class.getResourceAsStream(fxml));
      Scene scene = new Scene(panel);
      dialogStage.setScene(scene);
      dialogStage.getIcons().add(new Image(AppConfig.get().getIconLogoStream()));
      dialogStage.getScene().getStylesheets().add(StandaloneDialog.class.getResource("/styles/nexu.css").toString());
      StandaloneUIController controller = loader.getController();
      controller.init(dialogStage, params);
      switch (state) {
        case BLOCKING:
          BLOCKING_UI.put(fxml, controller);
          logger.info("Showing blocking standalone: "+fxml);
          try (controller) {
            dialogStage.showAndWait();
            logger.info("Releasing blocking standalone: "+fxml);
            BLOCKING_UI.remove(fxml);
          }
          break;
        case HIDDEN:
          logger.info("Spawning hidden standalone: "+fxml);
          dialogStage.hide();
          break;
        case NONBLOCKING:
        default:
          logger.info("Showing nonblocking standalone: "+fxml);
          dialogStage.show();
          break;
      }
    } catch (Throwable t) {
      logger.error(t.getMessage(), t);
      DialogMessage errMsg = new DialogMessage("feedback.message", DialogMessage.Level.ERROR);
      showErrorDialog(errMsg, null, t);
    }
  }

  public static void showSslErrorDialog(@NotNull SSLCommunicationException ex, @NotNull PlatformAPI api) {
    ResourceBundle resources = ResourceBundle.getBundle("bundles/nexu");
    // establish an error message
    String exceptionMsg = ex.getSSLException().getMessage();
    exceptionMsg = exceptionMsg != null ? exceptionMsg.toLowerCase() : "";
    String[] arguments = new String[] {"", resources.getString("dispatcher.ssl.error.contact")};
    if (exceptionMsg.contains("unable to find valid certification path to requested target")) {
      arguments[0] = resources.getString("dispatcher.ssl.error.notTrusted");
    } else if (ex.getSSLException() instanceof SSLPeerUnverifiedException && exceptionMsg.contains("doesn't match")) {
      arguments[0] = MessageFormat.format(resources.getString("dispatcher.ssl.error.hostname"), ex.getHostname());
    }
    else if (exceptionMsg.contains("keyusage")) {
      arguments[0] = resources.getString("dispatcher.ssl.error.keyusage");
    }
    else if (TextUtils.isCausedBy(ex.getSSLException(), CertificateExpiredException.class)) {
      arguments[0] = resources.getString("dispatcher.ssl.error.expired");
    }
    else if (TextUtils.isCausedBy(ex.getSSLException(), CertificateNotYetValidException.class)) {
      arguments[0] = resources.getString("dispatcher.ssl.error.notYet");
    }
    else {
      arguments[0] = resources.getString("dispatcher.ssl.error.generic");
    }
    // create the dialog window with the message
    DialogMessage errMsg = new DialogMessage("dispatcher.ssl.error", DialogMessage.Level.ERROR,
        arguments);
    errMsg.setHeight(220);
    errMsg.setWidth(600);
    Button certs = new Button();
    certs.setText(resources.getString("button.show.ssl.certificates"));
    certs.getStyleClass().add("btn-default");
    errMsg.addButton(new DialogMessage.MessageButton(certs, (start, controller) -> {
      X509Utils.openCertificateChain(errMsg.getOwner(), ex.getCertificateChain(), api);
    }));
    StandaloneDialog.showErrorDialog(errMsg, null, ex.getSSLException());
  }

  public static void showGenericErrorDialog(Throwable t) {
    DialogMessage errMsg = new DialogMessage("feedback.message",
        DialogMessage.Level.ERROR, 475, 150);
    showErrorDialog(errMsg, null, t);
  }

  public static void showErrorDialog(DialogMessage errMsg, String title, Throwable t) {
    showErrorDialog(errMsg, title, t != null ? TextUtils.printException(t) : null);
  }

  public static void showErrorDialog(DialogMessage errMsg, String title, String printedStacktrace) {
    // Display dialog
    ResourceBundle resources = ResourceBundle.getBundle("bundles/nexu");
    if (printedStacktrace != null) {
      Button detail = new Button();
      detail.setText(resources.getString("button.detail"));
      detail.getStyleClass().add("btn-default");
      errMsg.addButton(new DialogMessage.MessageButton(detail, (start, controller) -> {
        // text area with exception stacktrace
        TextArea area = new TextArea();
        area.setText(printedStacktrace);
        area.setStyle("-fx-highlight-fill: #4AA9E7; -fx-highlight-text-fill: #000000;");
        area.setEditable(false);

        BorderPane borderPane = new BorderPane();
        borderPane.setStyle("-fx-control-inner-background: white; -fx-background-color: white;");
        borderPane.setCenter(area);

        Stage detailStage = new Stage();
        if (errMsg.getOwner() != null) {
          detailStage.initOwner(errMsg.getOwner());
          detailStage.initModality(Modality.WINDOW_MODAL);
        }
        detailStage.setAlwaysOnTop(true);
        detailStage.setTitle(Objects.requireNonNullElseGet(title, () -> resources.getString("message.title.error")));
        detailStage.getIcons().add(new Image(AppConfig.get().getIconLogoStream()));
        Scene scene = new Scene(borderPane, 700, 450);
        detailStage.setScene(scene);
        detailStage.addEventHandler(KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
          if (event.getCode() == KeyCode.ESCAPE) {
            detailStage.close();
          }
        });
        detailStage.show();
      }));
    }
    StandaloneDialog.showDialog(null, errMsg, true);
  }

  public static void runLater(Runnable runnable) {
    try {
      Platform.runLater(runnable);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      // TODO - fallback error dialog?
    }
  }

}

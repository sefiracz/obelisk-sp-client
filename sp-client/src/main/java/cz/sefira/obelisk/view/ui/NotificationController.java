package cz.sefira.obelisk.view.ui;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.view.ui.NotificationController
 *
 * Created: 28.04.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.notification.MessageType;
import cz.sefira.obelisk.api.notification.Notification;
import cz.sefira.obelisk.util.ResourceUtils;
import cz.sefira.obelisk.util.TextUtils;
import cz.sefira.obelisk.view.StandaloneUIController;
import cz.sefira.obelisk.view.core.ControllerCore;
import cz.sefira.obelisk.view.core.TimerService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Notification floating window
 */
public class NotificationController extends ControllerCore implements PropertyChangeListener, StandaloneUIController, Initializable {

  private static final Logger logger = LoggerFactory.getLogger(NotificationController.class.getName());

  @FXML
  private BorderPane background;

  @FXML
  private Region icon;

  @FXML
  private ProgressIndicator progress;

  @FXML
  private Label message;

  @FXML
  private Button minimizeButton;

  @FXML
  private Button closeButton;

  @FXML
  private Label timestamp;

  private Stage stage;

  private PropertyChangeSupport propertyChangeSupport;
  private ScheduledExecutorService executorService;
  private double xOffset;
  private double yOffset;
  private boolean hiddenFlag;

  private Notification notification;
  private TimerService service;

  @Override
  public void init(Stage stage, Object... params) {
    this.stage = stage;
    this.propertyChangeSupport = (PropertyChangeSupport) params[0];
    this.propertyChangeSupport.addPropertyChangeListener(this);
    this.executorService = Executors.newSingleThreadScheduledExecutor();

    stage.initStyle(StageStyle.UNDECORATED);
    stage.setTitle(ResourceUtils.getBundle().getString("notification.title"));
    stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, e -> close());
    spawnInRightBottomCorner(background);

    // asynchronous window content update
    asyncUpdate(executorService, false, () -> {
      if (notification != null) {
        Notification currentNotification = notification;
        Platform.runLater(() -> {
          logger.info("Notification FX thread");
          String text = currentNotification.getMessageText();
          if (text != null) {
            resizeFont(text.length()); // resize according to text length
          }
          message.setText(text);
          message.setTooltip(new Tooltip(text));
          timestamp.setText(TextUtils.localizedDatetime(currentNotification.getDate(), true));
          // cancel if notification is in closing process and we have new notification
          if (service != null) {
            service.cancel();
            progress.setVisible(false);
            hiddenFlag = false;
            service = null;
          }
          // check if notification should be spawned
          if (!hiddenFlag) {
            if (!stage.isShowing())
              logger.info("Show notification");
            stage.show();
          }
          if (currentNotification.isClose()) {
            if (!stage.isShowing() || currentNotification.getDelay() == 0) {
              hideNotification(false);
            } else {
              service = createHideTimer(currentNotification.getDelay());
            }
          }
          if (currentNotification.getType() != null) {
            displayIcon(currentNotification.getType());
          }
        });
      } else {
        Platform.runLater(stage::hide);
      }
    });

  }

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    closeButton.setOnAction((e) -> hideNotification(true));

    minimizeButton.setOnAction(e -> stage.setIconified(true));

    background.setOnMousePressed(event -> {
      xOffset = stage.getX() - event.getScreenX();
      yOffset = stage.getY() - event.getScreenY();
    });

    background.setOnMouseDragged(event -> {
      stage.setX(event.getScreenX() + xOffset);
      stage.setY(event.getScreenY() + yOffset);
    });

    setLogoBackground(background, 230, 230);
  }

  private void spawnInRightBottomCorner(Region r) {
    final Rectangle2D screenResolution = Screen.getPrimary().getBounds();
    stage.setX(screenResolution.getWidth() - 75 - r.getPrefWidth());
    stage.setY(screenResolution.getHeight() - 75 - r.getPrefHeight());
  }

  @Override
  public void close() {
    stage.close();
    executorService.shutdown();
    propertyChangeSupport.removePropertyChangeListener(this);
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    asyncTask(() -> notification = (Notification) evt.getNewValue(), true);
  }

  public void displayIcon(MessageType type) {
    icon.setVisible(false);
    boolean visible = true;
    icon.getStyleClass().clear();
    switch (type) {
      case SUCCESS:
        icon.getStyleClass().add("icon-success");
        break;
      case INFO:
        icon.getStyleClass().add("icon-information");
        break;
      case WARNING:
        icon.getStyleClass().add("icon-warning");
        break;
      case ERROR:
        icon.getStyleClass().add("icon-error");
        break;
      case NONE:
      default:
        visible = false;
        break;
    }
    icon.setVisible(visible);
  }


  public TimerService createHideTimer(long seconds) {
    TimerService service = new TimerService(seconds);
    service.setOnSucceeded(e -> hideNotification(false));
    progress.progressProperty().bind(service.progressProperty());
    service.start();
    progress.setVisible(true);
    return service;
  }

  private void hideNotification(boolean flag) {
    logger.info("Hiding notification (flag=" + flag + ")");
    hiddenFlag = flag;
    stage.setIconified(false);
    stage.hide();
  }

  private void resizeFont(int textLength) {
    if (textLength < 120){
      message.setStyle("-fx-font-size: 16px");
    } else if (textLength < 180) {
      message.setStyle("-fx-font-size: 14px");
    } else if (textLength < 200) {
      message.setStyle("-fx-font-size: 13px");
    } else if (textLength < 280) {
      message.setStyle("-fx-font-size: 12px");
    } else {
      message.setStyle("-fx-font-size: 11px");
    }
  }

}

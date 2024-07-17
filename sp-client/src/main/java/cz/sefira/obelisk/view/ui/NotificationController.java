/**
 * © SEFIRA spol. s r.o., 2020-2023
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
import java.util.concurrent.TimeUnit;

/**
 * Notification floating window
 */
public class NotificationController extends ControllerCore implements PropertyChangeListener, StandaloneUIController, Initializable {

  private static final Logger logger = LoggerFactory.getLogger(NotificationController.class.getName());

  private static final int defaultResWidth = 1920;
  private static final int defaultResHeight = 1080;
  private double posOffsetX = 60;
  private double posOffsetY = 60;

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
  private final ScheduledExecutorService resolutionService = Executors.newSingleThreadScheduledExecutor();
  private Rectangle2D screenResolution;
  private double posX;
  private double posY;
  private double xOffset;
  private double yOffset;
  private boolean hiddenFlag; // hides notification for duration of current operation

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

    screenResolution = Screen.getPrimary().getBounds();
    double widthChange = screenResolution.getWidth() / defaultResWidth;
    double heightChange = screenResolution.getHeight() / defaultResHeight;
    spawnInRightBottomCorner(background, posOffsetX * widthChange, posOffsetY * heightChange);

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
            showNotification();
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
      posX = event.getScreenX() + xOffset;
      posY = event.getScreenY() + yOffset;
      stage.setX(posX);
      stage.setY(posY);
    });

    resolutionService.scheduleAtFixedRate(() -> {
      try {
        Rectangle2D newResolution = Screen.getPrimary().getBounds();
        if (!screenResolution.equals(newResolution)) {
          double widthChange = newResolution.getWidth() / screenResolution.getWidth();
          double heightChange = newResolution.getHeight() / screenResolution.getHeight();
          spawnInRightBottomCorner(background, posOffsetX * widthChange, posOffsetY * heightChange);
        }
      } catch (Exception e) {
        // ignore problems
      }
    }, 1, 1, TimeUnit.SECONDS);

    setLogoBackground(background, 230, 230);
  }

  private void spawnInRightBottomCorner(Region r, double offsetX, double offsetY) {
    screenResolution = Screen.getPrimary().getBounds();
    posOffsetX = offsetX;
    posOffsetY = offsetY;
    posX = screenResolution.getWidth() - offsetX; // starting X position
    posY = screenResolution.getHeight() - offsetY; // starting Y position
    stage.setX(posX - r.getPrefWidth());
    stage.setY(posY - r.getPrefHeight());
    logger.info("Spawning notification at position: "+posX+"x"+posY);
  }

  @Override
  public void close() {
    stage.close();
    executorService.shutdown();
    resolutionService.shutdown();
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

  private void showNotification() {
    if (!stage.isShowing())
      logger.info("Show notification");
    stage.show();
  }

  public TimerService createHideTimer(long seconds) {
    TimerService service = new TimerService(seconds);
    service.setOnSucceeded(e -> hideNotification(false));
    progress.progressProperty().bind(service.progressProperty());
    service.start();
    progress.setVisible(true);
    return service;
  }

  /**
   * Hides notification
   * @param flag True if notification is supposed to hide for duration of operation. False to reset for new
   */
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

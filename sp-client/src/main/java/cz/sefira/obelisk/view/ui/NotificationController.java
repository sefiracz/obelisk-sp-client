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

import cz.sefira.obelisk.api.Notification;
import cz.sefira.obelisk.api.PlatformAPI;
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
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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

  private PlatformAPI api;
  private Notification notification;
  private ScheduledExecutorService executorService;
  private double xOffset;
  private double yOffset;

  private TimerService service;

  @Override
  public void init(Stage stage, Object... params) {
    this.stage = stage;
    this.api = (PlatformAPI) params[0];
    this.notification = (Notification) params[1];
    executorService = Executors.newSingleThreadScheduledExecutor();

    // initial notification
    asyncTask(() -> {}, true);

    // asynchronous window content update
    asyncUpdate(executorService, () -> {
      String msg = notification.getMessageText();
      logger.info("Notify: "+msg);
      message.setText(msg);
      timestamp.setText(TextUtils.localizedDatetime(notification.getDate(), true));
      if (notification.isClose()) {
        if (stage != null && (!stage.isShowing() || notification.getDelay() == 0)) {
          close();
        } else {
          service = createTimer(notification.getDelay());
        }
      }
    });

    stage.initStyle(StageStyle.UNDECORATED);
    stage.setTitle(ResourceBundle.getBundle("bundles/nexu").getString("notification.title"));
    stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, e -> close());
    spawnInRightBottomCorner(background);
  }

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    closeButton.setOnAction((e) -> {
      if (stage != null)
        stage.hide();
    });

    minimizeButton.setOnAction(e -> {
      if (stage != null)
        stage.setIconified(true);
    });

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

  private void spawnInRightBottomCorner(Region r){
    final Rectangle2D screenResolution = Screen.getPrimary().getBounds();
    stage.setX(screenResolution.getWidth() - 75 - r.getPrefWidth());
    stage.setY(screenResolution.getHeight() - 75 - r.getPrefHeight());
  }

  @Override
  public void close() {
    logger.info("Closing notification window");
    if (stage != null) {
      stage.close();
    }
    if(executorService != null)
      executorService.shutdown();
    api.closeNotification();
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    asyncTask(() -> {
      Platform.runLater(() -> {
        if (service != null) {
          service.cancel();
          progress.setVisible(false);
        }
      });
      notification = (Notification) evt.getNewValue();
    }, true);
  }

  public TimerService createTimer(long seconds) {
    TimerService service = new TimerService(seconds);
    service.setOnSucceeded(e -> close());
    progress.progressProperty().bind(service.progressProperty());
    service.start();
    progress.setVisible(true);
    return service;
  }

}

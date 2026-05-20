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

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.view.BusyIndicator
 *
 * Created: 01.09.2021
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AppConfig;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;

/**
 * Busy indicator to show to user that something is happening
 *
 * Can be put into try-with-resource block which will define the scope of the busy indicator,
 * the long work where user needs to patiently wait should happen inside this block
 *
 * WORKAROUND: JavaFX has no clean way to not steal focus on new window show,
 * so busy indicator is always present, just hidden beyond viewport and is moved into view when needed.
 */
public class BusyIndicator implements Closeable {

  private static final Logger log = LoggerFactory.getLogger(BusyIndicator.class);
  private static BusyIndicator instance;

  private Scene scene = null;
  private Stage primaryStage = null;
  private Stage stage = null;

  private final ProgressIndicator indicator;

  private BusyIndicator() {
    indicator = new ProgressIndicator();
    Platform.runLater(() -> {
      // toggle on busy indicator
      indicator.setPrefSize(150, 150);
      indicator.setMinHeight(150);
      indicator.setMinWidth(150);
      indicator.setStyle("-fx-progress-color: rgba(0, 0, 0, 0)");
      VBox progressIndicator = new VBox(indicator);
      progressIndicator.setAlignment(Pos.CENTER);
      progressIndicator.setStyle("-fx-background-color: rgba(0, 0, 0, 0)");
      final StackPane background = new StackPane(progressIndicator);
      background.setStyle("-fx-background-color: rgba(0, 0, 0, 0)");
      scene = new Scene(background, 150, 150);
      scene.setFill(Color.TRANSPARENT);
      // primary utility stage (does not show busy indicator window on taskbar)
      primaryStage = new Stage();
      primaryStage.setTitle(AppConfig.get().getApplicationName());
      primaryStage.initStyle(StageStyle.UTILITY);
      primaryStage.initModality(Modality.NONE);
      primaryStage.setWidth(5);
      primaryStage.setHeight(30);
      primaryStage.setOpacity(0);
      primaryStage.setX(-90000);
      primaryStage.setY(-90000);
      Scene s = new Scene(new BorderPane(),1, 1);
      s.setFill(Color.TRANSPARENT);
      primaryStage.setScene(s);
      primaryStage.show();
      // stage
      stage = new Stage();
      stage.initOwner(primaryStage);
      stage.setX(-90000);
      stage.setY(-90000);
      stage.setScene(scene);
      stage.setTitle(AppConfig.get().getApplicationName());
      stage.initStyle(StageStyle.TRANSPARENT);
      stage.initModality(Modality.NONE);
      stage.getIcons().add(new Image(AppConfig.get().getIconLogoStream()));
      stage.show();
    });
  }

  public static synchronized void destroyInstance() {
    if (instance != null) {
      Platform.runLater(() -> {
        synchronized (BusyIndicator.class) {
          try {
            if (instance != null && instance.stage != null) {
              instance.stage.close();
            }
            if (instance != null && instance.primaryStage != null) {
              instance.primaryStage.close();
            }
            instance = null;
          } catch (Exception e) {
            log.error(e.getMessage(), e);
          }
        }
      });
    }
  }

  /**
   * Only returns BusyIndicator singleton instance handle, without showing it or change its behavior.
   *
   * @return Closable instance
   */
  public static synchronized BusyIndicator getRawInstance() {
    return getInstance(false, false);
  }

  /**
   * Toggles busy indicator always on top for potential long workloads to indicate
   * to user that something is still happening.
   *
   * @return Closable instance
   */
  public static synchronized BusyIndicator getInstance() {
    return getInstance(true, true);
  }

  /**
   * Toggles busy indicator always on top for potential long workloads to indicate
   * to user that something is still happening.
   *
   * @param show If set to false the busy indicator actually does not show up.
   * @return Closable instance
   */
  public static synchronized BusyIndicator getInstance(boolean show) {
    return getInstance(show, true);
  }

  /**
   * Toggles busy indicator always on top for potential long workloads to indicate
   * to user that something is still happening.
   *
   * @param show If set to false the busy indicator actually does not show up.
   * @param alwaysOnTop Flag setting the dialog to be set always on top.
   * @return Closable instance
   */
  public static synchronized BusyIndicator getInstance(boolean show, boolean alwaysOnTop) {
    if (instance == null) {
      instance = new BusyIndicator();
    }
    if (show) {
      instance.show(alwaysOnTop);
    }
    return instance;
  }

  public void show(boolean alwaysOnTop) {
    Platform.runLater(() -> {
      final Rectangle2D screenResolution = Screen.getPrimary().getBounds();
      stage.setX((screenResolution.getWidth() / 2) - (scene.getWidth() / 2));
      stage.setY((screenResolution.getHeight() / 2) - (scene.getHeight() / 2));
      stage.setWidth(150);
      stage.setHeight(150);
      indicator.setPrefSize(150, 150);
      indicator.setMinHeight(150);
      indicator.setMinWidth(150);
      indicator.setStyle("-fx-progress-color: rgba(0, 0, 0, 0.75)");
      stage.setAlwaysOnTop(alwaysOnTop);
    });
  }

  @Override
  public void close() {
    Platform.runLater(() -> {
      indicator.setStyle("-fx-progress-color: rgba(0, 0, 0, 0)");
      indicator.setPrefSize(1, 1);
      indicator.setMinHeight(1);
      indicator.setMinWidth(1);
      stage.setWidth(1);
      stage.setHeight(1);
      stage.setX(-90000);
      stage.setY(-90000);
    });
  }
}

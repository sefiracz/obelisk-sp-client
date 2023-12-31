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
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.Closeable;

/**
 * Busy indicator to show to user that something is happening
 *
 * Can be put into try-with-resource block which will define the scope of the busy indicator,
 * the long work where user needs to patiently wait should happen inside this block
 */
public class BusyIndicator implements Closeable {

  private Stage primaryStage = null;
  private Stage stage = null;

  public BusyIndicator() {
    this(true, true);
  }

  public BusyIndicator(boolean show) {
    this(show, true);
  }

  public BusyIndicator(boolean show, boolean alwaysOnTop) {
    if (show) {
      show(alwaysOnTop);
    }
  }

  /**
   * Toggles busy indicator for potential long workloads to indicate
   * to user that something is still happening
   */
  private void show(boolean alwaysOnTop) {
    Platform.runLater(() -> {
      // toggle on busy indicator
      ProgressIndicator indicator = new ProgressIndicator();
      indicator.setPrefSize(150, 150);
      indicator.setMinHeight(150);
      indicator.setMinWidth(150);
      indicator.setStyle("-fx-progress-color: rgba(0, 0, 0, 0.75)");
      VBox progressIndicator = new VBox(indicator);
      progressIndicator.setAlignment(Pos.CENTER);
      progressIndicator.setStyle("-fx-background-color: rgba(0, 0, 0, 0)");
      final StackPane background = new StackPane(progressIndicator);
      background.setStyle("-fx-background-color: rgba(0, 0, 0, 0)");
      final Scene scene = new Scene(background, 150, 150);
      scene.setFill(Color.TRANSPARENT);
      // primary utility stage (does not show busy indicator window on taskbar)
      primaryStage = new Stage();
      primaryStage.initStyle(StageStyle.UTILITY);
      primaryStage.setWidth(5);
      primaryStage.setHeight(30);
      primaryStage.setOpacity(0);
      primaryStage.setX(-1000);
      primaryStage.setY(-1000);
      Scene s = new Scene(new BorderPane(),1, 1);
      s.setFill(Color.TRANSPARENT);
      primaryStage.setScene(s);
      primaryStage.show();
      // stage
      stage = new Stage();
      stage.initOwner(primaryStage);
      final Rectangle2D screenResolution = Screen.getPrimary().getBounds();
      stage.setX((screenResolution.getWidth() / 2) - (scene.getWidth() / 2));
      stage.setY((screenResolution.getHeight() / 2) - (scene.getHeight() / 2));
      stage.setScene(scene);
      stage.setTitle(AppConfig.get().getApplicationName()+" busy indicator");
      stage.setAlwaysOnTop(alwaysOnTop);
      stage.initStyle(StageStyle.TRANSPARENT);
      stage.getIcons().add(new Image(AppConfig.get().getIconLogoStream()));
      stage.show();
    });
  }

  @Override
  public void close() {
    Platform.runLater(() -> {
      // toggle off busy indicator
      if(stage != null) {
        stage.close();
        stage = null;
      }
      if (primaryStage != null) {
        primaryStage.close();
        primaryStage = null;
      }
    });
  }
}

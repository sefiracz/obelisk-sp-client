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
 * cz.sefira.obelisk.view.ui.MainWindowController
 *
 * Created: 26.04.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.flow.StageHelper;
import cz.sefira.obelisk.prefs.PreferencesFactory;
import cz.sefira.obelisk.view.StandaloneUIController;
import cz.sefira.obelisk.view.core.ControllerCore;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Main application window
 */
public class MainWindowController extends ControllerCore implements StandaloneUIController, Initializable {

  private static final Logger logger = LoggerFactory.getLogger(MainWindowController.class.getName());

  public static String currentTab;

  @FXML
  private TabPane tabPane;

  @FXML
  private Tab preferencesTab;

  @FXML
  private Tab manageKeystoresTab;

  @FXML
  private Tab eventsViewerTab;

  @FXML
  private Tab aboutTab;

  @FXML
  private StandaloneUIController manageKeystoresController;

  @FXML
  private StandaloneUIController preferencesController;

  @FXML
  private StandaloneUIController eventsViewerController;

  @FXML
  private StandaloneUIController aboutController;

  private Stage stage;

  @Override
  public void init(Stage stage, Object... params) {
    this.stage = stage;
    stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, e -> close());
    PlatformAPI api = (PlatformAPI) params[0];
    eventsViewerController.init(stage, api);
    manageKeystoresController.init(stage, api);
    preferencesController.init(stage, api, PreferencesFactory.getInstance(AppConfig.get()), false);
    aboutController.init(stage);

    stage.setTitle(tabPane.getSelectionModel().getSelectedItem().getText());
    tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      stage.setTitle(newValue.getText());
      currentTab = newValue.getId();
    });
    stage.addEventHandler(KeyEvent.KEY_RELEASED, (KeyEvent e) -> {
      if (e.getCode() == KeyCode.ESCAPE) {
        logger.info("Closing window 'MainWindow_"+stage.getTitle()+"'");
        close();
        e.consume();
      }
    });
    StageHelper.getInstance().setMinSize(tabPane, stage);

    // spawn window with last opened tab
    if (currentTab != null) {
      try {
        tabPane.getSelectionModel().select((Tab) this.getClass().getDeclaredField(currentTab).get(this));
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    }
  }

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
  }

  @Override
  public void close(){
    try {
      eventsViewerController.close();
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
    try {
      manageKeystoresController.close();
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
    try {
      preferencesController.close();
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
    try {
      aboutController.close();
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
    stage.close();
  }
}

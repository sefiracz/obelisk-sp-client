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

import cz.sefira.obelisk.UserPreferences;
import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.flow.StageHelper;
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
    preferencesController.init(stage, api, new UserPreferences(AppConfig.get()), false);
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

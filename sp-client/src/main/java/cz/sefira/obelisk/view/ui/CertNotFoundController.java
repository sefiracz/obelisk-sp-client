package cz.sefira.obelisk.view.ui;

/*
 * Copyright 2022 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.view.ui.CertNotFoundController
 *
 * Created: 04.01.2022
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.flow.StageHelper;
import cz.sefira.obelisk.view.core.AbstractUIOperationController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class CertNotFoundController extends AbstractUIOperationController<Boolean> implements Initializable {

  @FXML
  private Region icon;

  @FXML
  private VBox messageBox;

  @FXML
  private Label message;

  @FXML
  private Button yes;

  @FXML
  private Button no;

  private PlatformAPI api;
  private ResourceBundle resources;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    this.resources = resourceBundle;
    this.yes.setOnAction(e -> signalEnd(true));
    this.no.setOnAction(e -> signalEnd(false));
  }

  @Override
  public void init(Object... params) {
    api = (PlatformAPI) params[0];

    // set title
    StageHelper.getInstance().setTitle(api.getAppConfig().getApplicationName(), "message.title");

    Platform.runLater(() -> {

      // set message
      message.setText(resources.getString("certificate.not.found"));

      // set dialog icon
      icon.getStyleClass().add("icon-warning");
      icon.setPrefSize(54, 50);

    });
    setLogoBackground(messageBox, 250, 250);
  }

}

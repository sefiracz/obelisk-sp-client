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
 * Copyright 2022 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.view.ui.CertNotFoundController
 *
 * Created: 04.01.2022
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AppConfig;
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
    StageHelper.getInstance().setTitle(AppConfig.get().getApplicationName(), "message.title.information");

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

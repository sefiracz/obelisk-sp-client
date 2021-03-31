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
package lu.nowina.nexu.view.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lu.nowina.nexu.api.SmartcardInfo;
import lu.nowina.nexu.flow.StageHelper;
import lu.nowina.nexu.flow.operation.CoreOperationStatus;
import lu.nowina.nexu.view.core.AbstractUIOperationController;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.view.ui.UnavailableConfigController
 *
 * Created: 01.03.2021
 * Author: hlavnicka
 */

public class UnavailableConfigController extends AbstractUIOperationController<Void> implements Initializable {

  private static final Logger LOG = LoggerFactory.getLogger(UnavailableConfigController.class.getName());

    @FXML
    private Label message;

    @FXML
    private VBox messageContainer;

//    @FXML
//    private Button cancel;

    @FXML
    private Button back;

    @FXML
    private Button hicSuntDracones;

    private ResourceBundle resources;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
      this.resources = ResourceBundle.getBundle("bundles/nexu");
      this.hicSuntDracones.setOnAction(ev -> this.signalEnd(null));
      this.back.setOnAction(e -> this.signalEndWithStatus(CoreOperationStatus.BACK));
    }

    @Override
    public final void init(final Object... params) {
        StageHelper.getInstance().setTitle((String) params[0], "unavailable.configuration.title");

        Platform.runLater(() -> {
          this.message.setText(StringEscapeUtils.unescapeJava(MessageFormat
                  .format(resources.getString("unavailable.configuration.header"), params[0])));

          if (params.length == 2) {
            SmartcardInfo info = (SmartcardInfo) params[1];
            boolean hasKnownDrivers = info != null && info.getDrivers() != null && !info.getDrivers().isEmpty();
            if (hasKnownDrivers && info.getDownloadUrl() != null && !info.getDownloadUrl().isEmpty()) {
              Label driverUrl = new Label();
              driverUrl.setWrapText(true);
              driverUrl.setText(StringEscapeUtils.unescapeJava(resources.getString("unavailable.configuration.driver.url")));
              driverUrl.setStyle("-fx-text-alignment: left; -fx-font-size: 14px;");

              final Hyperlink link = new Hyperlink("URL");
              link.setBorder(Border.EMPTY);
              link.setPadding(new Insets(1, 0, 0, 5));
              link.setOnAction(a -> {
                try {
                  Desktop.getDesktop().browse(new URI(info.getDownloadUrl()));
                } catch (IOException | URISyntaxException e) {
                  LOG.error(e.getMessage(), e);
                }
              });

              HBox hBox = new HBox(driverUrl, link);
              hBox.setPadding(new Insets(0,0,0,15));
              messageContainer.getChildren().add(hBox);
            }
          }

        });
    }

}

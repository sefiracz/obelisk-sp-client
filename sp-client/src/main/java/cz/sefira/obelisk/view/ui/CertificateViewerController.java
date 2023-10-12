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
 * cz.sefira.obelisk.view.ui.CertificateViwerController
 *
 * Created: 22.12.2022
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.dss.x509.CertificateDataParser;
import cz.sefira.obelisk.ipc.Message;
import cz.sefira.obelisk.ipc.MessageQueueFactory;
import cz.sefira.obelisk.view.DialogMessage;
import cz.sefira.obelisk.view.StandaloneDialog;
import cz.sefira.obelisk.view.StandaloneUIController;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Certificate(s) viewer
 */
public class CertificateViewerController implements StandaloneUIController, Initializable {

  private static final Logger logger = LoggerFactory.getLogger(CertificateViewerController.class);

  @FXML
  private Button trust;

  @FXML
  private Button save;

  @FXML
  private Button cancel;

  @FXML
  private VBox certChainBox;

  @FXML
  private TreeView<CertificateDataParser> certificateChainView;

  @FXML
  private TableView<String[]> certDataTable;

  @FXML
  private TableColumn<String[], String> fieldColumn;

  @FXML
  private TableColumn<String[], String> valueColumn;

  @FXML
  private Text valueContent;

  private Stage stage;

  private PlatformAPI api;

  private boolean sslTrust;

  private Message queueMessage;

  final private ObservableList<String[]> observableCertificateData;

  private ResourceBundle resourceBundle;

  public CertificateViewerController() {
    super();
    observableCertificateData = FXCollections.observableArrayList();
  }

  public void init(Stage stage, Object... params) {
    this.stage = stage;
    stage.setResizable(false);
    stage.setTitle(resourceBundle.getString("certificate.viewer.title"));
    stage.getScene().getStylesheets().add(this.getClass().getResource("/styles/styles.css").toString());
    List<X509Certificate> certificates = (List<X509Certificate>) params[0];
    if (params.length == 4) {
      this.api = (PlatformAPI) params[1];
      this.sslTrust = (boolean) params[2];
      this.queueMessage = (Message) params[3];
    }
    // no API interaction, hide trust SSL button
    if (api == null || !sslTrust) {
      trust.setVisible(false);
      trust.setManaged(false);
    }
    try {
      int fieldsCount = 0;
      // parse certificates
      List<CertificateDataParser> chainData = new ArrayList<>();
      for (X509Certificate certificate : certificates) {
        CertificateDataParser parser = new CertificateDataParser(certificate);
        fieldsCount = Math.max(fieldsCount, parser.getFieldData().size());
        chainData.add(parser);
      }
      certDataTable.setPrefHeight(25 + Math.ceil(fieldsCount * 18.5f)); // calculate height to fit without scrolling

      // create list of certificate items starting from root
      List<TreeItem<CertificateDataParser>> items = new ArrayList<>();
      for (int i = chainData.size() - 1; i >= 0; i--) {
        TreeItem<CertificateDataParser> parserItem = new TreeItem<>(chainData.get(i));
        parserItem.setExpanded(true);
        items.add(parserItem);
      }

      // connect certificates in a hierarchy
      for (int i = 0; i < items.size() - 1; i++) {
        if (i + 1 < items.size()) {
          items.get(i).getChildren().add(items.get(i + 1));
        }
      }
      certificateChainView.setRoot(items.get(0));
      certificateChainView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
      certificateChainView.getSelectionModel().select(items.size() - 1);
      certificateChainView.getSelectionModel().selectedItemProperty()
          .addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
              observableCertificateData.setAll(newValue.getValue().getFieldData());
              certDataTable.setItems(observableCertificateData);
              certDataTable.requestFocus();
              certDataTable.getSelectionModel().select(3);
              certDataTable.scrollTo(3);
            }
          });

      if (items.size() == 1) {
        // remove tree view when only displaying single certificate
        certChainBox.getChildren().remove(0);
        certChainBox.setPrefHeight(0);
      }
      else if (items.size() > 3) {
        int height = items.size() * 29;
        certChainBox.setPrefHeight(Math.min(height, 200));
      }

      trust.setOnAction(e -> {
        if (api != null) {
          DialogMessage message = new DialogMessage("certificate.viewer.temp.trust", DialogMessage.Level.WARNING);
          message.setShowOkButton(false);
          message.setOwner(stage);
          Button no = new Button(resourceBundle.getString("button.cancel"));
          no.getStyleClass().add("btn-default");
          Button yes = new Button(resourceBundle.getString("button.ok"));
          yes.getStyleClass().add("btn-primary");
          message.addButton(new DialogMessage.MessageButton(no, (dialogStage, controller) -> dialogStage.close()));
          message.addButton(new DialogMessage.MessageButton(yes, (dialogStage, controller) -> {
            boolean repeat = false;
            try {
              CertificateDataParser parser = certificateChainView.getSelectionModel().getSelectedItem().getValue();
              X509Certificate certificate = parser.getX509Certificate();
              api.getSslCertificateProvider().addToRuntimeTruststore(List.of(certificate)); // add to temp trust
              repeat = true;
            } catch (Exception ex) {
              logger.error(ex.getMessage(), ex);
              DialogMessage errMsg = new DialogMessage("feedback.message", DialogMessage.Level.ERROR, 380, 140);
              errMsg.setOwner(dialogStage);
              StandaloneDialog.showErrorDialog(errMsg, null, ex);
            }
            // close all dialog windows
            dialogStage.close();
            stage.close();
            if (stage.getOwner() instanceof Stage) {
              ((Stage) stage.getOwner()).close();
            }
            // push message back to queue
            if (repeat && queueMessage != null) {
              MessageQueueFactory.getInstance(AppConfig.get()).addMessage(queueMessage);
            }
          }));
          StandaloneDialog.showDialog(api, message, true);
        }
      });

      save.setOnAction(e -> {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(resourceBundle.getString("certificate.viewer.save.title"));
        CertificateDataParser parser = certificateChainView.getSelectionModel().getSelectedItem().getValue();
        String cn = parser.getField(parser.getSubjectDN(), "CN");
        cn = cn.replaceAll(" ", "_");
        cn = cn.toLowerCase();
        fileChooser.setInitialFileName(cn + ".cer");
        File f = fileChooser.showSaveDialog(stage);
        if (f != null) {
          try (OutputStream out = Files.newOutputStream(f.toPath())) {
            out.write(parser.getX509Certificate().getEncoded());
          }
          catch (IOException | CertificateEncodingException ex) {
            throw new RuntimeException(ex); // TODO handle error
          }
        }
      });

      Platform.runLater(() -> {
        observableCertificateData.setAll(items.get(items.size() - 1).getValue().getFieldData());
        certDataTable.setItems(observableCertificateData);
        certDataTable.requestFocus();
        certDataTable.getSelectionModel().select(3);
        certDataTable.scrollTo(3);
      });
    } catch (GeneralSecurityException e) {
      logger.error(e.getMessage(), e);
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    this.resourceBundle = resourceBundle;
    cancel.setOnAction(e -> stage.close());
    certDataTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    certDataTable.setRowFactory(tv -> {
      TableRow<String[]> row = new TableRow<>() {};
      row.setStyle("-fx-cell-size: 18px; -fx-font-size: 12px;");
      return row;
    });
    valueContent.setStyle("-fx-font-family: Monospaced");

    fieldColumn.setCellValueFactory((param) -> new ReadOnlyStringWrapper(param.getValue()[0]));
    // certificate common name
    valueColumn.setCellValueFactory((param) -> {
      String value = param.getValue()[1];
      if (value.contains("\n")) {
        value = value.replace("\n", " ");
      }
      return new ReadOnlyStringWrapper(value);
    });

    certDataTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if(newValue != null) {
        String value = "";
        if (newValue.length == 2) {
          value = newValue[1];
        } else if (newValue.length == 3) {
          value = newValue[2];
        }
        valueContent.setText(value);
      } else {
        valueContent.setText(null);
      }
    });

  }

  @Override
  public void close() throws IOException {
    stage.close();
  }
}

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

import cz.sefira.obelisk.Utils;
import cz.sefira.obelisk.api.AbstractProduct;
import cz.sefira.obelisk.api.ConfiguredKeystore;
import cz.sefira.obelisk.api.DetectedCard;
import cz.sefira.obelisk.api.NexuAPI;
import cz.sefira.obelisk.flow.StageHelper;
import cz.sefira.obelisk.generic.RegisteredProducts;
import cz.sefira.obelisk.view.core.AbstractUIOperationController;
import cz.sefira.obelisk.view.x509.CertificateInfoData;
import eu.europa.esig.dss.DSSASN1Utils;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import org.bouncycastle.asn1.x500.style.BCStyle;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.ResourceBundle;

/**
 * description
 */
public class CertificateViwerController  extends AbstractUIOperationController<Void> implements Initializable {


  @FXML
  private Button cancel;

  @FXML
  private TableView<String[]> certDataTable;

  @FXML
  private TableColumn<String[], String> fieldColumn;

  @FXML
  private TableColumn<String[], String> valueColumn;

  @FXML
  private Text valueContent;

  final private ObservableList<String[]> observableCertificateData;

  public CertificateViwerController() {
    super();
    observableCertificateData = FXCollections.observableArrayList();
  }

  @Override
  public void init(Object... params) {
    System.out.println("CERT VIEWER");
    StageHelper.getInstance().setTitle("", "certificate.viewer.title");
    if (params.length == 1) {
      Certificate certificate = (Certificate) params[0];
      List<String[]> fieldData = new CertificateInfoData(certificate).getFieldData();
      Platform.runLater(() -> {
        observableCertificateData.setAll(fieldData);
        certDataTable.setItems(observableCertificateData);
        certDataTable.requestFocus();
        certDataTable.getSelectionModel().select(3);
        certDataTable.scrollTo(3);
      });
    }
  }

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    cancel.setOnAction(e -> signalEnd(null));
    certDataTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);


    certDataTable.setRowFactory(tv -> {
      TableRow<String[]> row = new TableRow<String[]>() {};
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
}

package lu.nowina.nexu.view.ui;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.view.ui.UnknownCertificateMessageController
 *
 * Created: 12.01.2021
 * Author: hlavnicka
 */

import eu.europa.esig.dss.DSSUtils;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.x509.CertificateToken;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import lu.nowina.nexu.Utils;
import lu.nowina.nexu.flow.StageHelper;
import lu.nowina.nexu.view.core.AbstractUIOperationController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Info dialog about unknown certificate being tried to be used for signature
 */
public class UnknownCertificateMessageController extends AbstractUIOperationController<Object> implements Initializable {

  private static final Logger logger = LoggerFactory.getLogger(UnknownCertificateMessageController.class.getName());

  @FXML
  private Label message;

  @FXML
  private javafx.scene.control.Button ok;

  @FXML
  private javafx.scene.control.Button cancel;

  @FXML
  private Button certificate;

  private CertificateToken certificateToken;

  private ResourceBundle resources;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    this.resources = resources;
    this.ok.setOnAction(e -> signalEnd(null));
    this.cancel.setOnAction((e) -> this.signalUserCancel());
    this.certificate.setOnAction(actionEvent -> Utils.openCertificate(DSSUtils.convertToPEM(certificateToken)));
  }

  @Override
  @SuppressWarnings("unchecked")
  public void init(final Object... params) {
    StageHelper.getInstance().setTitle((String) params[0], "message.title");
    final String value = (String) params[1];
    if (value != null) {
      message.setText(MessageFormat.format(resources.getString(value), new Object[]{}));
    }
    certificateToken = (CertificateToken) params[2];
  }

}
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
package cz.sefira.obelisk.view.ui;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.view.ui.UnknownCertificateMessageController
 *
 * Created: 12.01.2021
 * Author: hlavnicka
 */

import cz.sefira.obelisk.Utils;
import cz.sefira.obelisk.flow.StageHelper;
import cz.sefira.obelisk.view.core.AbstractUIOperationController;
import eu.europa.esig.dss.DSSUtils;
import eu.europa.esig.dss.x509.CertificateToken;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

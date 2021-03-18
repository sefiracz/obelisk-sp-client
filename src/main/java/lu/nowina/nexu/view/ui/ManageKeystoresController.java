/**
 * © Nowina Solutions, 2015-2016
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package lu.nowina.nexu.view.ui;

import eu.europa.esig.dss.DSSASN1Utils;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import lu.nowina.nexu.ProductDatabase;
import lu.nowina.nexu.Utils;
import lu.nowina.nexu.api.*;
import lu.nowina.nexu.flow.StageHelper;
import lu.nowina.nexu.generic.RegisteredProducts;
import lu.nowina.nexu.view.core.AbstractUIOperationController;
import lu.nowina.nexu.windows.keystore.WindowsKeystore;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Allow to manage saved keystores.
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public class ManageKeystoresController extends AbstractUIOperationController<Void> implements Initializable {

	private static final Logger logger = LoggerFactory.getLogger(ManageKeystoresController.class.getName());

	@FXML
	private Button certificate;

	@FXML
	private Button remove;

	@FXML
	private TableView<AbstractProduct> keystoresTable;

	@FXML
	private TableColumn<AbstractProduct, String> keystoreNameTableColumn;

	@FXML
  private TableColumn<AbstractProduct, String> keystoreCertificateNameTableColumn;

	@FXML
	private TableColumn<AbstractProduct, String> keystoreTypeTableColumn;

	@FXML
	private Label keystoreLabel;

	private final ObservableList<AbstractProduct> observableKeystores;

	private NexuAPI api;

	private List<AbstractProduct> filtered;

	public ManageKeystoresController() {
		super();
		observableKeystores = FXCollections.observableArrayList();
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		keystoresTable.setPlaceholder(new Label(resources.getString("table.view.no.content")));
		keystoresTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		// double-click show certificate
		keystoresTable.setRowFactory( tv -> {
			TableRow<AbstractProduct> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
					Utils.openCertificate(row.getItem().getCertificate());
				}
			});
			return row;
		});
		// keystore/device name
		keystoreNameTableColumn.setCellValueFactory((param) -> new ReadOnlyStringWrapper(param.getValue().getLabel()));
		// certificate common name
    keystoreCertificateNameTableColumn.setCellValueFactory((param) -> {
      String cn = "";
      try {
        String certBase64 = param.getValue().getCertificate();
        byte[] cert = Base64.decodeBase64(certBase64);
        CertificateFactory factory = CertificateFactory.getInstance("X509");
        X509Certificate x509Certificate = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(cert));
        cn = DSSASN1Utils.extractAttributeFromX500Principal(BCStyle.CN, x509Certificate.getSubjectX500Principal());
      } catch (CertificateException e) {
        logger.error(e.getMessage(), e);
      }
      return new ReadOnlyStringWrapper(cn);
    });
    // keystore type
		keystoreTypeTableColumn
				.setCellValueFactory((param) -> new ReadOnlyStringWrapper(param.getValue().getType().getSimpleLabel()));
		keystoresTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue != null) {
				if(newValue instanceof ConfiguredKeystore) {
					keystoreLabel.setText(((ConfiguredKeystore) newValue).getUrl());
				} else if(newValue instanceof DetectedCard) {
					keystoreLabel.setText(newValue.getSimpleLabel());
				} else {
					keystoreLabel.setText(newValue.getLabel());
				}
			} else {
				keystoreLabel.setText(null);
			}
		});
		keystoresTable.setItems(observableKeystores);

		certificate.disableProperty().bind(keystoresTable.getSelectionModel().selectedItemProperty().isNull());
		certificate.setOnAction(actionEvent -> {
      Utils.openCertificate(keystoresTable.getSelectionModel().getSelectedItem().getCertificate());
		});

		remove.disableProperty().bind(keystoresTable.getSelectionModel().selectedItemProperty().isNull());
		remove.setOnAction((event) -> observableKeystores.remove(keystoresTable.getSelectionModel().getSelectedItem()));

		observableKeystores.addListener((ListChangeListener<AbstractProduct>)(c) -> {
			while(c.next()) {
				for(final AbstractProduct p : c.getRemoved()) {
					List<Match> matchList = api.matchingProductAdapters(p);
					if(!matchList.isEmpty()) {
						ProductDatabase database = matchList.get(0).getAdapter().getProductDatabase();
						database.remove(api, p);
					}
				}
			}
		});
	}

	@Override
	public void init(Object... params) {
		api = (NexuAPI) params[0];
		StageHelper.getInstance().setTitle("", "systray.menu.manage.keystores");
		if(params.length > 1) {
      filtered = (List<AbstractProduct>) params[1];
    }
		Platform.runLater(() -> {
		  if(filtered != null && !filtered.isEmpty()) {
		    // show only this subset
        observableKeystores.setAll(filtered);
      } else {
		    // show all
        observableKeystores.setAll(RegisteredProducts.getMap().getAllProducts());
      }
		});
	}

}

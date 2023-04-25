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
package cz.sefira.obelisk.view.ui;

import cz.sefira.obelisk.ProductStorage;
import cz.sefira.obelisk.token.keystore.ConfiguredKeystore;
import cz.sefira.obelisk.token.pkcs11.DetectedCard;
import cz.sefira.obelisk.util.X509Utils;
import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.flow.StageHelper;
import cz.sefira.obelisk.generic.QuickAccessProductsMap;
import cz.sefira.obelisk.view.core.AbstractUIOperationController;
import cz.sefira.obelisk.dss.DSSASN1Utils;
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
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Allow to manage saved keystores.
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public class ManageKeystoresController extends AbstractUIOperationController<Void> implements Initializable {

	private static final Logger logger = LoggerFactory.getLogger(ManageKeystoresController.class.getName());

	@FXML
	private Button cancel;

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
	private TableColumn<AbstractProduct, String> keystoreIssuerNameTableColumn;

	@FXML
	private TableColumn<AbstractProduct, String> keystoreTypeTableColumn;

	@FXML
	private TableColumn<AbstractProduct, String> keystoreNotAfterTableColumn;

	@FXML
	private Label keystoreLabel;

	private final ObservableList<AbstractProduct> observableKeystores;

	private PlatformAPI api;

	private List<AbstractProduct> filtered;

	public ManageKeystoresController() {
		super();
		observableKeystores = FXCollections.observableArrayList();
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		cancel.setOnAction(e -> signalEnd(null));
		keystoresTable.setPlaceholder(new Label(resources.getString("table.view.no.content")));
		keystoresTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		// double-click show certificate
		keystoresTable.setRowFactory(tv -> {

		 TableRow<AbstractProduct> row = new TableRow<AbstractProduct>() {

				@Override
				protected void updateItem(AbstractProduct item, boolean empty) {
					super.updateItem(item, empty);
					try {
						if (item != null) {
							this.getStyleClass().clear();
							this.getStyleClass().addAll("cell", "indexed-cell", "table-row-cell");
							String certBase64 = item.getCertificate();
							try {
								X509Utils.getCertificateFromBase64(certBase64).checkValidity();
							} catch (CertificateExpiredException | CertificateNotYetValidException e) {
								this.getStyleClass().add("expired-row");
							}
						}
					} catch (CertificateException e) {
						logger.error(e.getMessage(), e);
					}
				}
		 };

			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
					X509Utils.openPEMCertificate(X509Utils.wrapPEMCertificate(row.getItem().getCertificate()));
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
				X509Certificate x509Certificate = X509Utils.getCertificateFromBase64(certBase64);
        cn = DSSASN1Utils.extractAttributeFromX500Principal(BCStyle.CN, x509Certificate.getSubjectX500Principal());
      } catch (CertificateException e) {
        logger.error(e.getMessage(), e);
      }
      return new ReadOnlyStringWrapper(cn);
    });
		keystoreIssuerNameTableColumn.setCellValueFactory((param) -> {
			String cn = "";
			try {
				String certBase64 = param.getValue().getCertificate();
				X509Certificate x509Certificate = X509Utils.getCertificateFromBase64(certBase64);
				cn = DSSASN1Utils.extractAttributeFromX500Principal(BCStyle.CN, x509Certificate.getIssuerX500Principal());
			} catch (CertificateException e) {
				logger.error(e.getMessage(), e);
			}
			return new ReadOnlyStringWrapper(cn);
		});
		keystoreNotAfterTableColumn.setCellValueFactory((param) -> {
			String validUntil = "";
			try {
				String certBase64 = param.getValue().getCertificate();
				X509Certificate x509Certificate = X509Utils.getCertificateFromBase64(certBase64);
				Date notAfter = x509Certificate.getNotAfter();
				SimpleDateFormat sdf;
				if(Locale.getDefault().getLanguage().equals("cs")) {
					sdf = new SimpleDateFormat("dd. MM. yyyy");
				} else {
					sdf = new SimpleDateFormat("dd/MM/yyyy");
				}
				validUntil = sdf.format(notAfter);
			} catch (CertificateException e) {
				logger.error(e.getMessage(), e);
			}
			return new ReadOnlyStringWrapper(validUntil);
		});
    // keystore type
		keystoreTypeTableColumn
				.setCellValueFactory((param) -> new ReadOnlyStringWrapper(param.getValue().getType().getSimpleLabel()));
		keystoresTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue != null) {
				if(newValue instanceof ConfiguredKeystore) {
					String path = ((ConfiguredKeystore) newValue).getUrl();
					try {
						path = Paths.get(new URI(path)).toFile().getAbsolutePath();
					}
					catch (URISyntaxException e) {
						logger.error(e.getMessage(), e);
					}
					keystoreLabel.setText(path);
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
		certificate.setOnAction(actionEvent -> X509Utils.openPEMCertificate(
				X509Utils.wrapPEMCertificate(keystoresTable.getSelectionModel().getSelectedItem().getCertificate())));

		remove.disableProperty().bind(keystoresTable.getSelectionModel().selectedItemProperty().isNull());
		remove.setOnAction((event) -> observableKeystores.remove(keystoresTable.getSelectionModel().getSelectedItem()));

		observableKeystores.addListener((ListChangeListener<AbstractProduct>)(c) -> {
			while(c.next()) {
				for(final AbstractProduct p : c.getRemoved()) {
					List<Match> matchList = api.matchingProductAdapters(p);
					if(!matchList.isEmpty()) {
						ProductStorage<?> storage = matchList.get(0).getAdapter().getProductStorage();
						storage.remove(p);
					}
				}
			}
		});
	}

	@Override
	public void init(Object... params) {
		api = (PlatformAPI) params[0];
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
        observableKeystores.setAll(QuickAccessProductsMap.access().getAllProducts());
      }
		});
	}

}

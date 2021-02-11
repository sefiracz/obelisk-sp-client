/**
 * © Nowina Solutions, 2015-2016
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
import lu.nowina.nexu.api.*;
import lu.nowina.nexu.generic.ProductsMap;
import lu.nowina.nexu.view.core.AbstractUIOperationController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
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
	private TableColumn<AbstractProduct, String> keystoreKeyAliasTableColumn;

	@FXML
	private TableColumn<AbstractProduct, String> keystoreTypeTableColumn;

	@FXML
	private Label keystoreLabel;

	private final ObservableList<AbstractProduct> observableKeystores;

	private NexuAPI api;

	public ManageKeystoresController() {
		super();
		observableKeystores = FXCollections.observableArrayList();
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		keystoresTable.setPlaceholder(new Label(resources.getString("table.view.no.content")));
		keystoresTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		keystoreNameTableColumn.setCellValueFactory((param) ->
			new ReadOnlyStringWrapper(param.getValue().getLabel())
		);
		keystoreKeyAliasTableColumn.setCellValueFactory((param) -> {
			final String keyAlias = param.getValue().getKeyAlias();
			return new ReadOnlyStringWrapper(keyAlias);
		});
		keystoreTypeTableColumn.setCellValueFactory((param) -> {
			final String type = param.getValue().getType().getLabel();
			return new ReadOnlyStringWrapper(type);
		});
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
			if (Desktop.isDesktopSupported()) {
				try {
					final File tmpFile = File.createTempFile("certificate", ".crt");
					tmpFile.deleteOnExit();
					final String certificateStr = keystoresTable.getSelectionModel().getSelectedItem().getCertificate();
					final FileWriter writer = new FileWriter(tmpFile);
					writer.write(certificateStr);
					writer.close();
					new Thread(() -> {
						try {
							Desktop.getDesktop().open(tmpFile);
						} catch (final IOException e) {
							logger.error(e.getMessage(), e);
						}
					}).start();
				} catch (final Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		});

		remove.disableProperty().bind(keystoresTable.getSelectionModel().selectedItemProperty().isNull());
		remove.setOnAction((event) -> observableKeystores.remove(keystoresTable.getSelectionModel().getSelectedItem()));

		observableKeystores.addListener((ListChangeListener<AbstractProduct>)(c) -> {
			while(c.next()) {
				for(final AbstractProduct p : c.getRemoved()) {
					List<Match> matchList = api.matchingProductAdapters(p);
					if(!matchList.isEmpty()) {
						ProductDatabase database = matchList.get(0).getAdapter().getProductDatabase();
						database.remove(p);
					}
				}
			}
		});
	}

	@Override
	public void init(Object... params) {
		api = (NexuAPI) params[0];
		Platform.runLater(() -> {
			observableKeystores.setAll(ProductsMap.getMap().getAllProducts());
		});
	}

}

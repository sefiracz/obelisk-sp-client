/**
 * © Nowina Solutions, 2015-2015
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

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.flow.StageHelper;
import cz.sefira.obelisk.view.StandaloneUIController;
import cz.sefira.obelisk.view.core.AbstractUIOperationController;
import cz.sefira.obelisk.view.core.ControllerCore;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AboutController extends ControllerCore implements StandaloneUIController, Initializable {

	@FXML
	private GridPane gridPane;

	@FXML
	private Label aboutTitle;

	@FXML
	private Button cancel;

	@FXML
	private Label applicationVersion;

	private Stage primaryStage;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		cancel.setOnAction(e -> windowClose(primaryStage));
	}

	@Override
	public void init(Stage stage, Object... params) {
		primaryStage = stage;
		final String applicationName = AppConfig.get().getApplicationName();
		this.aboutTitle.setText(aboutTitle.getText() + " " + applicationName);
		StageHelper.getInstance().setTitle("", "about.header");
		final String applicationVersion = AppConfig.get().getApplicationVersion();
		this.applicationVersion.setText(applicationVersion);
		setLogoBackground(gridPane);
	}

	@Override
	public void close() throws IOException {

	}
}

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
package lu.nowina.nexu.view.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;
import lu.nowina.nexu.AppConfigurer;
import lu.nowina.nexu.UserPreferences;
import lu.nowina.nexu.api.EnvironmentInfo;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.OS;
import lu.nowina.nexu.flow.StageHelper;
import lu.nowina.nexu.object.model.AppLanguage;
import lu.nowina.nexu.view.DialogMessage;
import lu.nowina.nexu.view.StandaloneDialog;
import lu.nowina.nexu.view.core.AbstractUIOperationController;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

public class PreferencesController extends AbstractUIOperationController<Void> implements Initializable {

	@FXML
	private GridPane gridPane;

	@FXML
	private Button ok;

	@FXML
	private Button cancel;

	@FXML
	private Button reset;

	@FXML
	private CheckBox onStartup;

	@FXML
	private ComboBox<AppLanguage> language;

	private NexuAPI api;

	private UserPreferences userPreferences;

	private BooleanProperty readOnly;

	private final AppLanguage cz = new AppLanguage("Čeština", new Locale("cs", "CZ"));
	private final AppLanguage en = new AppLanguage("English", Locale.ENGLISH);

	private static final boolean isWindows;

	static {
		isWindows = EnvironmentInfo.buildFromSystemProperties(System.getProperties()).getOs().equals(OS.WINDOWS);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		readOnly = new SimpleBooleanProperty(false);
		ok.disableProperty().bind(readOnly);
		reset.disableProperty().bind(readOnly);

		language.getItems().add(cz);
		language.getItems().add(en);

		ok.setOnAction((evt) -> {
			userPreferences.setLanguage(language.isDisabled() ? null :
					language.getSelectionModel().getSelectedItem().getLocale().getLanguage());
			userPreferences.setAutoStart(onStartup.selectedProperty().getValue());
			AppConfigurer.setLocalePreferences(userPreferences);
			AppConfigurer.applyUserPreferences(userPreferences);
			signalEnd(null);
		});
		cancel.setOnAction((e) -> signalEnd(null));
		reset.setOnAction((e) -> {
			StandaloneDialog.showConfirmResetDialog(api, userPreferences);
			signalEnd(null);
		});
	}

	@Override
	public void init(Object... params) {
		StageHelper.getInstance().setTitle("", "preferences.header");
		this.api = (NexuAPI) params[0];
		this.userPreferences = (UserPreferences) params[1];
		this.readOnly.set((boolean) params[2]);
		if(Locale.getDefault().getLanguage().equals(cz.getLocale().getLanguage())) {
			language.getSelectionModel().select(cz);
		} else {
			language.getSelectionModel().select(en);
		}
		if(isWindows) {
			onStartup.selectedProperty().setValue(userPreferences.getAutoStart());
		} else {
			gridPane.getChildren().removeIf(node -> node.getId() != null && node.getId().startsWith("startUp"));
		}
	}

}

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

import cz.sefira.obelisk.AppConfigurer;
import cz.sefira.obelisk.UserPreferences;
import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.EnvironmentInfo;
import cz.sefira.obelisk.flow.StageHelper;
import cz.sefira.obelisk.generic.SessionManager;
import cz.sefira.obelisk.object.model.AppLanguage;
import cz.sefira.obelisk.view.StandaloneDialog;
import cz.sefira.obelisk.view.core.AbstractUIOperationController;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import cz.sefira.obelisk.api.NexuAPI;
import cz.sefira.obelisk.api.OS;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

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
	private CheckBox firefoxSupport;

	@FXML
	private ComboBox<AppLanguage> language;

	@FXML
	private Button minus;

	@FXML
	private TextField durationTextField;

	@FXML
	private Button plus;

	private NexuAPI api;

	private UserPreferences userPreferences;

	private BooleanProperty readOnly;

	private ResourceBundle resources;

	private int duration = 0;

	private static final boolean isWindows;
	private static final boolean isMac;
	private static final boolean isLinux;

	static {
		isWindows = EnvironmentInfo.buildFromSystemProperties(System.getProperties()).getOs().equals(OS.WINDOWS);
		isMac = EnvironmentInfo.buildFromSystemProperties(System.getProperties()).getOs().equals(OS.MACOSX);
		isLinux = EnvironmentInfo.buildFromSystemProperties(System.getProperties()).getOs().equals(OS.LINUX);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.resources = resources;
		readOnly = new SimpleBooleanProperty(false);
		ok.disableProperty().bind(readOnly);
		reset.disableProperty().bind(readOnly);

		language.getItems().add(AppConfig.CZ);
		language.getItems().add(AppConfig.EN);

		minus.setOnAction((e) -> minusDuration());
		minus.addEventFilter(MouseEvent.ANY, new PressedRepeatEventHandler(this::minusDuration,
				325, 125, TimeUnit.MILLISECONDS));

		plus.setOnAction((e) -> plusDuration());
		plus.addEventFilter(MouseEvent.ANY, new PressedRepeatEventHandler(this::plusDuration,
				325, 125, TimeUnit.MILLISECONDS));

		durationTextField.setTextFormatter(new TextFormatter<>(this::filter));
		durationTextField.setOnMouseClicked((e) -> setTextFieldDuration(true));

		ok.setOnAction((evt) -> {
			userPreferences.setLanguage(language.isDisabled() ? null :
					language.getSelectionModel().getSelectedItem().getLocale().getLanguage());
			userPreferences.setAutoStart(onStartup.selectedProperty().getValue());
			userPreferences.setFirefoxSupport(firefoxSupport.selectedProperty().getValue());
			if (duration == 0 || !Integer.valueOf(duration).equals(userPreferences.getCacheDuration())) {
				SessionManager.getManager().destroySecret();
			}
			userPreferences.setCacheDuration(duration);
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
		if(Locale.getDefault().getLanguage().equals(AppConfig.CZ.getLocale().getLanguage())) {
			language.getSelectionModel().select(AppConfig.CZ);
		} else {
			language.getSelectionModel().select(AppConfig.EN);
		}
		// auto start-up only for Windows, others can do it via system
		if(isWindows) {
			onStartup.selectedProperty().setValue(userPreferences.getAutoStart());
		} else {
			gridPane.getChildren().removeIf(node -> node.getId() != null && node.getId().startsWith("startUp"));
		}
		// firefox is always enabled for Linux, no need for preference option
		if(isLinux) {
			gridPane.getChildren().removeIf(node -> node.getId() != null && node.getId().startsWith("firefoxSupport"));
		} else {
			firefoxSupport.selectedProperty().setValue(userPreferences.getFirefoxSupport());
		}
		this.duration = userPreferences.getCacheDuration();
		setTextFieldDuration(true);
	}

  private TextFormatter.Change filter(TextFormatter.Change change) {
    if (!change.getControlNewText().matches("\\b([0-9]|[12][0-9]|30)\\b")) {
      change.setText("");
    }
    else {
      try {
        duration = Integer.parseInt(change.getControlNewText());
        setTextFieldDuration(false);
      }
      catch (NumberFormatException ignored) {
        // not a number, ignore this input
      }
    }
    return change;
  }

  void setTextFieldDuration(boolean clearText) {
    String minutes = duration == 0 || duration > 4 ? resources.getString("preferences.minutes.universal") :
        duration == 1 ? resources.getString("preferences.minute") : resources.getString("preferences.minutes");
    if (clearText) {
      durationTextField.setText("");
    }
    durationTextField.setPromptText(duration + " " + minutes);
  }

  private void minusDuration() {
    if (duration > 0) {
      duration--;
      setTextFieldDuration(true);
    }
  }

  private void plusDuration() {
    if (duration < 30) {
      duration++;
      setTextFieldDuration(true);
    }
  }

}

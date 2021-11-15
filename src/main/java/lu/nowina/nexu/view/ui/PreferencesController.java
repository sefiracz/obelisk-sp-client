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
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import lu.nowina.nexu.AppConfigurer;
import lu.nowina.nexu.UserPreferences;
import lu.nowina.nexu.api.EnvironmentInfo;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.OS;
import lu.nowina.nexu.flow.StageHelper;
import lu.nowina.nexu.generic.SessionManager;
import lu.nowina.nexu.object.model.AppLanguage;
import lu.nowina.nexu.view.StandaloneDialog;
import lu.nowina.nexu.view.core.AbstractUIOperationController;

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

	private final AppLanguage cz = new AppLanguage("Čeština", new Locale("cs", "CZ"));
	private final AppLanguage en = new AppLanguage("English", Locale.ENGLISH);

	private static final boolean isWindows;

	static {
		isWindows = EnvironmentInfo.buildFromSystemProperties(System.getProperties()).getOs().equals(OS.WINDOWS);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.resources = resources;
		readOnly = new SimpleBooleanProperty(false);
		ok.disableProperty().bind(readOnly);
		reset.disableProperty().bind(readOnly);

		language.getItems().add(cz);
		language.getItems().add(en);

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

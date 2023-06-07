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
import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.NotificationType;
import cz.sefira.obelisk.generic.SessionManager;
import cz.sefira.obelisk.prefs.UserPreferences;
import cz.sefira.obelisk.util.ZipUtils;
import cz.sefira.obelisk.view.StandaloneDialog;
import cz.sefira.obelisk.view.StandaloneUIController;
import cz.sefira.obelisk.view.core.ControllerCore;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import cz.sefira.obelisk.api.PlatformAPI;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import static cz.sefira.obelisk.api.NotificationType.*;

public class PreferencesController extends ControllerCore implements StandaloneUIController, Initializable {

	private static final Logger logger = LoggerFactory.getLogger(PreferencesController.class.getName());

	private static final int MIN_VALUE_CACHE_DURATION_MINUTES = 0;
	private static final int MAX_VALUE_CACHE_DURATION_MINUTES = 30;

	@FXML
	private GridPane gridPane;

	@FXML
	private Button ok;

	@FXML
	private Button cancel;

	@FXML
	private Button reset;

	@FXML
	private Button export;

	@FXML
	private CheckBox splashscreen;

	@FXML
	private ComboBox<NotificationType> showNotifications;

	@FXML
	private Button minusDuration;

	@FXML
	private TextField durationTextField;

	@FXML
	private Button plusDuration;

	@FXML
	private CheckBox debugMode;

	private Stage primaryStage;

	private PlatformAPI api;

	private UserPreferences userPreferences;

	private BooleanProperty readOnly;

	private ResourceBundle resources;

	private SimpleIntegerProperty duration = new SimpleIntegerProperty(0);

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.resources = resources;
		readOnly = new SimpleBooleanProperty(false);
		ok.disableProperty().bind(readOnly);
		reset.disableProperty().bind(readOnly);

		minusDuration.setOnAction((e) -> decrementDuration());
		minusDuration.addEventFilter(MouseEvent.ANY, new PressedRepeatEventHandler(this::decrementDuration,
				325, 125, TimeUnit.MILLISECONDS));

		plusDuration.setOnAction((e) -> incrementDuration());
		plusDuration.addEventFilter(MouseEvent.ANY, new PressedRepeatEventHandler(this::incrementDuration,
				325, 125, TimeUnit.MILLISECONDS));

		durationTextField.setTextFormatter(new TextFormatter<>(this::filter));
		durationTextField.setOnMouseClicked((e) -> setTextFieldDuration(true));

		showNotifications.getItems().addAll(List.of(OFF, NATIVE, INTEGRATED));

		ok.setOnAction((evt) -> {
			userPreferences.setDebugMode(debugMode.selectedProperty().getValue());
			userPreferences.setShowNotifications(showNotifications.getSelectionModel().getSelectedItem());
			userPreferences.setSplashScreen(splashscreen.selectedProperty().getValue());
			if (duration.getValue() == 0 || !duration.getValue().equals(userPreferences.getCacheDuration())) {
				SessionManager.getManager().destroySecret();
			}
			userPreferences.setCacheDuration(duration.getValue());
			AppConfigurer.applyUserPreferences(userPreferences);
			windowClose(primaryStage);
		});
		cancel.setOnAction((e) -> windowClose(primaryStage));
		reset.setOnAction((e) -> StandaloneDialog.showConfirmResetDialog(primaryStage, api, userPreferences));

		export.setOnAction((e) -> {
			final FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle(resources.getString("preferences.export.save.title"));
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
			String name = "obelisk-sp-client_export-"+sdf.format(new Date())+".zip";
			fileChooser.setInitialFileName(name);
			File f = fileChooser.showSaveDialog(primaryStage);
			if (f != null) {
				logger.info("Exporting configuration: "+f.getAbsolutePath());
				try (OutputStream out = Files.newOutputStream(f.toPath())) {
					File userHome = AppConfig.get().getAppUserHome();
					out.write(ZipUtils.zipDirectory(userHome, name, userPreferences.toString(), null));
				}
				catch (IOException ex) {
					StandaloneDialog.showGenericErrorDialog(ex);
				}
			}
		});
	}

	@Override
	public void init(Stage stage, Object... params) {
		this.primaryStage = stage;
		this.api = (PlatformAPI) params[0];
		this.userPreferences = (UserPreferences) params[1];
		this.readOnly.set((boolean) params[2]);
		this.debugMode.selectedProperty().setValue(userPreferences.isDebugMode());
		this.showNotifications.getSelectionModel().select(userPreferences.getShowNotifications());
		this.splashscreen.selectedProperty().setValue(userPreferences.isSplashScreen());
		this.duration = new SimpleIntegerProperty(userPreferences.getCacheDuration());
		toggleCacheDurationButtons(duration.getValue());
		duration.addListener((observable, oldValue, newValue) -> toggleCacheDurationButtons(newValue));
		setTextFieldDuration(true);
		setLogoBackground(gridPane);
	}

  private TextFormatter.Change filter(TextFormatter.Change change) {
    if (!change.getControlNewText().matches("\\b([0-9]|[12][0-9]|30)\\b")) {
      change.setText("");
    }
    else {
      try {
        duration.set(Integer.parseInt(change.getControlNewText()));
        setTextFieldDuration(false);
      }
      catch (NumberFormatException ignored) {
        // not a number, ignore this input
      }
    }
    return change;
  }

  void setTextFieldDuration(boolean clearText) {
    String minutes = duration.getValue() == 0 || duration.getValue() > 4 ? resources.getString("preferences.minutes.1") :
        duration.getValue() == 1 ? resources.getString("preferences.minute") : resources.getString("preferences.minutes");
    if (clearText) {
      durationTextField.setText("");
    }
    durationTextField.setPromptText(duration.getValue() + " " + minutes);
  }

  private void decrementDuration() {
    if (duration.getValue() > MIN_VALUE_CACHE_DURATION_MINUTES) {
			duration.set(duration.get()-1);
      setTextFieldDuration(true);
    }
  }

  private void incrementDuration() {
    if (duration.getValue() < MAX_VALUE_CACHE_DURATION_MINUTES) {
			duration.set(duration.get()+1);
      setTextFieldDuration(true);
    }
  }

	private void toggleCacheDurationButtons(Number currentValue) {
		minusDuration.setDisable(currentValue.equals(MIN_VALUE_CACHE_DURATION_MINUTES));
		plusDuration.setDisable(currentValue.equals(MAX_VALUE_CACHE_DURATION_MINUTES));
	}

	@Override
	public void close() {

	}
}

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
import cz.sefira.obelisk.flow.StageHelper;
import cz.sefira.obelisk.generic.SessionManager;
import cz.sefira.obelisk.util.ZipUtils;
import cz.sefira.obelisk.view.StandaloneDialog;
import cz.sefira.obelisk.view.core.AbstractUIOperationController;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import cz.sefira.obelisk.api.PlatformAPI;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

public class PreferencesController extends AbstractUIOperationController<Void> implements Initializable {

	private static final Logger logger = LoggerFactory.getLogger(PreferencesController.class.getName());

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
	private CheckBox debugMode;

	@FXML
	private Button minus;

	@FXML
	private TextField durationTextField;

	@FXML
	private Button plus;

	private PlatformAPI api;

	private UserPreferences userPreferences;

	private BooleanProperty readOnly;

	private ResourceBundle resources;

	private int duration = 0;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.resources = resources;
		readOnly = new SimpleBooleanProperty(false);
		ok.disableProperty().bind(readOnly);
		reset.disableProperty().bind(readOnly);

		minus.setOnAction((e) -> decrementDuration());
		minus.addEventFilter(MouseEvent.ANY, new PressedRepeatEventHandler(this::decrementDuration,
				325, 125, TimeUnit.MILLISECONDS));

		plus.setOnAction((e) -> incrementDuration());
		plus.addEventFilter(MouseEvent.ANY, new PressedRepeatEventHandler(this::incrementDuration,
				325, 125, TimeUnit.MILLISECONDS));

		durationTextField.setTextFormatter(new TextFormatter<>(this::filter));
		durationTextField.setOnMouseClicked((e) -> setTextFieldDuration(true));

		ok.setOnAction((evt) -> {
			userPreferences.setDebugMode(debugMode.selectedProperty().getValue());
			userPreferences.setSplashScreen(splashscreen.selectedProperty().getValue());
			if (duration == 0 || !Integer.valueOf(duration).equals(userPreferences.getCacheDuration())) {
				SessionManager.getManager().destroySecret();
			}
			userPreferences.setCacheDuration(duration);
			AppConfigurer.applyUserPreferences(userPreferences);
			signalEnd(null);
		});
		cancel.setOnAction((e) -> signalEnd(null));
		reset.setOnAction((e) -> {
			StandaloneDialog.showConfirmResetDialog(api, userPreferences);
			signalEnd(null);
		});

		export.setOnAction((e) -> {
			final FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle(resources.getString("preferences.export.save.title"));
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
			String name = "obelisk-sp-client_export-"+sdf.format(new Date())+".zip";
			fileChooser.setInitialFileName(name);
			File f = fileChooser.showSaveDialog(getDisplay().getStage(false));
			if (f != null) {
				logger.info("Exporting configuration: "+f.getAbsolutePath());
				try (OutputStream out = Files.newOutputStream(f.toPath())) {
					File userHome = api.getAppConfig().getAppUserHome();
					out.write(ZipUtils.zipDirectory(userHome, name, userPreferences.toString(), null));
				}
				catch (IOException ex) {
					StandaloneDialog.showGenericErrorDialog(ex);
				}
			}
		});
	}

	@Override
	public void init(Object... params) {
		StageHelper.getInstance().setTitle("", "preferences.header");
		this.api = (PlatformAPI) params[0];
		this.userPreferences = (UserPreferences) params[1];
		this.readOnly.set((boolean) params[2]);
		this.debugMode.selectedProperty().setValue(userPreferences.isDebugMode());
		this.splashscreen.selectedProperty().setValue(userPreferences.getSplashScreen());
		this.duration = userPreferences.getCacheDuration();
		setTextFieldDuration(true);
		setLogoBackground(gridPane);
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

  private void decrementDuration() {
    if (duration > 0) {
      duration--;
      setTextFieldDuration(true);
    }
  }

  private void incrementDuration() {
    if (duration < 30) {
      duration++;
      setTextFieldDuration(true);
    }
  }

}

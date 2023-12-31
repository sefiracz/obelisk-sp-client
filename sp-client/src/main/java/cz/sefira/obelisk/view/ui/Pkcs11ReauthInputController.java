/**
 * © SEFIRA spol. s r.o., 2020-2021
 * <p>
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 * <p>
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 * <p>
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package cz.sefira.obelisk.view.ui;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.view.ui.QPINInputController
 *
 * Created: 09.11.2021
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.flow.StageHelper;
import cz.sefira.obelisk.generic.SessionManager;
import cz.sefira.obelisk.prefs.PreferencesFactory;
import cz.sefira.obelisk.prefs.UserPreferences;
import cz.sefira.obelisk.view.core.AbstractUIOperationController;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import org.identityconnectors.common.security.GuardedString;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Pkcs11ReauthInputController extends AbstractUIOperationController<GuardedString> implements Initializable {

  @FXML
  private Region icon;

  @FXML
  private Button ok;

  @FXML
  private Button cancel;

  @FXML
  private Label passwordPrompt;

  @FXML
  private PasswordField password;

  @FXML
  private CheckBox storeInputCheckbox;

  @FXML
  private SplitPane checkboxPane;

  private ResourceBundle resources;

  private AppConfig appConfig;

  private ScheduledExecutorService executorService;

  private Integer cacheDuration;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    EventHandler<ActionEvent> handler = event -> {
      GuardedString reauth = new GuardedString(password.getText().toCharArray());
      UserPreferences prefs = PreferencesFactory.getInstance(appConfig);
      boolean cacheEnabled = prefs.getCacheDuration() != null && prefs.getCacheDuration() > 0;
      if(storeInputCheckbox.selectedProperty().getValue() && cacheEnabled) {
        SessionManager.getManager().setSecret(reauth, prefs.getCacheDuration());
      }
      signalEnd(reauth);
      password.clear();
    };
    ok.setOnAction(handler);
    password.setOnAction(handler);
    cancel.setOnAction(e -> signalUserCancel());
    this.resources = resources;
  }

  @Override
  public void init(Object... params) {
    this.appConfig = (AppConfig) params[0];
    String titleKey = "reauth.title.qpin";
    this.password.setPromptText(resources.getString(titleKey));
    this.passwordPrompt.setText(resources.getString("reauth.dialog.label"));
    executorService = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "ReAuth");
      t.setDaemon(true);
      return t;
    });
    StageHelper.getInstance().setTitle(appConfig.getApplicationName(), titleKey);
    cacheDuration = setCacheCheckbox();
    executorService.scheduleAtFixedRate(this::setCacheCheckbox, 500, UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
    icon.getStyleClass().add("icon-qpin");
    icon.setPrefSize(50, 40);
  }

  private int setCacheCheckbox() {
    UserPreferences prefs = PreferencesFactory.getInstance(appConfig);
    int duration = getCacheDuration(prefs);
    if (cacheDuration == null || cacheDuration != duration) {
      boolean enabled = isCacheEnabled(prefs);
      if (enabled) {
        storeInputCheckbox.selectedProperty().setValue(true);
        storeInputCheckbox.setSelected(true);
      } else {
        storeInputCheckbox.selectedProperty().setValue(false);
        storeInputCheckbox.setSelected(false);
      }
      Tooltip tooltip = getTooltip(duration);
      storeInputCheckbox.disableProperty().setValue(!enabled);
      checkboxPane.setTooltip(tooltip);
      cacheDuration = duration;
    }
    return duration;
  }

  private boolean isCacheEnabled(UserPreferences prefs) {
    return prefs.getCacheDuration() != null && prefs.getCacheDuration() > 0;
  }

  private int getCacheDuration(UserPreferences prefs) {
    return prefs.getCacheDuration() != null ? prefs.getCacheDuration() : 0;
  }

  private Tooltip getTooltip(int cacheDuration) {
    Tooltip tooltip = new Tooltip();
    tooltip.setShowDelay(new Duration(50));
    if (cacheDuration == 0) {
      tooltip.setText(resources.getString("reauth.tooltip.disabled"));
    } else {
      String minutes = cacheDuration > 4 ? resources.getString("preferences.minutes.1") :
          cacheDuration == 1 ? resources.getString("preferences.minute.1") : resources.getString("preferences.minutes");
      tooltip.setText(MessageFormat.format(resources.getString("reauth.tooltip.enabled"), cacheDuration+" "+minutes));
    }
    return tooltip;
  }

  @Override
  public void close() throws IOException {
    if (executorService != null)
      executorService.shutdown();
  }
}
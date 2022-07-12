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

import cz.sefira.obelisk.UserPreferences;
import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.flow.StageHelper;
import cz.sefira.obelisk.generic.SessionManager;
import cz.sefira.obelisk.view.core.AbstractUIOperationController;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.Duration;
import org.identityconnectors.common.security.GuardedString;

import java.net.URL;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class Pkcs11ReauthInputController extends AbstractUIOperationController<GuardedString> implements Initializable {

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

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    EventHandler<ActionEvent> handler = event -> {
      GuardedString reauth = new GuardedString(password.getText().toCharArray());
      UserPreferences prefs = new UserPreferences(appConfig);
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
    this.passwordPrompt.setText(resources.getString("reauth.dialog.label"));

    StageHelper.getInstance().setTitle(appConfig.getApplicationName(), titleKey);

    UserPreferences prefs = new UserPreferences(appConfig);
    boolean cacheEnabled = prefs.getCacheDuration() != null && prefs.getCacheDuration() > 0;
    if (cacheEnabled) {
      storeInputCheckbox.selectedProperty().setValue(true);
      storeInputCheckbox.setSelected(true);
    }
    Integer cacheDuration = prefs.getCacheDuration();
    boolean cacheDisabled = cacheDuration == null || cacheDuration == 0;
    if (cacheDuration == null) {
      cacheDuration = 0;
    }
    storeInputCheckbox.disableProperty().setValue(cacheDisabled);
    String minutes = cacheDuration == 0 || cacheDuration > 4 ? resources.getString("preferences.minutes.universal") :
        cacheDuration == 1 ? resources.getString("preferences.minute") : resources.getString("preferences.minutes");
    Tooltip tooltip = new Tooltip(MessageFormat.format(resources.getString("reauth.tooltip.enabled"),
        prefs.getCacheDuration()+" "+minutes));
    tooltip.setShowDelay(new Duration(50));
    if (cacheDisabled) {
      tooltip.setText(resources.getString("reauth.tooltip.disabled"));
    }
    checkboxPane.setTooltip(tooltip);
  }
}
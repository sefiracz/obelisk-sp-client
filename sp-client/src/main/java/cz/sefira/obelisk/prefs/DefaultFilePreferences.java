/**
 * © SEFIRA spol. s r.o., 2020-2023
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
package cz.sefira.obelisk.prefs;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.prefs.FilePreferences
 *
 * Created: 07.06.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.notification.NotificationType;
import cz.sefira.obelisk.util.annotation.NotNull;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.FileBasedBuilderParameters;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Property file-based preferences
 */
public class DefaultFilePreferences extends FilePreferences {

  private static final Logger logger = LoggerFactory.getLogger(DefaultFilePreferences.class.getName());

  public DefaultFilePreferences(@NotNull AppConfig appConfig, @NotNull String defaultPreferencesFile) {
    try {
      // load default config file
      Path defaultDir = appConfig.getDefaultUserConfigDir();
      if (defaultDir != null) {
        Path defaultConfigFile = defaultDir.resolve(defaultPreferencesFile);
        if (defaultConfigFile.toFile().exists()) {
          // config params
          FileBasedBuilderParameters fileBasedParams = new Parameters().fileBased();
          fileBasedParams.setEncoding("UTF-8");
          fileBasedParams.setFile(defaultConfigFile.toFile());

          // config builder
          FileBasedConfigurationBuilder<PropertiesConfiguration> builder = new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class);
          builder.configure(fileBasedParams);
          builder.setAutoSave(true);

          // config contains all properties read from the file
          config = builder.getConfiguration();

          // get values
          String hiddenDialogIdsList = get(String.class, HIDDEN_DIALOGS, null);
          if (hiddenDialogIdsList != null && !hiddenDialogIdsList.isEmpty()) {
            String[] dialogIds = hiddenDialogIdsList.split(",");
            hiddenDialogIds = new ArrayList<>(Arrays.asList(dialogIds));
          } else {
            hiddenDialogIds = new ArrayList<>();
          }
          splashScreen = get(Boolean.class,SPLASH_SCREEN, true);
          showNotifications = NotificationType.fromType(get(String.class, SHOW_NOTIFICATIONS, null));
          debugMode = get(Boolean.class, DEBUG_MODE, false);
          language = get(String.class, LANGUAGE, null);
          cacheDuration = normalizeCacheDuration(get(Integer.class, CACHE_DURATION, 0));
          // proxy setup
          proxyReadOnly = get(Boolean.class, FLAG_PROXY_READONLY, false);
          useSystemProxy = get(Boolean.class, USE_SYSTEM_PROXY, false);
          proxyServer = get(String.class, PROXY_SERVER, null);
          proxyPort = get(Integer.class, PROXY_PORT, null);
          proxyUseHttps = get(Boolean.class, PROXY_USE_HTTPS, false);
          proxyAuthentication = get(Boolean.class, PROXY_AUTHENTICATION, false);
          proxyUsername = get(String.class, PROXY_USERNAME, null);
          proxyPassword = get(String.class, PROXY_PASSWORD, null);
        }
      }
    } catch (Exception e) {
      logger.error("Failed to load configuration: "+e.getMessage(), e);
    }
  }

  @Override
  public void setLanguage(String language) {
    // read-only
  }

  @Override
  public void addHiddenDialogId(String hiddenDialogIds) {
    // read-only
  }

  @Override
  public void setSplashScreen(Boolean splashScreen) {
    // read-only
  }

  @Override
  public void setShowNotifications(NotificationType showNotifications) {
    // read-only
  }

  @Override
  public void setCacheDuration(Integer cacheDuration) {
    // read-only
  }

  @Override
  public void setDebugMode(Boolean debugMode) {
    // read-only
  }

  @Override
  public void setHiddenDialogIds(List<String> hiddenDialogIds) {
    // read-only
  }

  @Override
  public void setUseSystemProxy(Boolean useSystemProxy) {
    // read-only
  }

  @Override
  public void setProxyServer(String proxyServer) {
    // read-only
  }

  @Override
  public void setProxyPort(Integer proxyPort) {
    // read-only
  }

  @Override
  public void setProxyUseHttps(Boolean proxyUseHttps) {
    // read-only
  }

  @Override
  public void setProxyAuthentication(Boolean proxyAuthentication) {
    // read-only
  }

  @Override
  public void setProxyUsername(String proxyUsername) {
    // read-only
  }

  @Override
  public void setProxyPassword(String proxyPassword) {
    // read-only
  }

  @Override
  public void clear() {
    // read-only
  }

}

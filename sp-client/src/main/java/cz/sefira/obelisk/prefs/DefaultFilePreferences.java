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
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.FileBasedBuilderParameters;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Property file-based preferences
 */
public class DefaultFilePreferences extends FilePreferences {

  private static final Logger logger = LoggerFactory.getLogger(DefaultFilePreferences.class.getName());

  public DefaultFilePreferences(AppConfig appConfig) {
    try {
      // load default config file
      Path defaultDir = appConfig.getDefaultUserConfigDir();
      if (defaultDir != null) {
        Path defaultConfigFile = defaultDir.resolve("default-user-preferences.properties");
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
  public void clear() {
    // read-only
  }

}

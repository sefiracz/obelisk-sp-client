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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Property file-based preferences
 */
public class FilePreferences extends UserPreferences {

  private static final Logger logger = LoggerFactory.getLogger(FilePreferences.class.getName());

  private PropertiesConfiguration config;

  public FilePreferences(AppConfig appConfig) {
    try {
      Path configFile = appConfig.getAppUserHome().toPath().resolve("user-preferences.properties");
      if (!configFile.toFile().exists()) {
        try {
          Files.createFile(configFile);
          try {
            InputStream configStream = FilePreferences.class.getResourceAsStream("/user-preferences.properties");
            if (configStream != null) {
              Files.copy(configStream, configFile, REPLACE_EXISTING);
              logger.info("New user-preferences.properties file created from template");
            }
          } catch (Exception e) {
            logger.error("Unable to create config from template: "+e.getMessage(), e);
          }
        } catch (IOException e) {
          logger.error("Failed to create configuration file: "+e.getMessage(), e);
          throw new RuntimeException(e);
        }
      }

      // config params
      FileBasedBuilderParameters fileBasedParams = new Parameters().fileBased();
      fileBasedParams.setEncoding("UTF-8");
      fileBasedParams.setFile(configFile.toFile());

      // config builder
      FileBasedConfigurationBuilder<PropertiesConfiguration> builder = new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class);
      builder.configure(fileBasedParams);
      builder.setAutoSave(true);

      // config contains all properties read from the file
      config = builder.getConfiguration();
      hiddenDialogIds = config.getString(HIDDEN_DIALOGS, null);

      final String splashScreenValue = config.getString(SPLASH_SCREEN, "true");
      splashScreen = splashScreenValue != null ? Boolean.parseBoolean(splashScreenValue) : null;

      final String showNotificationsType = config.getString(SHOW_NOTIFICATIONS, null);
      showNotifications = NotificationType.fromType(showNotificationsType);

      final String debugModeValue = config.getString(DEBUG_MODE, "false");
      debugMode = debugModeValue != null ? Boolean.parseBoolean(debugModeValue) : null;

      language = config.getString(LANGUAGE, null);

      try {
        final String cacheDurationValue = config.getString(CACHE_DURATION, "0");
        cacheDuration = normalizeCacheDuration(Integer.parseInt(cacheDurationValue));
      } catch (NumberFormatException e) {
        cacheDuration = 0;
      }

    } catch (Exception e) {
      logger.error("Failed to load configuration: "+e.getMessage(), e);
    }
  }

  public void setLanguage(String language) {
    config.setProperty(LANGUAGE, language);
    this.language = language;
  }

  public void setDebugMode(Boolean debugMode) {
    if(Boolean.TRUE.equals(debugMode)) {
      config.setProperty(DEBUG_MODE, "true");
    } else {
      config.setProperty(DEBUG_MODE, "false");
    }
    this.debugMode = debugMode;
  }

  public void addHiddenDialogId(String dialogId) {
    List<String> list = getHiddenDialogIds();
    list.add(dialogId);
    this.hiddenDialogIds = String.join(",", list);
    config.setProperty(HIDDEN_DIALOGS, hiddenDialogIds);
  }

  public void setSplashScreen(Boolean splashScreen) {
    if(Boolean.TRUE.equals(splashScreen)) {
      config.setProperty(SPLASH_SCREEN, "true");
    } else {
      config.setProperty(SPLASH_SCREEN, "false");
    }
    this.splashScreen = splashScreen;
  }

  public void setShowNotifications(NotificationType showNotifications) {
    config.setProperty(SHOW_NOTIFICATIONS, Objects.requireNonNullElse(showNotifications, NotificationType.OFF).getType());
    this.showNotifications = showNotifications;
  }

  public void setCacheDuration(Integer cacheDuration) {
    cacheDuration = normalizeCacheDuration(cacheDuration);
    config.setProperty(CACHE_DURATION, String.valueOf(cacheDuration));
    this.cacheDuration = cacheDuration;
  }

  @Override
  public void clear() {
    super.clear();
    this.config.clear();
  }


}

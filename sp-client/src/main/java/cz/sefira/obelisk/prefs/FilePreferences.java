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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Property file-based preferences
 */
public class FilePreferences extends UserPreferences {

  private static final Logger logger = LoggerFactory.getLogger(FilePreferences.class.getName());

  protected PropertiesConfiguration config;

  protected FilePreferences() {}

  public FilePreferences(AppConfig appConfig) {
    try {
      Path configFile = appConfig.getAppUserHome().toPath().resolve("user-preferences.properties");
      if (!configFile.toFile().exists()) {
        try {
          Files.createFile(configFile);
          try {
            InputStream configStream = FilePreferences.class.getResourceAsStream("/prefs/user-preferences-template.properties");
            if (configStream != null) {
              Files.copy(configStream, configFile, REPLACE_EXISTING);
              logger.info("New user-preferences.properties file created from template");
            }
          } catch (Exception e) {
            logger.error("Unable to create config from template: "+e.getMessage(), e);
          }
        } catch (IOException e) {
          logger.error("Failed to create configuration file: "+e.getMessage(), e);
        }
      }
      // load default values
      DefaultFilePreferences defaultUserConfig = new DefaultFilePreferences(appConfig, "default-user-preferences.properties");
      DefaultFilePreferences defaultProxyConfig = new DefaultFilePreferences(appConfig, "default-proxy-preferences.properties");

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

      // get values
      // user setup
      String hiddenDialogIdsList = get(String.class, HIDDEN_DIALOGS, null);
      if (hiddenDialogIdsList != null && !hiddenDialogIdsList.isEmpty()) {
        String[] dialogIds = hiddenDialogIdsList.split(",");
        hiddenDialogIds = new ArrayList<>(Arrays.asList(dialogIds));
      } else {
        hiddenDialogIds = defaultUserConfig.getHiddenDialogIds();
      }
      splashScreen = get(Boolean.class, SPLASH_SCREEN, defaultUserConfig.isSplashScreen());
      String notificationType = get(String.class, SHOW_NOTIFICATIONS, null);
      showNotifications = (notificationType != null && !notificationType.isEmpty()) ? NotificationType.fromType(notificationType) : defaultUserConfig.getShowNotifications();
      debugMode = get(Boolean.class, DEBUG_MODE, defaultUserConfig.isDebugMode());
      language = get(String.class, LANGUAGE, defaultUserConfig.getLanguage());
      cacheDuration = normalizeCacheDuration(get(Integer.class, CACHE_DURATION, defaultUserConfig.getCacheDuration()));
      // proxy setup
      proxyReadOnly = defaultProxyConfig.isProxyReadOnly();
      if (proxyReadOnly) {
        useSystemProxy = defaultProxyConfig.isUseSystemProxy();
        proxyServer = defaultProxyConfig.getProxyServer();
        proxyPort = defaultProxyConfig.getProxyPort();
        proxyUseHttps = defaultProxyConfig.isProxyUseHttps();
        proxyAuthentication = defaultProxyConfig.isProxyAuthentication();
        proxyUsername = defaultProxyConfig.getProxyUsername();
        proxyPassword = defaultProxyConfig.getProxyPassword();
      } else {
        useSystemProxy = get(Boolean.class, USE_SYSTEM_PROXY, defaultProxyConfig.isUseSystemProxy());
        proxyServer = get(String.class, PROXY_SERVER, defaultProxyConfig.getProxyServer());
        proxyPort = get(Integer.class, PROXY_PORT, defaultProxyConfig.getProxyPort());
        proxyUseHttps = get(Boolean.class, PROXY_USE_HTTPS, defaultProxyConfig.isProxyUseHttps());
        proxyAuthentication = get(Boolean.class, PROXY_AUTHENTICATION, defaultProxyConfig.isProxyAuthentication());
        proxyUsername = get(String.class, PROXY_USERNAME, defaultProxyConfig.getProxyUsername());
        proxyPassword = get(String.class, PROXY_PASSWORD, defaultProxyConfig.getProxyPassword());
      }
    } catch (Exception e) {
      logger.error("Failed to load configuration: "+e.getMessage(), e);
    }
  }

  protected <T> T get(final Class<T> cls, final String key, final T defaultValue) {
    try {
      return config.get(cls, key, defaultValue);
    } catch (Exception e) {
      return defaultValue;
    }
  }

  public void setLanguage(String language) {
    config.setProperty(LANGUAGE, Objects.requireNonNullElse(language, ""));
    this.language = language;
  }

  public void setDebugMode(Boolean debugMode) {
    if (null == debugMode) {
      config.setProperty(DEBUG_MODE, "");
    } else if(Boolean.TRUE.equals(debugMode)) {
      config.setProperty(DEBUG_MODE, "true");
    } else {
      config.setProperty(DEBUG_MODE, "false");
    }
    this.debugMode = debugMode;
  }

  public void setHiddenDialogIds(List<String> hiddenDialogIds){
    if (null == hiddenDialogIds) {
      config.setProperty(HIDDEN_DIALOGS, "");
    } else {
      config.setProperty(HIDDEN_DIALOGS, String.join(",", hiddenDialogIds));
    }
    this.hiddenDialogIds = hiddenDialogIds;
  }

  public void addHiddenDialogId(String dialogId) {
    if (hiddenDialogIds == null) {
      hiddenDialogIds = new ArrayList<>();
    }
    if (!hiddenDialogIds.contains(dialogId)) {
      hiddenDialogIds.add(dialogId);
    }
    config.setProperty(HIDDEN_DIALOGS, String.join(",", hiddenDialogIds));
  }

  public void setSplashScreen(Boolean splashScreen) {
    if (null == splashScreen) {
      config.setProperty(SPLASH_SCREEN, "");
    } else if(Boolean.TRUE.equals(splashScreen)) {
      config.setProperty(SPLASH_SCREEN, "true");
    } else {
      config.setProperty(SPLASH_SCREEN, "false");
    }
    this.splashScreen = splashScreen;
  }

  public void setShowNotifications(NotificationType showNotifications) {
    if (null == showNotifications) {
      config.setProperty(SHOW_NOTIFICATIONS, "");
    } else {
      config.setProperty(SHOW_NOTIFICATIONS, showNotifications.getType());
    }
    this.showNotifications = showNotifications;
  }

  public void setCacheDuration(Integer cacheDuration) {
    if (null == cacheDuration) {
      config.setProperty(CACHE_DURATION, "");
    } else {
      cacheDuration = normalizeCacheDuration(cacheDuration);
      config.setProperty(CACHE_DURATION, String.valueOf(cacheDuration));
    }
    this.cacheDuration = cacheDuration;
  }

  public void setUseSystemProxy(Boolean useSystemProxy) {
    if (null == useSystemProxy) {
      config.setProperty(USE_SYSTEM_PROXY, "");
    } else if(Boolean.TRUE.equals(useSystemProxy)) {
      config.setProperty(USE_SYSTEM_PROXY, "true");
    } else {
      config.setProperty(USE_SYSTEM_PROXY, "false");
    }
    this.useSystemProxy = useSystemProxy;
  }

  public void setProxyServer(String proxyServer) {
    config.setProperty(PROXY_SERVER, Objects.requireNonNullElse(proxyServer, ""));
    this.proxyServer = proxyServer;
  }

  public void setProxyPort(Integer proxyPort) {
    config.setProperty(PROXY_PORT, Objects.requireNonNullElse(proxyPort, ""));
    this.proxyPort = proxyPort;
  }

  public void setProxyUseHttps(Boolean proxyUseHttps) {
    if (null == proxyUseHttps) {
      config.setProperty(PROXY_USE_HTTPS, "");
    } else if(Boolean.TRUE.equals(proxyUseHttps)) {
      config.setProperty(PROXY_USE_HTTPS, "true");
    } else {
      config.setProperty(PROXY_USE_HTTPS, "false");
    }
    this.proxyUseHttps = proxyUseHttps;
  }

  public void setProxyAuthentication(Boolean proxyAuthentication) {
    if (null == proxyAuthentication) {
      config.setProperty(PROXY_AUTHENTICATION, "");
    } else if(Boolean.TRUE.equals(proxyAuthentication)) {
      config.setProperty(PROXY_AUTHENTICATION, "true");
    } else {
      config.setProperty(PROXY_AUTHENTICATION, "false");
    }
    this.proxyAuthentication = proxyAuthentication;
  }

  public void setProxyUsername(String proxyUsername) {
    config.setProperty(PROXY_USERNAME, Objects.requireNonNullElse(proxyUsername, ""));
    this.proxyUsername = proxyUsername;
  }

  public void setProxyPassword(String proxyPassword) {
    config.setProperty(PROXY_PASSWORD, Objects.requireNonNullElse(proxyPassword, ""));
    this.proxyPassword = proxyPassword;
  }

  @Override
  public void clear() {
    setDebugMode(null);
    setHiddenDialogIds(null);
    setSplashScreen(null);
    setShowNotifications(null);
    setLanguage(null);
    setCacheDuration(null);

    // proxy setup
    if (!proxyReadOnly) {
      setUseSystemProxy(null);
      setProxyServer(null);
      setProxyPort(null);
      setProxyUseHttps(null);
      setProxyAuthentication(null);
      setProxyUsername(null);
      setProxyPassword(null);
    }
  }


}

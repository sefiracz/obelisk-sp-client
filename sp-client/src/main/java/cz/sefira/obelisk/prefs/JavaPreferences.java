package cz.sefira.obelisk.prefs;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.prefs.JavaPreferences
 *
 * Created: 07.06.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.notification.NotificationType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Java-based preferences
 */
public class JavaPreferences extends UserPreferences {

  private final Preferences prefs;

  public JavaPreferences(AppConfig appConfig) {
    prefs = Preferences.userRoot().node(appConfig.getApplicationPathName().toLowerCase());

    final String hiddenDialogIdsValue = prefs.get(HIDDEN_DIALOGS, null);
    if (hiddenDialogIdsValue != null && !hiddenDialogIdsValue.isEmpty()) {
      String[] dialogIds = hiddenDialogIdsValue.split(",");
      hiddenDialogIds = new ArrayList<>(Arrays.asList(dialogIds));
    } else {
      hiddenDialogIds = new ArrayList<>();
    }

    final String splashScreenValue = prefs.get(SPLASH_SCREEN, null);
    splashScreen = splashScreenValue != null ? Boolean.parseBoolean(splashScreenValue) : null;

    final String showNotificationsType = prefs.get(SHOW_NOTIFICATIONS, null);
    showNotifications = NotificationType.fromType(showNotificationsType);

    final String debugModeValue = prefs.get(DEBUG_MODE, null);
    debugMode = debugModeValue != null ? Boolean.parseBoolean(debugModeValue) : null;

    language = prefs.get(LANGUAGE, null);

    final String useSystemProxyStr = prefs.get(USE_SYSTEM_PROXY, null);
    useSystemProxy = (useSystemProxyStr != null) ? Boolean.valueOf(useSystemProxyStr) : null;

    proxyServer = prefs.get(PROXY_SERVER, null);

    final String proxyPortStr = prefs.get(PROXY_PORT, null);
    proxyPort = (proxyPortStr != null) ? Integer.valueOf(proxyPortStr) : null;

    final String proxyHttps = prefs.get(PROXY_USE_HTTPS, null);
    proxyUseHttps = (proxyHttps != null) ? Boolean.valueOf(proxyHttps) : null;

    final String proxyAuthenticationStr = prefs.get(PROXY_AUTHENTICATION, null);
    proxyAuthentication = (proxyAuthenticationStr != null) ? Boolean.valueOf(proxyAuthenticationStr) : null;

    proxyUsername = prefs.get(PROXY_USERNAME, null);
    proxyPassword = prefs.get(PROXY_PASSWORD, null);
    try {
      final String cacheDurationValue = prefs.get(CACHE_DURATION, "0");
      cacheDuration = normalizeCacheDuration(Integer.parseInt(cacheDurationValue));
    } catch (NumberFormatException e) {
      cacheDuration = 0;
    }
  }

  public void setLanguage(String language) {
    if (language != null) {
      prefs.put(LANGUAGE, language);
    } else {
      prefs.remove(LANGUAGE);
    }
    this.language = language;
  }

  public void setDebugMode(Boolean debugMode) {
    if(Boolean.TRUE.equals(debugMode)) {
      prefs.put(DEBUG_MODE, "true");
    } else {
      prefs.put(DEBUG_MODE, "false");
    }
    this.debugMode = debugMode;
  }

  public void addHiddenDialogId(String dialogId) {
    if (hiddenDialogIds == null) {
      hiddenDialogIds = new ArrayList<>();
    }
    if (!hiddenDialogIds.contains(dialogId)) {
      hiddenDialogIds.add(dialogId);
    }
    prefs.put(HIDDEN_DIALOGS, String.join(",", hiddenDialogIds));
  }

  public void setHiddenDialogIds(List<String> hiddenDialogs){
    this.hiddenDialogIds = hiddenDialogs;
  }

  public void setSplashScreen(Boolean splashScreen) {
    if(Boolean.TRUE.equals(splashScreen)) {
      prefs.put(SPLASH_SCREEN, "true");
    } else {
      prefs.put(SPLASH_SCREEN, "false");
    }
    this.splashScreen = splashScreen;
  }

  public void setShowNotifications(NotificationType showNotifications) {
    prefs.put(SHOW_NOTIFICATIONS, Objects.requireNonNullElse(showNotifications, NotificationType.OFF).getType());
    this.showNotifications = showNotifications;
  }

  public void setCacheDuration(Integer cacheDuration) {
    cacheDuration = normalizeCacheDuration(cacheDuration);
    prefs.put(CACHE_DURATION, String.valueOf(cacheDuration));
    this.cacheDuration = cacheDuration;
  }


  @Override
  public void setUseSystemProxy(Boolean useSystemProxy) {
    if(Boolean.TRUE.equals(useSystemProxy)) {
      prefs.put(USE_SYSTEM_PROXY, "true");
    } else {
      prefs.put(USE_SYSTEM_PROXY, "false");
    }
    this.useSystemProxy = useSystemProxy;
  }

  @Override
  public void setProxyServer(String proxyServer) {
    if (language != null) {
      prefs.put(LANGUAGE, language);
    } else {
      prefs.remove(LANGUAGE);
    }
    this.language = language;
  }

  @Override
  public void setProxyPort(Integer proxyPort) {
    if (proxyPort != null) {
      prefs.put(PROXY_PORT, String.valueOf(proxyPort));
    } else {
      prefs.remove(PROXY_PORT);
    }
    this.proxyPort = proxyPort;
  }

  @Override
  public void setProxyUseHttps(Boolean proxyUseHttps) {
    if (proxyUseHttps) {
      prefs.put(PROXY_USE_HTTPS, "true");
    } else {
      prefs.put(PROXY_USE_HTTPS, "false");
    }
    this.proxyUseHttps = proxyUseHttps;
  }

  @Override
  public void setProxyAuthentication(Boolean proxyAuthentication) {
    if (proxyAuthentication) {
      prefs.put(PROXY_AUTHENTICATION, "true");
    } else {
      prefs.put(PROXY_AUTHENTICATION, "false");
    }
    this.proxyAuthentication = proxyAuthentication;
  }

  @Override
  public void setProxyUsername(String proxyUsername) {
    if (proxyUsername != null) {
      prefs.put(PROXY_USERNAME, proxyUsername);
    } else {
      prefs.remove(PROXY_USERNAME);
    }
    this.proxyUsername = proxyUsername;
  }

  @Override
  public void setProxyPassword(String proxyPassword) {
    if (proxyPassword != null) {
      prefs.put(PROXY_PASSWORD, proxyPassword);
    } else {
      prefs.remove(PROXY_PASSWORD);
    }
    this.proxyPassword = proxyPassword;
  }

  @Override
  public void clear() {
    try {
      this.prefs.clear();
      language = null;
      hiddenDialogIds = new ArrayList<>();
      splashScreen = true;
      showNotifications = NotificationType.getDefault();
      cacheDuration = 0;
      debugMode = false;

      // proxy setup
      useSystemProxy = true;
      proxyServer = null;
      proxyPort = null;
      proxyUseHttps = false;
      proxyAuthentication = false;
      proxyUsername = null;
      proxyPassword = null;
    } catch (BackingStoreException e) {
      throw new IllegalStateException(e);
    }
  }
}

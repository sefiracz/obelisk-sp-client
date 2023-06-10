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

    hiddenDialogIds = prefs.get(HIDDEN_DIALOGS, null);

    final String splashScreenValue = prefs.get(SPLASH_SCREEN, null);
    splashScreen = splashScreenValue != null ? Boolean.parseBoolean(splashScreenValue) : null;

    final String showNotificationsType = prefs.get(SHOW_NOTIFICATIONS, null);
    showNotifications = NotificationType.fromType(showNotificationsType);

    final String debugModeValue = prefs.get(DEBUG_MODE, null);
    debugMode = debugModeValue != null ? Boolean.parseBoolean(debugModeValue) : null;

    language = prefs.get(LANGUAGE, null);

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
    if (debugMode) {
      prefs.put(DEBUG_MODE, "true");
    } else {
      prefs.put(DEBUG_MODE, "false");
    }
    this.debugMode = debugMode;
  }

  public void addHiddenDialogId(String dialogId) {
    List<String> list = getHiddenDialogIds();
    list.add(dialogId);
    this.hiddenDialogIds = String.join(",", list);
    prefs.put(HIDDEN_DIALOGS, hiddenDialogIds);
  }

  public void setSplashScreen(Boolean splashScreen) {
    if (splashScreen) {
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
  public void clear() {
    super.clear();
    try {
      this.prefs.clear();
    } catch (BackingStoreException e) {
      throw new IllegalStateException(e);
    }
  }
}

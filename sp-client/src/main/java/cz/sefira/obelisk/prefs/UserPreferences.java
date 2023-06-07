package cz.sefira.obelisk.prefs;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.prefs.UserPreferences
 *
 * Created: 07.06.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.NotificationType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Abstract user preferences
 */
public abstract class UserPreferences {

  protected static final String LANGUAGE = "cz.sefira.obelisk.signingportal.language";
  protected static final String SPLASH_SCREEN = "cz.sefira.obelisk.signingportal.splashscreen";
  protected static final String SHOW_NOTIFICATIONS = "cz.sefira.obelisk.signingportal.shownotifications";
  protected static final String DEBUG_MODE = "cz.sefira.obelisk.signingportal.debugmode";
  protected static final String HIDDEN_DIALOGS = "cz.sefira.obelisk.signingportal.hiddendialogs";
  protected static final String CACHE_DURATION = "cz.sefira.obelisk.signingportal.cacheduration";


  protected String language;
  protected String hiddenDialogIds;
  protected Boolean splashScreen;
  protected NotificationType showNotifications;
  protected Integer cacheDuration;
  protected Boolean debugMode;

  public void setLanguage(String language) {
    this.language = language;
  }

  public void addHiddenDialogId(String hiddenDialogIds) {
    this.hiddenDialogIds = hiddenDialogIds;
  }

  public void setSplashScreen(Boolean splashScreen) {
    this.splashScreen = splashScreen;
  }

  public void setShowNotifications(NotificationType showNotifications) {
    this.showNotifications = showNotifications;
  }

  public void setCacheDuration(Integer cacheDuration) {
    this.cacheDuration = cacheDuration;
  }

  public void setDebugMode(Boolean debugMode) {
    this.debugMode = debugMode;
  }

  public List<String> getHiddenDialogIds() {
    if(hiddenDialogIds != null) {
      String[] dialogIds = hiddenDialogIds.split(",");
      return new ArrayList<>(Arrays.asList(dialogIds));
    } else {
      return new ArrayList<>();
    }
  }

  public String getLanguage() {
    return language;
  }

  public Boolean isDebugMode() {
    return debugMode != null ? debugMode : false; // by default is OFF
  }

  public Boolean isSplashScreen() {
    return splashScreen != null ? splashScreen : true; // by default is ON
  }

  public NotificationType getShowNotifications() {
    return showNotifications != null ? showNotifications : NotificationType.getDefault(); // default is system dependant
  }

  public Integer getCacheDuration() {
    return cacheDuration;
  }

  protected int normalizeCacheDuration(Integer cacheDuration) {
    if (cacheDuration == null || cacheDuration < 0) {
      cacheDuration = 0;
    } else if (cacheDuration > 30) {
      cacheDuration = 30;
    }
    return cacheDuration;
  }

  public void clear() {
    // set default
    debugMode = null;
    hiddenDialogIds = null;
    splashScreen = null;
    showNotifications = NotificationType.getDefault();
    language = null;
    cacheDuration = 0;
  }

  @Override
  public String toString() {
    return "hiddenDialogIds=" + hiddenDialogIds + "\n" +
        "showNotifications=" + showNotifications.getType() + "\n" +
        "splashScreen=" + splashScreen + "\n" +
        "debugMode=" + debugMode + "\n" +
        "cacheDuration=" + cacheDuration + "\n" +
        "language="+language;
  }

}

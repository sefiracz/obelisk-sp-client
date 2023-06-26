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

import cz.sefira.obelisk.api.notification.NotificationType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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


  protected String language = null;
  protected List<String> hiddenDialogIds = new ArrayList<>();
  protected Boolean splashScreen = true;
  protected NotificationType showNotifications = NotificationType.getDefault();
  protected Integer cacheDuration = 0;
  protected Boolean debugMode = false;

  public abstract void setLanguage(String language);

  public abstract void addHiddenDialogId(String hiddenDialogIds);

  public abstract void setHiddenDialogIds(List<String> hiddenDialogs);

  public abstract void setSplashScreen(Boolean splashScreen);

  public abstract void setShowNotifications(NotificationType showNotifications);

  public abstract void setCacheDuration(Integer cacheDuration);

  public abstract void setDebugMode(Boolean debugMode);

  public List<String> getHiddenDialogIds() {
    return Objects.requireNonNullElseGet(hiddenDialogIds, ArrayList::new);
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
    return cacheDuration != null ? cacheDuration : 0;
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
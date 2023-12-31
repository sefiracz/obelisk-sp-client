/**
 * © Nowina Solutions, 2015-2015
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package cz.sefira.obelisk;

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.notification.NotificationType;

import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

class UserPreferences {

  private static final String LANGUAGE = "cz.sefira.obelisk.signingportal.language";
  private static final String SPLASH_SCREEN = "cz.sefira.obelisk.signingportal.splashscreen";
  private static final String SHOW_NOTIFICATIONS = "cz.sefira.obelisk.signingportal.shownotifications";
  private static final String DEBUG_MODE = "cz.sefira.obelisk.signingportal.debugmode";
  private static final String HIDDEN_DIALOGS = "cz.sefira.obelisk.signingportal.hiddendialogs";
  private static final String CACHE_DURATION = "cz.sefira.obelisk.signingportal.cacheduration";

  private final Preferences prefs;

  private String language;
  private String hiddenDialogIds;
  private Boolean splashScreen;
  private NotificationType showNotifications;
  private Integer cacheDuration;
  private Boolean debugMode;

  public UserPreferences(final AppConfig appConfig) {

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
      cacheDuration = Integer.parseInt(cacheDurationValue);
      if (cacheDuration < 0) {
        cacheDuration = 0;
      } else if (cacheDuration > 30) {
        cacheDuration = 30;
      }
    } catch (NumberFormatException e) {
      cacheDuration = 0;
    }
  }

  public void setLanguage(String language) {
    if(language != null) {
      prefs.put(LANGUAGE, language);
    } else {
      prefs.remove(LANGUAGE);
    }
    this.language = language;
  }

  public void setDebugMode(Boolean debugMode) {
    if(debugMode) {
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
    if(splashScreen) {
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
    if(cacheDuration != null) {
      prefs.put(CACHE_DURATION, String.valueOf(cacheDuration));
    } else {
      prefs.remove(CACHE_DURATION);
    }
    this.cacheDuration = cacheDuration == null ? 0 : cacheDuration;
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

  public void clear() {
    try {
      this.prefs.clear();
    } catch (BackingStoreException e) {
      throw new IllegalStateException(e);
    }

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

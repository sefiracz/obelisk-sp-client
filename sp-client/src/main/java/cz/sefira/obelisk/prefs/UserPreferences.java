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

  /////// USER SETUP ///////
  protected static final String LANGUAGE = "cz.sefira.obelisk.signingportal.language";
  protected static final String SPLASH_SCREEN = "cz.sefira.obelisk.signingportal.splashscreen";
  protected static final String SHOW_NOTIFICATIONS = "cz.sefira.obelisk.signingportal.shownotifications";
  protected static final String DEBUG_MODE = "cz.sefira.obelisk.signingportal.debugmode";
  protected static final String HIDDEN_DIALOGS = "cz.sefira.obelisk.signingportal.hiddendialogs";
  protected static final String CACHE_DURATION = "cz.sefira.obelisk.signingportal.cacheduration";

  /////// PROXY SETUP ///////
  protected static final String FLAG_PROXY_READONLY = "cz.sefira.obelisk.signingportal.proxyreadonly";
  protected static final String USE_SYSTEM_PROXY = "cz.sefira.obelisk.signingportal.usesystemproxy";
  protected static final String PROXY_SERVER = "cz.sefira.obelisk.signingportal.proxyserver";
  protected static final String PROXY_PORT = "cz.sefira.obelisk.signingportal.proxyport";
  protected static final String PROXY_USE_HTTPS = "cz.sefira.obelisk.signingportal.proxyhttps";
  protected static final String PROXY_AUTHENTICATION = "cz.sefira.obelisk.signingportal.proxyauthentication";
  protected static final String PROXY_USERNAME = "cz.sefira.obelisk.signingportal.proxyusername";
  protected static final String PROXY_PASSWORD = "cz.sefira.obelisk.signingportal.proxypassword";

  protected String language = null;
  protected List<String> hiddenDialogIds = new ArrayList<>();
  protected Boolean splashScreen = true;
  protected NotificationType showNotifications = NotificationType.getDefault();
  protected Integer cacheDuration = 0;
  protected Boolean debugMode = false;

  // proxy setup
  protected Boolean proxyReadOnly = false;
  protected Boolean useSystemProxy = true;
  protected String proxyServer = null;
  protected Integer proxyPort = null;
  protected Boolean proxyUseHttps = false;
  protected Boolean proxyAuthentication = false;
  protected String proxyUsername = null;
  protected String proxyPassword = null;

  public abstract void setLanguage(String language);

  public abstract void addHiddenDialogId(String hiddenDialogIds);

  public abstract void setHiddenDialogIds(List<String> hiddenDialogs);

  public abstract void setSplashScreen(Boolean splashScreen);

  public abstract void setShowNotifications(NotificationType showNotifications);

  public abstract void setCacheDuration(Integer cacheDuration);

  public abstract void setDebugMode(Boolean debugMode);

  public abstract void setUseSystemProxy(Boolean useSystemProxy);

  public abstract void setProxyServer(String proxyServer);

  public abstract void setProxyPort(Integer proxyPort);

  public abstract void setProxyUseHttps(Boolean proxyUseHttps);

  public abstract void setProxyAuthentication(Boolean proxyAuthentication);

  public abstract void setProxyUsername(String proxyUsername);

  public abstract void setProxyPassword(String proxyPassword);

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

  public Boolean isProxyReadOnly() {
    return proxyReadOnly != null ? proxyReadOnly : false; // by default is OFF
  }

  public Boolean isUseSystemProxy() {
    return useSystemProxy != null ? useSystemProxy : false; // by default is OFF
  }

  public String getProxyServer() {
    return proxyServer;
  }

  public Integer getProxyPort() {
    return proxyPort;
  }

  public Boolean isProxyUseHttps() {
    return proxyUseHttps != null ? proxyUseHttps : false; // by default is OFF
  }

  public Boolean isProxyAuthentication() {
    return proxyAuthentication != null ? proxyAuthentication : false; // by default is OFF
  }

  public String getProxyUsername() {
    return proxyUsername;
  }

  public String getProxyPassword() {
    return proxyPassword;
  }

  public abstract void clear();

  @Override
  public String toString() {
    return "hiddenDialogIds=" + hiddenDialogIds + "\n" +
        "showNotifications=" + showNotifications.getType() + "\n" +
        "splashScreen=" + splashScreen + "\n" +
        "debugMode=" + debugMode + "\n" +
        "cacheDuration=" + cacheDuration + "\n" +
        "language=" + language + "\n" +
        "proxyReadOnly=" + proxyReadOnly + "\n" +
        "useSystemProxy=" + useSystemProxy + "\n" +
        "proxyServer=" + proxyServer + "\n" +
        "proxyPort=" + proxyPort + "\n" +
        "proxyUseHttps=" + proxyUseHttps + "\n" +
        "proxyAuthentication=" + proxyAuthentication + "\n" +
        "proxyUsername=" + proxyUsername + "\n" +
        "proxyPassword=[REDACTED]";
  }
}

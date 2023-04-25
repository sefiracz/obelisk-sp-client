/**
 * © Nowina Solutions, 2015-2015
 * © SEFIRA spol. s r.o., 2020-2021
 * <p>
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 * <p>
 * http://ec.europa.eu/idabc/eupl5
 * <p>
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package cz.sefira.obelisk.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;

/**
 * Configuration of the App Platform
 *
 * @author David Naramski
 */
public class AppConfig {

  private static final String APPLICATION_NAME = "application_name";
  private static final String APPLICATION_PATH_NAME = "application_path_name";
  private static final String DEBUG = "debug";

  private static final String TICKET_URL = "ticket_url";
  private static final String ENABLE_INCIDENT_REPORT = "enable_incident_report";
  private static final String ACTION_TOKEN_ENDPOINT = "action_token_endpoint";
  private static final String TOKEN_ENDPOINT = "token_endpoint";
  private static final String WINDOWS_INSTALLED_PATH = "windows_installed_path";

  private static final String ENABLE_SYSTRAY_MENU = "enable_systray_menu";
  private static final String USER_PREFERENCES_EDITABLE = "user_preferences_editable";

  private static final Logger logger = LoggerFactory.getLogger(AppConfig.class.getName());

  private String applicationVersion;
  private File appUserHome;
  private File legacyUserHome;

  private String applicationName;
  private String applicationPathName;
  private boolean debug;

  private String ticketUrl;
  private boolean enableIncidentReport;

  private String actionTokenEndpoint;
  private String tokenEndpoint;

  private String windowsInstalledPath;

  private boolean enableSystrayMenu;
  private boolean userPreferencesEditable;


  private static volatile AppConfig appConfig;
  private static volatile Properties properties;

  private AppConfig() {
    try {
      properties = loadAppConfigProperties();
      loadFromProperties(properties);
      final URL versionResourceURL = this.getClass().getResource("/version.txt");
      if (versionResourceURL == null) {
        logger.error("Cannot retrieve application version: version.txt not found");
      } else {
        try (InputStream input = versionResourceURL.openStream();
             InputStreamReader streamReader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
          BufferedReader reader = new BufferedReader(streamReader);
          String versionLine = reader.readLine();
          this.applicationVersion = versionLine.trim();
        }
      }
    } catch (final IOException e) {
      logger.error("Cannot retrieve application version: " + e.getMessage(), e);
      this.applicationVersion = "2.0.0";
    }
  }

  public static synchronized AppConfig get() {
    if (appConfig == null) {
      appConfig = new AppConfig();
    }
    return appConfig;
  }

  public Properties getProperties() {
    return properties;
  }

  private Properties loadAppConfigProperties() throws IOException {
    Properties props = new Properties();
    InputStream configFile = Thread.currentThread().getContextClassLoader()
        .getResourceAsStream("app-config.properties");
    if (configFile != null) {
      props.load(configFile);
    }
    return props;
  }

  public void loadFromProperties(final Properties props) {
    this.setApplicationName(props.getProperty(APPLICATION_NAME, "OBELISK Signing Portal client"));
    this.setApplicationPathName(props.getProperty(APPLICATION_PATH_NAME, "OBELISK Signing Portal client"));

    this.setDebug(Boolean.parseBoolean(props.getProperty(DEBUG, "false")));

    this.setActionTokenEndpoint(props.getProperty(ACTION_TOKEN_ENDPOINT, "/login-actions/action-token"));
    this.setTokenEndpoint(props.getProperty(TOKEN_ENDPOINT, "/protocol/openid-connect/token"));

    this.setUserPreferencesEditable(Boolean.parseBoolean(props.getProperty(USER_PREFERENCES_EDITABLE, "true")));

    this.setWindowsInstalledPath(
        props.getProperty(WINDOWS_INSTALLED_PATH, "C:\\Program Files\\SEFIRA\\OBELISK Signing Portal\\"));

    this.setEnableSystrayMenu(Boolean.parseBoolean(props.getProperty(ENABLE_SYSTRAY_MENU, "true")));
    this.setTicketUrl(props.getProperty(TICKET_URL, "ob-support@sefira.cz"));
    this.setEnableIncidentReport(Boolean.parseBoolean(props.getProperty(ENABLE_INCIDENT_REPORT, "false")));
  }

  public ConfigurationManager getConfigurationManager() {
    return new ConfigurationManager();
  }

  public boolean isDebug() {
    return this.debug;
  }

  public String getApplicationName() {
    return this.applicationName;
  }

  public String getApplicationPathName() {
    return applicationPathName;
  }

  public String getApplicationVersion() {
    return this.applicationVersion;
  }

  public boolean isUserPreferencesEditable() {
    return this.userPreferencesEditable;
  }

  public String getActionTokenEndpoint() {
    return actionTokenEndpoint;
  }

  public String getTokenEndpoint() {
    return tokenEndpoint;
  }

  public String getWindowsInstalledPath() {
    return windowsInstalledPath;
  }

  public boolean isEnableSystrayMenu() {
    return this.enableSystrayMenu;
  }

  public File getAppUserHome() {
    if (this.appUserHome != null) {
      return this.appUserHome;
    }
    final ConfigurationManager configurationManager = this.getConfigurationManager();
    try {
      this.appUserHome = configurationManager.manageConfiguration(this.getApplicationPathName());
    }
    catch (final IOException e) {
      logger.error("Error while managing App config : {}", e.getMessage(), e);
      this.appUserHome = null;
    }
    return this.appUserHome;
  }

  /**
   * Version 1.x.y app user home dir
   */
  public File getLegacyAppUserHome() {
    if (this.legacyUserHome != null) {
      return this.legacyUserHome;
    }
    final ConfigurationManager configurationManager = this.getConfigurationManager();
    try {
      this.legacyUserHome = configurationManager.manageConfiguration("OBELISK Signing Portal");
    }
    catch (final IOException e) {
      logger.error("Error while managing App config : {}", e.getMessage(), e);
      this.legacyUserHome = null;
    }
    return this.legacyUserHome;
  }

  public Path getAppProcessDirectory() throws IOException {
    File appUserHome = getAppUserHome();
    if (appUserHome == null) {
      throw new IllegalStateException("Unable to determine user app home");
    }
    final Path files = appUserHome.toPath().resolve(".files");
    if (!files.toFile().exists()) {
      Files.createDirectories(files);
      Files.setAttribute(files, "dos:hidden", true, LinkOption.NOFOLLOW_LINKS);
    }
    return files;
  }

  public Path getAppRunDirectory() throws IOException {
    Path runDir = getAppProcessDirectory().resolve("run");
    if (!runDir.toFile().exists()) {
      Files.createDirectory(runDir);
    }
    return runDir;
  }

  public Path getAppStorageDirectory() throws IOException {
    Path storage = getAppProcessDirectory().resolve("storage");
    if (!storage.toFile().exists()) {
      Files.createDirectory(storage);
    }
    return storage;
  }

  public String getTicketUrl() {
    return this.ticketUrl;
  }

  public boolean isEnableIncidentReport() {
    return this.enableIncidentReport;
  }

  // --- setters --/ //
  private void setDebug(final boolean debug) {
    this.debug = debug;
  }

  private void setApplicationName(final String applicationName) {
    this.applicationName = applicationName;
  }

  private void setApplicationPathName(String applicationPathName) {
    this.applicationPathName = applicationPathName;
  }

  private void setUserPreferencesEditable(final boolean userPreferencesEditable) {
    this.userPreferencesEditable = userPreferencesEditable;
  }

  private void setActionTokenEndpoint(String actionTokenEndpoint) {
    this.actionTokenEndpoint = actionTokenEndpoint;
  }

  private void setTokenEndpoint(String tokenEndpoint) {
    this.tokenEndpoint = tokenEndpoint;
  }

  private void setWindowsInstalledPath(String windowsInstalledPath) {
    this.windowsInstalledPath = windowsInstalledPath;
  }

  private void setEnableSystrayMenu(final boolean enableSystrayMenu) {
    this.enableSystrayMenu = enableSystrayMenu;
  }

  private void setTicketUrl(final String ticketUrl) {
    this.ticketUrl = ticketUrl;
  }

  private void setEnableIncidentReport(final boolean enableIncidentReport) {
    this.enableIncidentReport = enableIncidentReport;
  }

}

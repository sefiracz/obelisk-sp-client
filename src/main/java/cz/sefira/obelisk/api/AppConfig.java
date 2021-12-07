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
package cz.sefira.obelisk.api;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.apache.commons.lang.StringUtils.isNotEmpty;


/**
 * Configuration of the NexU Platform
 *
 * @author David Naramski
 */
public class AppConfig {

    private static final String APPLICATION_NAME = "application_name";
    private static final String SHOW_SPLASH_SCREEN = "show_splash_screen";
    private static final String DEBUG = "debug";

    private static final String NEXU_HOSTNAME = "nexu_hostname";
    private static final String BINDING_IP = "binding_ip";
    private static final String BINDING_PORTS = "binding_ports";
    private static final String BINDING_PORTS_HTTPS = "binding_ports_https";

    private static final String HTTP_SERVER_CLASS = "http_server_class";

    private static final String CORS_ALLOWED_ORIGIN = "cors_allowed_origin";

    private static final String TICKET_URL = "ticket_url";
    private static final String ENABLE_INCIDENT_REPORT = "enable_incident_report";

    private static final String REQUEST_PROCESSOR_CLASS = "request_processor_class";
    private static final String WINDOWS_INSTALLED_EXE_PATH = "windows_installed_executable_path";
    private static final String WINDOWS_STARTUP_LINK_PATH = "windows_startup_link_path";

    private static final String ROLLING_LOG_FILE_SIZE = "rolling_log_file_size";
    private static final String ROLLING_LOG_FILE_NUMBER = "rolling_log_file_number";

    private static final String ENABLE_SYSTRAY_MENU = "enable_systray_menu";
    private static final String USER_PREFERENCES_EDITABLE = "user_preferences_editable";
    private static final String CONNECTIONS_CACHE_MAX_SIZE = "connections_cache_max_size";

    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class.getName());

    private String applicationVersion;
    private File nexuHome;

    private String applicationName;
    private boolean showSplashScreen;
    private boolean debug;

    private String nexuHostname;
    private String bindingIP;
    private List<Integer> bindingPorts;
    private List<Integer> bindingPortsHttps;

    private String httpServerClass;

    private boolean corsAllowAllOrigins;
    private Set<String> corsAllowedOrigins;

    private String ticketUrl;
    private boolean enableIncidentReport;

    private String requestProcessorClass;
    private String windowsInstalledExePath;
    private String windowsStartupLinkPath;

    private String rollingLogMaxFileSize;
    private int rollingLogMaxFileNumber;

    private boolean enableSystrayMenu;
    private boolean userPreferencesEditable;
    private int connectionsCacheMaxSize;

    public AppConfig() {
        try {
            final URL versionResourceURL = this.getClass().getResource("/version.txt");
            if (versionResourceURL == null) {
                logger.error("Cannot retrieve application version: version.txt not found");
            } else {
                this.applicationVersion = IOUtils.toString(versionResourceURL, StandardCharsets.UTF_8);
            }
        } catch (final IOException e) {
            logger.error("Cannot retrieve application version: " + e.getMessage(), e);
            this.applicationVersion = "";
        }
    }

    public String getBindingIP() {
        return this.bindingIP;
    }

    public void setBindingIP(final String bindingIP) {
        this.bindingIP = bindingIP;
    }

    public List<Integer> getBindingPorts() {
        return this.bindingPorts;
    }

    public void setBindingPorts(final List<Integer> bindingPorts) {
        this.bindingPorts = Collections.unmodifiableList(bindingPorts);
    }

    public String getNexuHostname() {
        return this.nexuHostname;
    }

    public void setNexuHostname(final String nexuHostname) {
        this.nexuHostname = nexuHostname;
    }

    public String getHttpServerClass() {
        return this.httpServerClass;
    }

    public void setHttpServerClass(final String httpServerClass) {
        this.httpServerClass = httpServerClass;
    }

    public boolean isDebug() {
        return this.debug;
    }

    public void setDebug(final boolean debug) {
        this.debug = debug;
    }

    public String getApplicationName() {
        return this.applicationName;
    }

    public void setApplicationName(final String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationVersion() {
        return this.applicationVersion;
    }

    public int getConnectionsCacheMaxSize() {
        return this.connectionsCacheMaxSize;
    }

    public void setConnectionsCacheMaxSize(final int connectionsCacheMaxSize) {
        this.connectionsCacheMaxSize = connectionsCacheMaxSize;
    }

    public boolean isUserPreferencesEditable() {
        return this.userPreferencesEditable;
    }

    public void setUserPreferencesEditable(final boolean userPreferencesEditable) {
        this.userPreferencesEditable = userPreferencesEditable;
    }

    public String getRequestProcessorClass() {
        return this.requestProcessorClass;
    }

    public void setRequestProcessorClass(final String requestProcessorClass) {
        this.requestProcessorClass = requestProcessorClass;
    }

    public String getWindowsInstalledExePath() {
        return windowsInstalledExePath;
    }

    public void setWindowsInstalledExePath(String windowsInstalledExePath) {
        this.windowsInstalledExePath = windowsInstalledExePath;
    }

    public String getWindowsStartupLinkPath() {
        return windowsStartupLinkPath;
    }

    public void setWindowsStartupLinkPath(String windowsStartupLinkPath) {
        this.windowsStartupLinkPath = windowsStartupLinkPath;
    }

    public List<Integer> getBindingPortsHttps() {
        return this.bindingPortsHttps;
    }

    public void setBindingPortsHttps(final List<Integer> bindingPortsHttps) {
        this.bindingPortsHttps = Collections.unmodifiableList(bindingPortsHttps);
    }

    public String getRollingLogMaxFileSize() {
        return this.rollingLogMaxFileSize;
    }

    /**
     * This method allows to set the maximum size for a log file. Expected format : 64KB, 10MB,...
     *
     * @param rollingLogMaxFileSize
     */
    public void setRollingLogMaxFileSize(final String rollingLogMaxFileSize) {
        this.rollingLogMaxFileSize = rollingLogMaxFileSize;
    }

    public int getRollingLogMaxFileNumber() {
        return this.rollingLogMaxFileNumber;
    }

    /**
     * This method allows to set the maxium number of log files to keep on the file system.
     *
     * @param rollingLogMaxFileNumber
     */
    public void setRollingLogMaxFileNumber(final int rollingLogMaxFileNumber) {
        this.rollingLogMaxFileNumber = rollingLogMaxFileNumber;
    }

    public boolean isEnableSystrayMenu() {
        return this.enableSystrayMenu;
    }

    public void setEnableSystrayMenu(final boolean enableSystrayMenu) {
        this.enableSystrayMenu = enableSystrayMenu;
    }

    public File getNexuHome() {
        if (this.nexuHome != null) {
            return this.nexuHome;
        }

        final ConfigurationManager configurationManager = this.getConfigurationManager();
        try {
            this.nexuHome = configurationManager.manageConfiguration(this.getApplicationName());
        } catch (final IOException e) {
            logger.error("Error while managing Nexu config : {}", e.getMessage(), e);
            this.nexuHome = null;
        }
        return this.nexuHome;
    }



    public void loadFromProperties(final Properties props) {
        this.setApplicationName(props.getProperty(APPLICATION_NAME, "Obelisk Signing Portal"));

        final String bindingPortsStr = props.getProperty(BINDING_PORTS, "9795");
        if (isNotEmpty(bindingPortsStr)) {
            this.setBindingPorts(this.toListOfInt(bindingPortsStr));
        }

        this.setBindingIP(props.getProperty(BINDING_IP, "127.0.0.1"));
        this.setNexuHostname(props.getProperty(NEXU_HOSTNAME, "localhost"));
        this.setHttpServerClass(props.getProperty(HTTP_SERVER_CLASS, "cz.sefira.obelisk.jetty.JettyServer"));
        this.setDebug(Boolean.parseBoolean(props.getProperty(DEBUG, "false")));
        this.setConnectionsCacheMaxSize(Integer.parseInt(props.getProperty(CONNECTIONS_CACHE_MAX_SIZE, "1")));

        this.setUserPreferencesEditable(Boolean.parseBoolean(props.getProperty(USER_PREFERENCES_EDITABLE, "true")));

        this.setRollingLogMaxFileNumber(Integer.parseInt(props.getProperty(ROLLING_LOG_FILE_NUMBER, "5")));
        this.setRollingLogMaxFileSize(props.getProperty(ROLLING_LOG_FILE_SIZE, "10MB"));

        this.setRequestProcessorClass(props.getProperty(REQUEST_PROCESSOR_CLASS, "cz.sefira.obelisk.jetty.RequestProcessor"));
        this.setWindowsInstalledExePath(props.getProperty(WINDOWS_INSTALLED_EXE_PATH, "C:\\Program Files\\SEFIRA\\OBELISK Signing Portal\\OBELISK Signing Portal.exe"));
        this.setWindowsStartupLinkPath(props.getProperty(WINDOWS_STARTUP_LINK_PATH, "/AppData/Roaming/Microsoft/Windows/Start Menu/Programs/Startup/OBELISK Signing Portal.lnk"));

        final String bindingPortHttpsStr = props.getProperty(BINDING_PORTS_HTTPS, "9895");
        if (isNotEmpty(bindingPortHttpsStr)) {
            this.setBindingPortsHttps(this.toListOfInt(bindingPortHttpsStr));
        }

        this.setEnableSystrayMenu(Boolean.parseBoolean(props.getProperty(ENABLE_SYSTRAY_MENU, "true")));
        this.setCorsAllowedOrigins(props.getProperty(CORS_ALLOWED_ORIGIN, "*"));
        this.setTicketUrl(props.getProperty(TICKET_URL, "ob-support@sefira.cz"));
        this.setEnableIncidentReport(Boolean.parseBoolean(props.getProperty(ENABLE_INCIDENT_REPORT, "false")));
        this.setShowSplashScreen(Boolean.parseBoolean(props.getProperty(SHOW_SPLASH_SCREEN, "false")));
    }

    /**
     * Returns a list of {@link Integer} from <code>str</code> which should be tokenized by commas.
     *
     * @param str
     *            A list of strings tokenized by commas.
     * @return A list of {@link Integer}.
     */
    protected List<Integer> toListOfInt(final String str) {
        final List<Integer> ports = new ArrayList<Integer>();
        for (final String port : str.split(",")) {
            ports.add(Integer.parseInt(port.trim()));
        }
        return ports;
    }

    public ConfigurationManager getConfigurationManager() {
        return new ConfigurationManager();
    }

    public boolean isCorsAllowAllOrigins() {
		return corsAllowAllOrigins;
	}

	public Set<String> getCorsAllowedOrigins() {
        return this.corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(final String corsAllowedOrigins) {
    	if("*".equals(corsAllowedOrigins)) {
    		this.corsAllowAllOrigins = true;
    		this.corsAllowedOrigins = Collections.emptySet();
    	} else {
    		this.corsAllowAllOrigins = false;
    		final String[] corsAllowedOriginsArray = corsAllowedOrigins.split(",");
    		this.corsAllowedOrigins = new HashSet<String>(corsAllowedOriginsArray.length);
    		for(final String corsAllowedOrigin : corsAllowedOriginsArray) {
    			this.corsAllowedOrigins.add(corsAllowedOrigin.trim());
    		}
    	}
    }

    public String getTicketUrl() {
        return this.ticketUrl;
    }

    public void setTicketUrl(final String ticketUrl) {
        this.ticketUrl = ticketUrl;
    }

    public boolean isEnableIncidentReport() {
        return this.enableIncidentReport;
    }

    public void setEnableIncidentReport(final boolean enableIncidentReport) {
        this.enableIncidentReport = enableIncidentReport;
    }

    public boolean isShowSplashScreen() {
        return this.showSplashScreen;
    }

    public void setShowSplashScreen(final boolean showSplashScreen) {
        this.showSplashScreen = showSplashScreen;
    }

}
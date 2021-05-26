/**
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package lu.nowina.nexu;

/*
 * Copyright 2020 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.AppConfigurer
 *
 * Created: 29.01.2020
 * Author: hlavnicka
 */

import lu.nowina.nexu.api.EnvironmentInfo;
import lu.nowina.nexu.api.OS;
import mslinks.ShellLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class AppConfigurer {

	private static final Logger logger = LoggerFactory.getLogger(AppConfigurer.class.getSimpleName());

	private static final boolean isWindows;

	static {
		isWindows = EnvironmentInfo.buildFromSystemProperties(System.getProperties()).getOs().equals(OS.WINDOWS);
	}

	public static void setLocalePreferences(final UserPreferences preferences) {
		String language = preferences.getLanguage() != null ? preferences.getLanguage() : System.getProperty("user.language");
		Locale.setDefault(new Locale(language, ""));
		App.refreshSystrayMenu();
	}

	public static void applyUserPreferences(final UserPreferences preferences) {
		if(isWindows){
			try {
				checkAutoStartPresent(preferences.getAutoStart());
			}
			catch (IOException e) {
				logger.error("Unable to make startup link: "+e.getMessage(), e);
			}
		}
	}

	// TODO - add to properties
	private static void checkAutoStartPresent(boolean addShortcut) throws IOException {
		Path target = Paths.get("C:\\Program Files\\SEFIRA\\OBELISK Signing Portal\\OBELISK Signing Portal.exe");
		Path startup = Paths.get(System.getProperty("user.home"), "/AppData/Roaming/Microsoft/Windows/Start Menu/Programs/Startup/OBELISK Signing Portal.lnk");
		if (!startup.toFile().exists() && addShortcut) { // shortcut not found and want to add it
			ShellLink.createLink(target.toFile().getAbsolutePath(), startup.toFile().getAbsolutePath());
		} else if (startup.toFile().exists() && !addShortcut) { // shortcut found and want to remove it
		  boolean deleted = startup.toFile().delete();
		  logger.info("Deleted automatic app startup link: "+deleted);
		}
	}
}

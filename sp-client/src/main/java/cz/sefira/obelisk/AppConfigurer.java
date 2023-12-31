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
package cz.sefira.obelisk;

/*
 * Copyright 2020 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.AppConfigurer
 *
 * Created: 29.01.2020
 * Author: hlavnicka
 */

import ch.qos.logback.classic.Level;
import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.prefs.PreferencesFactory;
import cz.sefira.obelisk.prefs.UserPreferences;
import cz.sefira.obelisk.util.LogUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class AppConfigurer {

	private static final Logger logger = LoggerFactory.getLogger(AppConfigurer.class.getSimpleName());

	public static void applyLocale(PlatformAPI api, String lang) {
		String defaultLang = Locale.getDefault().getLanguage();
		if (!defaultLang.equalsIgnoreCase(lang)) {
			UserPreferences prefs = PreferencesFactory.getInstance(AppConfig.get());
			String prefLang = prefs.getLanguage();
			String language = StringUtils.defaultIfEmpty(prefLang, defaultLang); // prefs or default
			language = StringUtils.defaultIfEmpty(lang, language);
			// initialize preferences language
			if (prefLang == null) {
				prefs.setLanguage(language);
			}
			if (!defaultLang.equalsIgnoreCase(language)) {
				Locale.setDefault(new Locale(language, "")); // change locale language
				prefs.setLanguage(language); // save new language to preferences
				api.getSystray().refreshLabels(); // refresh Systray labels with new language
			}
		}
	}

	public static void applyUserPreferences(final UserPreferences preferences) {
		logger.info("Applying user preferences");
		LogUtils.setLogLevel(preferences.isDebugMode() ? Level.DEBUG : Level.INFO);
	}

}

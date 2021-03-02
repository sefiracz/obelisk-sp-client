/**
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Licensed under EUPL Version 1.1 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 *
 * http://ec.europa.eu/idabc/eupl5
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
 * lu.nowina.nexu.LocaleConfigurer
 *
 * Created: 29.01.2020
 * Author: hlavnicka
 */

import java.util.Locale;

public class LocaleConfigurer {

	public static void setUserPreferences(final UserPreferences preferences) {
		String language = preferences.getLanguage() != null ? preferences.getLanguage() : Locale.getDefault().getLanguage();
		Locale.setDefault(new Locale(language, ""));
		NexUApp.refreshSystrayMenu();
	}

}

package cz.sefira.obelisk.prefs;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.prefs.PreferencesFactory
 *
 * Created: 07.06.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AppConfig;

/**
 * User preferences factory
 */
public class PreferencesFactory {

  public static UserPreferences getInstance(AppConfig config) {
    String prefsImpl = System.getenv("OBSP_PREFS_IMPL");
    if ("java".equalsIgnoreCase(prefsImpl)) {
      return new JavaPreferences(config);
    } else if ("file".equalsIgnoreCase(prefsImpl)) {
      return new FilePreferences(config);
    }
    // default impl
    return new FilePreferences(config);
  }

}

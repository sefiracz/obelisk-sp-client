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

import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class UserPreferences {

	private static final String LANGUAGE = "sefira.obelisk.sp.language";
	private static final String AUTO_START = "sefira.obelisk.sp.autoStart";
  private static final String HIDDEN_DIALOGS = "sefira.obelisk.sp.hiddenDialogs";
  private static final String CACHE_DURATION = "sefira.obelisk.sp.cacheDuration";

	private final Preferences prefs;
	private final AppConfig appConfig;

	private String language;
	private String hiddenDialogIds;
	private Boolean autoStart;
	private Integer cacheDuration;

	public UserPreferences(final AppConfig appConfig) {
	  this.appConfig = appConfig;
		prefs = Preferences.userRoot().node(appConfig.getApplicationName().toLowerCase());

		language = prefs.get(LANGUAGE, Locale.getDefault().getLanguage());
    hiddenDialogIds = prefs.get(HIDDEN_DIALOGS, null);
		final String autoStartValue = prefs.get(AUTO_START, null);
    autoStart = Boolean.parseBoolean(autoStartValue);

    try {
      final String cacheDurationValue = prefs.get(CACHE_DURATION, "0");
      cacheDuration = Integer.parseInt(cacheDurationValue);
      if (cacheDuration < 0 || cacheDuration > 30) {
        cacheDuration = 0;
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

  public void addHiddenDialogId(String dialogId) {
    List<String> list = getHiddenDialogIds();
    list.add(dialogId);
    this.hiddenDialogIds = String.join(",", list);
    prefs.put(HIDDEN_DIALOGS, hiddenDialogIds);
  }

	public void setAutoStart(Boolean autoStart) {
		if(autoStart) {
			prefs.put(AUTO_START, "true");
		} else {
			prefs.remove(AUTO_START);
		}
		this.autoStart = autoStart;
	}

  public void setCacheDuration(Integer cacheDuration) {
    if(cacheDuration != null) {
      prefs.put(CACHE_DURATION, String.valueOf(cacheDuration));
    } else {
      prefs.remove(CACHE_DURATION);
    }
    this.cacheDuration = cacheDuration == null ? 0 : cacheDuration;
  }

  public String getLanguage() {
    return language;
  }

  public List<String> getHiddenDialogIds() {
    if(hiddenDialogIds != null) {
      String[] dialogIds = hiddenDialogIds.split(",");
      return new ArrayList<>(Arrays.asList(dialogIds));
    } else {
      return new ArrayList<>();
    }
  }

	public Boolean getAutoStart() {
		return autoStart != null ? autoStart : false;
	}

  public Integer getCacheDuration() {
    return cacheDuration;
  }

  public AppConfig getAppConfig() {
    return appConfig;
  }

  public void clear() {
    try {
      this.prefs.clear();
    } catch (BackingStoreException e) {
      throw new IllegalStateException(e);
    }

    language = null;
    hiddenDialogIds = null;
    autoStart = null;
    cacheDuration = 0;
  }

}
package cz.sefira.obelisk.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static org.apache.commons.lang.StringUtils.isBlank;

public class StageHelper {

	private static StageHelper instance;

	private static final Logger LOGGER = LoggerFactory.getLogger(StageHelper.class);

	private String title;

	private StageHelper() {
	}

	public static synchronized StageHelper getInstance() {
		if (instance == null) {
			synchronized (StageHelper.class) {
				if (instance == null) {
					instance = new StageHelper();
				}
			}
		}
		return instance;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String applicationName, final String resourceBundleKey) {
		if(isBlank(applicationName) && isBlank(resourceBundleKey)) {
			title = "";
			return;
		}
		String translatedTitle = "";
		try {
			translatedTitle = ResourceBundle.getBundle("bundles/nexu").getString(resourceBundleKey);
		} catch (MissingResourceException mre) {
			LOGGER.warn("Resource bundle key \"{}\" is missing.", resourceBundleKey);
		}catch(Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		if(!isBlank(applicationName) && !isBlank(translatedTitle)) {
			title = applicationName + " - " + translatedTitle;
		} else if(isBlank(applicationName)) {
			title = translatedTitle;
		} else if(isBlank(translatedTitle)) {
			title = applicationName;
		} else {
			title = "";
		}
	}

}

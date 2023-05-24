package cz.sefira.obelisk.flow;

import cz.sefira.obelisk.util.ResourceUtils;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
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
			translatedTitle = ResourceUtils.getBundle().getString(resourceBundleKey);
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

	public void setMinSize(Region rootPane, Stage stage) {
		double minWidth = rootPane.getMinWidth() > 0 ? rootPane.getMinWidth() : rootPane.getPrefWidth();
		double minHeight = rootPane.getMinHeight() > 0 ? rootPane.getMinHeight() : rootPane.getPrefHeight();
		if (minWidth > 0 && minHeight > 0) {
			stage.setMinWidth(minWidth);
			stage.setMinHeight(minHeight);
		}
	}

}

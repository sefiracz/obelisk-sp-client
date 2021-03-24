/**
 * © Nowina Solutions, 2015-2017
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
package lu.nowina.nexu;

import javafx.application.Platform;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.SystrayMenuItem;
import lu.nowina.nexu.api.flow.FutureOperationInvocation;
import lu.nowina.nexu.api.flow.OperationFactory;
import lu.nowina.nexu.api.flow.OperationResult;
import lu.nowina.nexu.systray.SystrayMenuInitializer;
import lu.nowina.nexu.view.core.NonBlockingUIOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class SystrayMenu {

	private static final Logger LOGGER = LoggerFactory.getLogger(SystrayMenu.class.getName());
	private SystrayMenuInitializer systrayMenuInitializer;

	public SystrayMenu(OperationFactory operationFactory, NexuAPI api, UserPreferences prefs) {
		final ResourceBundle resources = ResourceBundle.getBundle("bundles/nexu");

		final List<SystrayMenuItem> extensionSystrayMenuItems = api.getExtensionSystrayMenuItem();
		final SystrayMenuItem[] systrayMenuItems = new SystrayMenuItem[extensionSystrayMenuItems.size() + 2];

		systrayMenuItems[0] = createAboutSystrayMenuItem(api, resources);
		systrayMenuItems[1] = createPreferencesSystrayMenuItem(api, prefs);

		int i = 2;
		for(final SystrayMenuItem systrayMenuItem : extensionSystrayMenuItems) {
			systrayMenuItems[i++] = systrayMenuItem;
		}

		final SystrayMenuItem exitMenuItem = createExitSystrayMenuItem();

		final String tooltip = api.getAppConfig().getApplicationName();
		final URL trayIconURL = this.getClass().getResource("/tray-icon.png");
		try {
			switch(api.getEnvironmentInfo().getOs()) {
			case WINDOWS:
			case MACOSX:
			case LINUX:
				// Use reflection to avoid wrong initialization issues
				systrayMenuInitializer = Class.forName("lu.nowina.nexu.systray.AWTSystrayMenuInitializer")
					.asSubclass(SystrayMenuInitializer.class).getDeclaredConstructor().newInstance();
				systrayMenuInitializer.init(tooltip, trayIconURL, operationFactory, exitMenuItem, systrayMenuItems);
				break;
        // TODO - waiting for SystemTray 4.0 with Java 11 support - https://github.com/dorkbox/SystemTray/milestones/Release%204.0
//			case LINUX:
//				// Use reflection to avoid wrong initialization issues
//				systrayMenuInitializer = Class.forName("lu.nowina.nexu.systray.DorkboxSystrayMenuInitializer")
//					.asSubclass(SystrayMenuInitializer.class).getDeclaredConstructor().newInstance();
//				systrayMenuInitializer.init(tooltip, trayIconURL, operationFactory, exitMenuItem, systrayMenuItems);
//				break;
			case NOT_RECOGNIZED:
				LOGGER.warn("System tray is currently not supported for NOT_RECOGNIZED OS.");
				break;
			default:
				throw new IllegalArgumentException("Unhandled value: " + api.getEnvironmentInfo().getOs());
			}
		} catch (ReflectiveOperationException e) {
			LOGGER.error("Cannot initialize systray menu", e);
		}
	}

	public static SystrayMenuItem createAboutSystrayMenuItem(final NexuAPI api, final ResourceBundle resources) {
		return new SystrayMenuItem() {

			@Override
			public String getName() {
				return "systray.menu.about";
			}

			@Override
			public String getLabel() {
				return ResourceBundle.getBundle("bundles/nexu").getString(getName());
			}

			@Override
			public FutureOperationInvocation<Void> getFutureOperationInvocation() {
				return operationFactory -> operationFactory.getOperation(NonBlockingUIOperation.class, "/fxml/about.fxml",
            api.getAppConfig().getApplicationName(), api.getAppConfig().getApplicationVersion(),
            resources).perform();
			}
		};
	}

	public static SystrayMenuItem createPreferencesSystrayMenuItem(final NexuAPI api, final UserPreferences prefs) {
		return new SystrayMenuItem() {

			@Override
			public String getName() {
				return "systray.menu.preferences";
			}

			@Override
			public String getLabel() {
				return ResourceBundle.getBundle("bundles/nexu").getString(getName());
			}

			@Override
			public FutureOperationInvocation<Void> getFutureOperationInvocation() {
				return operationFactory -> {
          final ProxyConfigurer proxyConfigurer = new ProxyConfigurer(api.getAppConfig(), prefs);
          return operationFactory.getOperation(NonBlockingUIOperation.class, "/fxml/preferences.fxml",
              proxyConfigurer, prefs, !api.getAppConfig().isUserPreferencesEditable()).perform();
        };
			}
		};
	}

	public static SystrayMenuItem createExitSystrayMenuItem() {
		return new SystrayMenuItem() {

			@Override
			public String getName() {
				return "systray.menu.exit";
			}

			@Override
			public String getLabel() {
				return ResourceBundle.getBundle("bundles/nexu").getString(getName());
			}

			@Override
			public FutureOperationInvocation<Void> getFutureOperationInvocation() {
				return operationFactory -> {
          LOGGER.info("Exiting...");
          Platform.exit();
          return new OperationResult<Void>((Void) null);
        };
			}
		};
	}

	public SystrayMenuInitializer getSystrayMenuInitializer() {
		return systrayMenuInitializer;
	}
}

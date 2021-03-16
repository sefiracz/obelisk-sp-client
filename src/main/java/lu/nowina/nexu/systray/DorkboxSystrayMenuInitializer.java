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
package lu.nowina.nexu.systray;

import dorkbox.systemTray.Menu;
import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.SystemTray;
import lu.nowina.nexu.api.SystrayMenuItem;
import lu.nowina.nexu.api.flow.OperationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Implementation of {@link SystrayMenuInitializer} using
 * <a href="https://github.com/dorkbox/SystemTray">SystemTray from Dorkbox</a>.
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public class DorkboxSystrayMenuInitializer implements SystrayMenuInitializer {

	private static final Logger LOGGER = LoggerFactory.getLogger(DorkboxSystrayMenuInitializer.class.getName());

	private int menuItems = 0;

	public DorkboxSystrayMenuInitializer() {
		super();
	}

	@Override
	public void init(final String tooltip, final URL trayIconURL, final OperationFactory operationFactory,
			final SystrayMenuItem exitMenuItem, final SystrayMenuItem... systrayMenuItems) {
		final SystemTray systemTray = SystemTray.get();
		if (systemTray == null) {
			LOGGER.warn("System tray is currently not supported.");
			return;
		}

		systemTray.setImage(trayIconURL);

		final Menu menu = systemTray.getMenu();
		for(final SystrayMenuItem systrayMenuItem : systrayMenuItems) {
			menu.add(new LocalizedMenuItem(systrayMenuItem.getLabel(), systrayMenuItem.getName(),
					(e) -> systrayMenuItem.getFutureOperationInvocation().call(operationFactory)));
			menuItems++;
		}

		menu.add(new LocalizedMenuItem(exitMenuItem.getLabel(), exitMenuItem.getName(),
				(e) -> exitMenuItem.getFutureOperationInvocation().call(operationFactory)));
		menuItems++;
	}

	public void refreshLabels() {
		for (int i = 0; i < menuItems; i++) {
			LocalizedMenuItem entry = (LocalizedMenuItem) SystemTray.get().getMenu().get(i);
			entry.setText(ResourceBundle.getBundle("bundles/nexu").getString(entry.getName()));
		}
	}

	private static class LocalizedMenuItem extends MenuItem {

		private final String name;

		public LocalizedMenuItem(String text, String name, ActionListener callback) {
			super(text, callback);
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

}

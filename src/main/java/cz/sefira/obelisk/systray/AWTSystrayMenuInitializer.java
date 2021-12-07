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
package cz.sefira.obelisk.systray;

import cz.sefira.obelisk.api.SystrayMenuItem;
import cz.sefira.obelisk.api.flow.OperationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Implementation of {@link SystrayMenuInitializer} using AWT.
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public class AWTSystrayMenuInitializer implements SystrayMenuInitializer {

	private static final Logger LOGGER = LoggerFactory.getLogger(AWTSystrayMenuInitializer.class.getName());
	private Robot robot;

	public AWTSystrayMenuInitializer() {
		super();
		try {
			this.robot = new Robot();
		}
		catch (AWTException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	@Override
	public void init(final String tooltip, final URL trayIconURL, final OperationFactory operationFactory,
			final SystrayMenuItem exitMenuItem, final SystrayMenuItem... systrayMenuItems) {
		if (SystemTray.isSupported()) {
			final PopupMenu popup = new PopupMenu();

			for(final SystrayMenuItem systrayMenuItem : systrayMenuItems) {
				final MenuItem mi = new MenuItem(systrayMenuItem.getLabel());
				mi.setName(systrayMenuItem.getName());
				mi.addActionListener((l) -> systrayMenuItem.getFutureOperationInvocation().call(operationFactory));
				popup.add(mi);
			}

			final Image image = Toolkit.getDefaultToolkit().getImage(trayIconURL);
			final TrayIcon trayIcon = new TrayIcon(image, tooltip, popup);
			trayIcon.setImageAutoSize(true);
			trayIcon.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					// simulate right-click when user performs left-click to open the popup
					if (e.getButton() == MouseEvent.BUTTON1 && robot != null) {
						robot.mousePress(MouseEvent.BUTTON3_DOWN_MASK);
						robot.mouseRelease(MouseEvent.BUTTON3_DOWN_MASK);
					}
				}
			});

			final MenuItem mi = new MenuItem(exitMenuItem.getLabel());
			mi.setName(exitMenuItem.getName());
			mi.addActionListener((l) -> exit(operationFactory, exitMenuItem, trayIcon));
			popup.add(mi);

			try {
				SystemTray.getSystemTray().add(trayIcon);
			} catch (final AWTException e) {
				LOGGER.error("Cannot add TrayIcon", e);
			}
		} else {
			LOGGER.error("System tray is currently not supported.");
		}
	}

	@Override
	public void refreshLabels() {
		TrayIcon[] trayIcons = SystemTray.getSystemTray().getTrayIcons();
		if (trayIcons.length > 0) {
			TrayIcon trayIcon = trayIcons[0];
			for (int i = 0; i < trayIcon.getPopupMenu().getItemCount(); i++) {
				MenuItem item = trayIcon.getPopupMenu().getItem(i);
				item.setLabel(ResourceBundle.getBundle("bundles/nexu").getString(item.getName()));
			}
		}
	}

	private void exit(final OperationFactory operationFactory, final SystrayMenuItem exitMenuItem,
			final TrayIcon trayIcon) {
		SystemTray.getSystemTray().remove(trayIcon);
		exitMenuItem.getFutureOperationInvocation().call(operationFactory);
 	}
}
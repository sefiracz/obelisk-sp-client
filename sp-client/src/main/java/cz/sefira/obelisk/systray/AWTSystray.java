package cz.sefira.obelisk.systray;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.systray.AWTSystray
 *
 * Created: 03.05.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.Systray;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

/**
 * AWTSystray
 */
public class AWTSystray extends AbstractSystray {

  private static final Logger logger = LoggerFactory.getLogger(AWTSystray.class.getName());

  public AWTSystray(String tooltip, URL trayIcon) {
    super(tooltip, trayIcon);
  }

  @Override
  public void spawnTray(final Runnable r, final SystrayMenuItem... systrayMenuItems) {
    PopupMenu popup = null;
    if (systrayMenuItems != null && systrayMenuItems.length > 0) {
      popup = new PopupMenu();
      for (SystrayMenuItem item : systrayMenuItems) {
        final MenuItem mi = new MenuItem(item.getLabel());
        mi.setName(item.getName());
        mi.addActionListener((l) -> Platform.runLater(item.getOperation()));
        popup.add(mi);
      }
    }
    final Image image = Toolkit.getDefaultToolkit().getImage(trayIcon);
    final TrayIcon trayIcon = new TrayIcon(image, tooltip, popup);
    trayIcon.setImageAutoSize(true);
    trayIcon.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
          Platform.runLater(r);
        }
      }
    });

    try {
      SystemTray.getSystemTray().add(trayIcon);
      logger.info("Creating AWT SystemTray icon");
    } catch (final AWTException e) {
      logger.error("Cannot add TrayIcon", e);
    }
  }

}

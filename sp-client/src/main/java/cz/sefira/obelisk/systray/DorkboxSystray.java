package cz.sefira.obelisk.systray;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.systray.DorkboxSystray
 *
 * Created: 03.05.2023
 * Author: hlavnicka
 */

//import dorkbox.systemTray.MenuItem;
//import dorkbox.systemTray.SystemTray;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * DorkboxSystray
 */
public class DorkboxSystray extends AbstractSystray {

  private static final Logger logger = LoggerFactory.getLogger(DorkboxSystray.class.getName());

  public DorkboxSystray(String tooltip, URL trayIcon) {
    super(tooltip, trayIcon);
  }

  @Override
  public void spawnTray(Runnable r) {
//    SystemTray systemTray = SystemTray.get();
//    if (systemTray != null) {
//      systemTray.setImage(trayIcon);
//      systemTray.setTooltip(tooltip);
//      systemTray.getMenu().add(new MenuItem("Open", e -> Platform.runLater(r)));
//      logger.info("Creating Dorkbox SystemTray icon");
//    } else {
//      logger.error("Unable to spawn system tray icon");
//    }
  }
}

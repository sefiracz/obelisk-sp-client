package cz.sefira.obelisk;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.Systray
 *
 * Created: 03.05.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.systray.AWTSystray;
import cz.sefira.obelisk.systray.AbstractSystray;
import cz.sefira.obelisk.systray.DorkboxSystray;
import cz.sefira.obelisk.view.StandaloneDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.net.URL;

/**
 * description
 */
public class Systray {

  private static final Logger logger = LoggerFactory.getLogger(Systray.class.getName());

  public static void spawnSystray(PlatformAPI api) {
    logger.info("Spawning system tray icon");
    final String tooltip = AppConfig.get().getApplicationName();
    final URL trayIcon = Systray.class.getResource("/images/icon.png");

    AbstractSystray systray = null;
    if (SystemTray.isSupported()) {
      systray = new AWTSystray(tooltip, trayIcon);
    }
    if (systray != null) {
      systray.spawnTray(() -> StandaloneDialog.createDialogFromFXML("/fxml/main-window.fxml", null, false, api));
    }
  }
}

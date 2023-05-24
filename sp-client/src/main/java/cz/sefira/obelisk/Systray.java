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
import cz.sefira.obelisk.systray.SystrayMenuItem;
import cz.sefira.obelisk.util.ResourceUtils;
import cz.sefira.obelisk.view.StandaloneDialog;
import cz.sefira.obelisk.view.core.StageState;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.net.URL;
import java.util.ResourceBundle;

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
    ResourceBundle rb = ResourceUtils.getBundle();
    Runnable r = () -> StandaloneDialog.createDialogFromFXML("/fxml/main-window.fxml", null, StageState.BLOCKING, api);
    if (systray != null) {
      SystrayMenuItem[] items = {
          new SystrayMenuItem(rb.getString("systray.menu.open"), "systray.menu.open", r),
          new SystrayMenuItem(rb.getString("systray.menu.exit"), "systray.menu.exit", Platform::exit)
      };
      systray.spawnTray(r, items);
    }
  }

  public static void refreshLabels() {
    TrayIcon[] trayIcons = SystemTray.getSystemTray().getTrayIcons();
    if (trayIcons != null && trayIcons.length > 0) {
      TrayIcon trayIcon = trayIcons[0];
      for (int i = 0; i < trayIcon.getPopupMenu().getItemCount(); i++) {
        MenuItem item = trayIcon.getPopupMenu().getItem(i);
        item.setLabel(ResourceUtils.getBundle().getString(item.getName()));
      }
    }
  }
}

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
import cz.sefira.obelisk.api.Notification;
import cz.sefira.obelisk.api.NotificationType;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.systray.AWTSystray;
import cz.sefira.obelisk.systray.AbstractSystray;
import cz.sefira.obelisk.systray.NoSystray;
import cz.sefira.obelisk.systray.SystrayMenuItem;
import cz.sefira.obelisk.util.ResourceUtils;
import cz.sefira.obelisk.util.annotation.NotNull;
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

  private final AbstractSystray systray;
  private final PlatformAPI api;

  public Systray(PlatformAPI api) {
    this.api = api;
    logger.info("Spawning system tray icon");
    final String tooltip = AppConfig.get().getApplicationName();
    final URL trayIcon = Systray.class.getResource("/images/icon.png");
    if (SystemTray.isSupported()) {
      // AWT implementation
      systray = new AWTSystray(api, tooltip, trayIcon);
    } else {
      // default no systray
      systray = new NoSystray(api);
    }
    ResourceBundle rb = ResourceUtils.getBundle();
    Runnable r = () -> StandaloneDialog.createDialogFromFXML("/fxml/main-window.fxml", null, StageState.BLOCKING, api);
    if (systray instanceof AWTSystray) {
      SystrayMenuItem[] items = {
          new SystrayMenuItem(rb.getString("systray.menu.open"), "systray.menu.open", r),
          new SystrayMenuItem(rb.getString("systray.menu.exit"), "systray.menu.exit", Platform::exit)
      };
      systray.spawnTray(r, items);
    }
  }

  public void pushNotification(@NotNull Notification notification) {
    NotificationType showNotification = new UserPreferences(AppConfig.get()).getShowNotifications();
    logger.info("Push notification: " + notification.getMessageText());
    // push notification into events
    api.getEventsStorage().addNotification(notification);
    // show notification
    switch (showNotification) {
      case NATIVE:
        systray.pushNotification(notification);
        break;
      case INTEGRATED:
        api.pushIntegratedNotification(notification);
        break;
      case OFF:
      default:
        break;
    }
  }

  public void refreshLabels() {
    systray.refreshLabels();
  }
}

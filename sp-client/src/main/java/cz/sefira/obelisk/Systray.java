/**
 * © SEFIRA spol. s r.o., 2020-2023
 * <p>
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 * <p>
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 * <p>
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
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

import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.api.notification.EventNotification;
import cz.sefira.obelisk.api.notification.Notification;
import cz.sefira.obelisk.api.notification.NotificationType;
import cz.sefira.obelisk.prefs.PreferencesFactory;
import cz.sefira.obelisk.systray.*;
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
import java.util.concurrent.TimeUnit;

/**
 * description
 */
public class Systray {

  private static final Logger logger = LoggerFactory.getLogger(Systray.class.getName());
  private static final long DISPLAY_PERIOD = TimeUnit.SECONDS.toMillis(2);

  private final AbstractSystray systray;
  private final PlatformAPI api;

  private long lastShown = 0;
  private boolean showing = false;

  public Systray(PlatformAPI api) {
    this.api = api;
    logger.info("Spawning system tray icon");
    final String tooltip = AppConfig.get().getApplicationName();
    final URL trayIcon = Systray.class.getResource("/images/icon"+(Boolean.parseBoolean(System.getProperty("dev.icon")) ? "-dev" : "")+".png");
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
    NotificationType showNotification = PreferencesFactory.getInstance(AppConfig.get()).getShowNotifications();
    logger.info("Push notification ("+showNotification.name()+"): " + notification.getMessageText());
    // push notification into events
    if (notification instanceof EventNotification) {
      api.getEventsStorage().addNotification((EventNotification) notification);
    }
    // show notification
    switch (showNotification) {
      case NATIVE:
        systray.pushNotification(notification);
        break;
      case INTEGRATED:
        waitForLast(notification); // wait if last notification before closing wasn't displayed for longer period
        // push notification
        showing = true;
        lastShown = System.currentTimeMillis();
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

  private void waitForLast(Notification notification) {
    long displayTime = System.currentTimeMillis() - lastShown;
    if (showing && notification.isClose() && (displayTime < DISPLAY_PERIOD)) {
      try {
        showing = false;
        Thread.sleep(DISPLAY_PERIOD - displayTime); // show last notification for longer
      } catch (InterruptedException e) {
        logger.error(e.getMessage(), e);
      }
    }
  }
}

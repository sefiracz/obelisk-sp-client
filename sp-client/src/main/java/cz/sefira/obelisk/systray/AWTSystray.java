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

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.notification.MessageType;
import cz.sefira.obelisk.api.notification.Notification;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.util.ResourceUtils;
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

  private TrayIcon trayIcon;

  public AWTSystray(PlatformAPI api, String tooltip, URL icon) {
    super(api, tooltip, icon);
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
    final Image image = Toolkit.getDefaultToolkit().getImage(icon);
    trayIcon = new TrayIcon(image, tooltip, popup);
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

  @Override
  public void pushNotification(Notification notification) {
    TrayIcon.MessageType type = getAWTMessageType(notification.getType());
    trayIcon.displayMessage(AppConfig.get().getApplicationName(), notification.getMessageText(), type);
  }

  public void refreshLabels() {
    TrayIcon[] trayIcons = SystemTray.getSystemTray().getTrayIcons();
    if (trayIcons != null && trayIcons.length > 0) {
      TrayIcon trayIcon = trayIcons[0];
      for (int i = 0; i < trayIcon.getPopupMenu().getItemCount(); i++) {
        MenuItem item = trayIcon.getPopupMenu().getItem(i);
        item.setLabel(ResourceUtils.getBundle().getString(item.getName()));
      }
    }
  }

  private TrayIcon.MessageType getAWTMessageType(MessageType messageType) {
    TrayIcon.MessageType type;
    switch (messageType) {
      case WARNING:
        type = TrayIcon.MessageType.WARNING;
        break;
      case ERROR:
        type = TrayIcon.MessageType.ERROR;
        break;
      case INFO:
        type = TrayIcon.MessageType.INFO;
        break;
      case SUCCESS:
      case NONE:
      default:
        type = TrayIcon.MessageType.NONE;
        break;
    }
    return type;
  }

}

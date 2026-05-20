package cz.sefira.obelisk.api.notification.os;

/*
 * Copyright 2025 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.notification.NotificationService
 *
 * Created: 03/11/2025
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.notification.MessageType;
import cz.sefira.obelisk.api.notification.Notification;
import cz.sefira.obelisk.api.notification.NotificationService;

import java.awt.*;

/**
 * Windows notifications service
 */
public class WindowsNotificationService extends NotificationService {

  public WindowsNotificationService(PlatformAPI api) {
    super(api);
  }

  public void pushNativeNotification(Notification notification) {
    TrayIcon[] trayIcons = SystemTray.getSystemTray().getTrayIcons();
    if (trayIcons != null && trayIcons.length == 1) {
      TrayIcon.MessageType messageType = getAWTMessageType(notification.getType());
      trayIcons[0].displayMessage(AppConfig.get().getApplicationName(), notification.getMessageText(), messageType);
    } else {
      pushIntegratedNotification(notification);
    }
  }

  private TrayIcon.MessageType getAWTMessageType(MessageType messageType) {
    return switch (messageType) {
      case WARNING -> TrayIcon.MessageType.WARNING;
      case ERROR -> TrayIcon.MessageType.ERROR;
      case INFO -> TrayIcon.MessageType.INFO;
      default -> TrayIcon.MessageType.NONE;
    };
  }

}

package cz.sefira.obelisk.api.notification;

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

import java.awt.*;

/**
 * description
 */
public class WindowsNotificationService extends NotificationService {

  private TrayIcon trayIcon;

  public WindowsNotificationService(PlatformAPI api, TrayIcon trayIcon) {
    super(api);
    this.trayIcon = trayIcon;
  }

  public void pushNotification(NotificationType type, Notification notification) {
    switch (type) {
      case NATIVE:
        TrayIcon.MessageType messageType = getAWTMessageType(notification.getType());
        trayIcon.displayMessage(AppConfig.get().getApplicationName(), notification.getMessageText(), messageType);
        break;
      case INTEGRATED:

        break;
      case OFF:
      default:
        break;
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

package cz.sefira.obelisk.api.notification;

/*
 * Copyright 2025 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.notification.NotificationServiceFactory
 *
 * Created: 03/11/2025
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.model.OS;
import cz.sefira.obelisk.api.notification.os.LinuxNotificationService;
import cz.sefira.obelisk.api.notification.os.MacOSNotificationService;
import cz.sefira.obelisk.api.notification.os.WindowsNotificationService;

/**
 * description
 */
public class NotificationServiceFactory {

  private static volatile NotificationService notificationService;

  public static NotificationService get(PlatformAPI api) {
    if (notificationService == null) {
      synchronized (NotificationService.class) {
        if (notificationService == null) {
          if (OS.isWindows())
            notificationService = new WindowsNotificationService(api);
          else if (OS.isLinux())
            notificationService = new LinuxNotificationService(api);
          else if (OS.isMacOS())
            notificationService = new MacOSNotificationService(api);
          else
          throw new IllegalStateException("Unexpected operating system: '" + System.getProperty("os.name") + "'");
        }
      }
    }
    return notificationService;
  }

}

package cz.sefira.obelisk.api.notification.os;

/*
 * Copyright 2026 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.notification.os.MacOSNotificationService
 *
 * Created: 24/04/2026
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.notification.Notification;
import cz.sefira.obelisk.api.notification.NotificationService;

/**
 * MacOS notification service
 */
public class MacOSNotificationService extends NotificationService {

  public MacOSNotificationService(PlatformAPI api) {
    super(api);
  }

  @Override
  public void pushNativeNotification(Notification notification) {
    // TODO missing native support
    pushIntegratedNotification(notification); // fallback
  }

}

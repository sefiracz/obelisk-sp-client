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

import cz.sefira.obelisk.api.PlatformAPI;

/**
 * description
 */
public abstract class NotificationService {

  private final PlatformAPI api;

  public NotificationService(PlatformAPI api) {
    this.api = api;
  }

  abstract void pushNotification(NotificationType type, Notification notification);

  protected void pushIntegratedNotification(Notification notification) {
    api.pushIntegratedNotification(notification);
  }

}

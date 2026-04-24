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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * description
 */
public abstract class NotificationService {

  private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

  private static final long DISPLAY_PERIOD = TimeUnit.SECONDS.toMillis(2);

  private final PlatformAPI api;

  private long lastShown = 0;
  private boolean showing = false;

  public NotificationService(PlatformAPI api) {
    this.api = api;
  }

  public abstract void pushNativeNotification(Notification notification);

  public void pushIntegratedNotification(Notification notification) {
    waitForLast(notification); // wait if last notification before closing wasn't displayed for longer period
    // push notification
    api.pushIntegratedNotification(notification);
    showing = true;
    lastShown = System.currentTimeMillis();
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

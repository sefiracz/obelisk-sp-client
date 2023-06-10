package cz.sefira.obelisk.api.notification;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.generic.LongActivityNotifier
 *
 * Created: 08/06/2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.util.ResourceUtils;
import cz.sefira.obelisk.util.annotation.NotNull;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Notify user about activity taking long time then expected
 * Wrap potentially long-lasting code within try-with-resource or close manually to gauge long activity scope
 */
public class LongActivityNotifier extends TimerTask implements AutoCloseable {

  private static final AtomicInteger nextSerialNumber = new AtomicInteger(0);
  private static int serialNumber() {
    return nextSerialNumber.getAndIncrement();
  }

  private final Timer timer;

  private final PlatformAPI api;

  private final Notification notification;

  public LongActivityNotifier(@NotNull PlatformAPI api, @NotNull String notificationMessage, long delay) {
    this.api = api;
    this.notification = new Notification(ResourceUtils.getBundle().getString(notificationMessage), MessageType.WARNING);
    this.timer = new Timer("LongActivity-"+serialNumber(),false);
    timer.schedule(this, delay);
  }

  @Override
  public void run() {
    if (api != null && notification != null) {
      api.getSystray().pushNotification(notification);
    }
  }

  @Override
  public void close() {
    timer.cancel();
    timer.purge();
  }
}

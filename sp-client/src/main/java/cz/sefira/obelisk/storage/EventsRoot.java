package cz.sefira.obelisk.storage;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.storage.EventsRoot
 *
 * Created: 05.05.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.Notification;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * description
 */
public class EventsRoot {

  private AtomicLong sequence = new AtomicLong(1);

  private boolean closeFlag;

  private List<Notification> notifications;

  public AtomicLong getSequence() {
    return sequence;
  }

  public void setSequence(AtomicLong sequence) {
    this.sequence = sequence;
  }

  public void incrementSequence() {
    this.sequence.incrementAndGet();
  }

  public List<Notification> getNotifications() {
    if (notifications == null) {
      notifications = new ArrayList<>();
    }
    return notifications;
  }

  public boolean isCloseFlag() {
    return closeFlag;
  }

  public void setCloseFlag(boolean closeFlag) {
    this.closeFlag = closeFlag;
  }
}

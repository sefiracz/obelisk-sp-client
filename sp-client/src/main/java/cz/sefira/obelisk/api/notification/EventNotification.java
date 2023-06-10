package cz.sefira.obelisk.api.notification;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.Notification
 *
 * Created: 02.05.2023
 * Author: hlavnicka
 */

/**
 * Event notification message (gets recorded in events storage)
 */
public class EventNotification extends Notification {

  public EventNotification(String messageText) {
    super(messageText);
  }

  public EventNotification(String messageText, MessageType type) {
    super(messageText, type);
  }

  public EventNotification(String messageText, MessageType type, boolean close, int delay) {
    super(messageText, type, close, delay);
  }
}

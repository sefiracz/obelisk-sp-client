package cz.sefira.obelisk.api;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.Notification
 *
 * Created: 02.05.2023
 * Author: hlavnicka
 */

import java.util.Date;

/**
 * Notification message
 */
public class Notification {

  private long seqId;
  private final String messageText;
  private final Date date;
  private boolean close = false;
  private int delay;

  public Notification() {
    // empty notification (placeholder)
    this.messageText = null;
    this.date = null;
    this.seqId = -1L;
  }

  public Notification(String messageText) {
    this(messageText, false, 5);
  }

  public Notification(String messageText, boolean close, int delay) {
    this.messageText = messageText;
    this.close = close;
    this.delay = delay;
    this.date = new Date();
  }

  public long getSeqId() {
    return seqId;
  }

  public void setSeqId(long seqId) {
    this.seqId = seqId;
  }

  public String getMessageText() {
    return messageText;
  }

  public boolean isClose() {
    return close;
  }

  public int getDelay() {
    return delay;
  }

  public Date getDate() {
    return date;
  }

}

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

import cz.sefira.obelisk.util.TextUtils;

import java.util.Date;

/**
 * Notification message
 */
public class Notification {

  private long seqId;
  private final String messageText;
  private final Date date = new Date();
  private boolean close = false;
  private int delay;

  public Notification(String messageText) {
    this.messageText = messageText;
  }

  public Notification(String messageText, boolean close, int delay) {
    this.messageText = messageText;
    this.close = close;
    this.delay = delay;
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

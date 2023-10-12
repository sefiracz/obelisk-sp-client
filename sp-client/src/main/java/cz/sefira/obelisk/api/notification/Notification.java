/**
 * © SEFIRA spol. s r.o., 2020-2023
 * <p>
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 * <p>
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 * <p>
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package cz.sefira.obelisk.api.notification;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.Notification
 *
 * Created: 08/06/2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.util.annotation.NotNull;

import java.util.Date;

/**
 * Notification
 */
public class Notification {

  private long seqId;
  private final String messageText;
  private final Date date;
  private final MessageType type;
  private final boolean close;
  private final int delay;

  public Notification(String messageText) {
    this(messageText, MessageType.INFO, false, 5);
  }

  public Notification(String messageText, @NotNull MessageType type) {
    this(messageText, type, false, 5);
  }

  public Notification(String messageText, @NotNull MessageType type, boolean close, int delay) {
    this.messageText = messageText;
    this.type = type;
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

  public MessageType getType() {
    return type;
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

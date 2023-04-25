package cz.sefira.obelisk.ipc;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.ipc.Message
 *
 * Created: 01.03.2023
 * Author: hlavnicka
 */

/**
 * description
 */
public class Message {

  private final String id;
  private final Long timestamp;
  private final byte[] payload;

  public Message(String id, Long timestamp, byte[] payload) {
    this.id = id;
    this.timestamp = timestamp;
    this.payload = payload;
  }

  public String getId() {
    return id;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public byte[] getPayload() {
    return payload;
  }
}

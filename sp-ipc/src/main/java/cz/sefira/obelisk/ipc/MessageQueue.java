package cz.sefira.obelisk.ipc;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.ipc.MessageQueue
 *
 * Created: 26.01.2023
 * Author: hlavnicka
 */

/**
 * MessageQueue interface
 */
public interface MessageQueue {

  /**
   * Add message to the queue and return message ID
   * @param message Message payload
   * @return Message identification
   */
  String addMessage(byte[] message);

  /**
   * Add message to thq queue and return message ID
   * @param message Message instance
   * @return Message identification
   */
  String addMessage(Message message);

  /**
   * Retrieve message from queue
   * @return Message instance
   */
  Message getMessage();

}

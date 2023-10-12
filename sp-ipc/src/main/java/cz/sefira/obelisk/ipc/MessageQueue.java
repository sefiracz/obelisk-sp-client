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

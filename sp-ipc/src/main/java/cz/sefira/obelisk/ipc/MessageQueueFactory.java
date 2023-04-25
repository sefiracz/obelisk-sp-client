package cz.sefira.obelisk.ipc;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.ipc.MessageQueueFactory
 *
 * Created: 26.01.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AppConfig;

/**
 * MessageQueue factory
 */
public class MessageQueueFactory {

  public static MessageQueue getInstance(AppConfig appConfig) {
    return new FileMessageQueue(appConfig);
  }

}

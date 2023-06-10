package cz.sefira.obelisk.api.notification;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.notification.MessageType
 *
 * Created: 09/06/2023
 * Author: hlavnicka
 */

/**
 * Notification message type
 */
public enum MessageType {

  /** A success message */
  SUCCESS,
  /** An error message */
  ERROR,
  /** A warning message */
  WARNING,
  /** An information message */
  INFO,
  /** Simple message */
  NONE

}

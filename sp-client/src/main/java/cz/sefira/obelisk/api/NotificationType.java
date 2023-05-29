package cz.sefira.obelisk.api;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.NotificationType
 *
 * Created: 26.05.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.util.ResourceUtils;

/**
 * Notification type
 */
public enum NotificationType {

  OFF("notification.type.off"), NATIVE("notification.type.native"), INTEGRATED("notification.type.integrated");

  private final String type;

  NotificationType(String type) {
    this.type = type;
  }

  public static NotificationType fromType(String type) {
    if (type != null) {
      for (NotificationType n : NotificationType.values()) {
        if (n.getType().equals(type)) {
          return n;
        }
      }
    }
    return getDefault();
  }

  public static NotificationType getDefault() {
    return INTEGRATED;
  }

  public String getType() {
    return type;
  }

  @Override
  public String toString() {
    return ResourceUtils.getBundle().getString(type);
  }
}

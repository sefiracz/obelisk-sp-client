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
    if (type != null && !type.isEmpty()) {
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

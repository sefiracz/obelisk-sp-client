package cz.sefira.obelisk.systray;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.systray.AbstractSystray
 *
 * Created: 03.05.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.Notification;
import cz.sefira.obelisk.api.PlatformAPI;

import java.net.URL;

/**
 * Abstract basic systray support
 */
public abstract class AbstractSystray {

  protected final PlatformAPI api;
  protected final String tooltip;
  protected final URL icon;

  public AbstractSystray(PlatformAPI api, String tooltip, URL icon) {
    this.api = api;
    this.tooltip = tooltip;
    this.icon = icon;
  }

  public abstract void spawnTray(Runnable r, SystrayMenuItem... systrayMenuItems);

  public void pushNotification(Notification notification) {
    api.pushIntegratedNotification(notification);
  }

  public abstract void refreshLabels();

}

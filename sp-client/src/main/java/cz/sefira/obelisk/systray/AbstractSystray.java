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

import java.net.URL;

/**
 * description
 */
public abstract class AbstractSystray {

  protected final String tooltip;
  protected final URL trayIcon;

  public AbstractSystray(String tooltip, URL trayIcon) {
    this.tooltip = tooltip;
    this.trayIcon = trayIcon;
  }

  public abstract void spawnTray(Runnable r);

}

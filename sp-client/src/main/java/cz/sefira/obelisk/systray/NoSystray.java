package cz.sefira.obelisk.systray;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.systray.NoSystray
 *
 * Created: 26.05.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.PlatformAPI;

/**
 * No systray icon and basic notification support
 */
public class NoSystray extends AbstractSystray {

  public NoSystray(PlatformAPI api) {
    super(api, null, null);
  }

  @Override
  public void spawnTray(Runnable r, SystrayMenuItem... systrayMenuItems) {
    // unsupported
  }

  @Override
  public void refreshLabels() {
    // unsupported
  }
}

package cz.sefira.obelisk.systray;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.systray.SystrayMenuItem
 *
 * Created: 24.05.2023
 * Author: hlavnicka
 */

/**
 * description
 */
public class SystrayMenuItem {

  private final String label;
  private final String name;
  private final Runnable runnable;

  public SystrayMenuItem(String label, String name, Runnable runnable) {
    this.label = label;
    this.name = name;
    this.runnable = runnable;
  }

  public String getLabel() {
    return label;
  }

  public String getName() {
    return name;
  }

  public Runnable getOperation() {
    return runnable;
  }
}

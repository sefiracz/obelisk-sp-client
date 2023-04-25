package cz.sefira.obelisk.api.model;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.model.Platform
 *
 * Created: 21.04.2023
 * Author: hlavnicka
 */


/**
 * Returns software platform string
 */
public class Platform {

  public static String get() {
    OS os = OS.getOS();
    switch (os) {
      case LINUX:
        return "linux64";
      case MACOSX:
        return "macos64";
      case WINDOWS:
      default:
        return "win64";
    }
  }

}

package cz.sefira.obelisk.util;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.util.ResourceUtils
 *
 * Created: 24.05.2023
 * Author: hlavnicka
 */

import java.util.ResourceBundle;

/**
 * Resource bundle static getter
 */
public class ResourceUtils {

  public static ResourceBundle getBundle() {
    return ResourceBundle.getBundle("bundles/messages");
  }

}

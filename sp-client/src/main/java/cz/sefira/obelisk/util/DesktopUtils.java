package cz.sefira.obelisk.util;

/*
 * Copyright 2025 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.util.DesktopUtils
 *
 * Created: 22/10/2025
 * Author: hlavnicka
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Desktop actions support
 */
public class DesktopUtils {

  private static final Logger logger = LoggerFactory.getLogger(DesktopUtils.class.getName());

  public static void browse(String uri) {
    if (Desktop.isDesktopSupported()) {
      Desktop desktop = Desktop.getDesktop();
      if (desktop.isSupported(Desktop.Action.BROWSE)) {
        Thread t = new Thread(() -> {
          try {
            desktop.browse(new URI(uri));
          } catch (IOException | URISyntaxException e) {
            logger.error("Unable to open URI '"+uri+"': "+e.getMessage(), e);
          }
        });
        t.setName("desktop-browse");
        t.start();
      } else {
        logger.warn("Desktop action BROWSE is not supported");
      }
    } else {
      logger.warn("Desktop is not supported");
    }
  }


}

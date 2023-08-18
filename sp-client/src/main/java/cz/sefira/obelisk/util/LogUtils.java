package cz.sefira.obelisk.util;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.util.LogUtils
 *
 * Created: 27.04.2023
 * Author: hlavnicka
 */

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * description
 */
public class LogUtils {

  private static final Logger logger = LoggerFactory.getLogger(LogUtils.class.getName());

  public static void setLogLevel(Level logLevel) {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger log = loggerContext.getLogger("ROOT");
    log.setLevel(Level.toLevel(logLevel.levelInt, Level.INFO));
    logger.info("Log level: " + logLevel);
  }

  public static class Time implements AutoCloseable {

    private final long start;
    private final String logText;
    private final boolean debug;

    public Time(String logText) {
      this(logText, false);
    }

    public Time(String logText, boolean debug) {
      this.start = System.currentTimeMillis();
      this.logText = logText;
      this.debug = debug;
    }

    @Override
    public void close() throws Exception {
      if(debug) {
        if (logger.isDebugEnabled()) {
          logger.debug(logText + ": " + (System.currentTimeMillis() - start) + "ms");
        }
      } else {
        logger.info(logText+": "+(System.currentTimeMillis()-start)+"ms");
      }

    }
  }

}

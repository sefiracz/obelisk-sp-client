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

  public static class TimeMeasure implements AutoCloseable {

    private final long start;
    private final String logText;

    public TimeMeasure(String logText) {
      this.start = System.currentTimeMillis();
      this.logText = logText;
    }

    @Override
    public void close() throws Exception {
      logger.info(logText+": "+(System.currentTimeMillis()-start)+"ms");
    }
  }

}

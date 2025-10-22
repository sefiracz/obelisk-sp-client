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
import cz.sefira.obelisk.api.ws.HttpResponseException;
import cz.sefira.obelisk.util.annotation.NotNull;
import org.apache.commons.codec.binary.Base64;
import org.apache.hc.core5.http.Header;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logging utils
 */
public class LogUtils {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(LogUtils.class.getName());

  private static final Set<Integer> logCodes = ConcurrentHashMap.newKeySet();

  /**
   * Helper logging method that prevents repeatable logging of the same messages.
   * Example: logMessage(logger::info, "Log this message", null, disabledLogFlag);
   * @param logger Method reference to the logger
   * @param message Logged message
   * @param t Thrown exception to get logged
   * @param disableLog Flag that disabled repeated log of this message + throwable
   */
  public static void logMessage(LoggingMethod loggingMethod, @NotNull String message, Throwable t, boolean disableLog) {
    int logCode = (message + (t != null ? t.getMessage() : "")).hashCode();
    if (disableLog && !logCodes.contains(logCode)) {
      // disabled log, but message (identified by logCode) has not been logged yet
      logCodes.add(logCode);
      loggingMethod.log(message, t);
    } else if (!disableLog) {
      // enabled log, remove this logCode from Set and log the message
      logCodes.remove(logCode);
      loggingMethod.log(message, t);
    }
    // else do not log anything
  }

  public static void logHttpHeaders(LoggingMethod loggingMethod, int statusCode, String reasonPhrase,
                                    @NotNull Header[] headers) {
    StringBuilder headersBuilder = new StringBuilder(statusCode + " " + reasonPhrase + ":\n");
    for (Header header : headers) {
      headersBuilder.append(header.getName()).append(": ").append(header.getValue()).append("\n");
    }
    loggingMethod.log(headersBuilder.toString(), null);
  }

  /**
   * Log headers and body content of error HTTP response exception
   * @param e Exception carrying details of fault response
   */
  public static void logHttpResponseException(HttpResponseException e) {
    try {
      // log error response headers
      if (e.getHeaders() != null) {
        logHttpHeaders(logger::error, e.getStatusCode(), e.getReasonPhrase(), e.getHeaders());
      }
      // log error response body
      if (e.getContent() != null && e.getContent().length > 0) {
        int maxLen = 16384; // 16kiB
        if (logger.isDebugEnabled()) {
          maxLen = 1024 * 1024; // 1MiB
        }
        logger.error("Body: {}", Base64.encodeBase64String(Arrays.copyOfRange(e.getContent(), 0, Math.min(e.getContent().length, maxLen))));
      }
    } catch (Throwable t) {
      logger.error("Unable to process HttpResponseException: "+t.getMessage(), t);
    }
  }

  @FunctionalInterface
  public interface LoggingMethod {

    void log(String message, Throwable t);

  }

  /**
   * Sets given log level to the ROOT logger
   * @param logLevel Logging level to set to the ROOT logger
   */
  public static void setLogLevel(Level logLevel) {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger log = loggerContext.getLogger("ROOT");
    log.setLevel(Level.toLevel(logLevel.levelInt, Level.INFO));
    logger.info("Log level: " + logLevel);
  }

  /**
   * Calculate execution time of block of code and log message with the time duration
   */
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

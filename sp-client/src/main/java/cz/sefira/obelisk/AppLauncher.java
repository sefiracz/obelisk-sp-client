/**
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package cz.sefira.obelisk;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.AppLauncher
 *
 * Created: 24.03.2021
 * Author: hlavnicka
 */

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.ipc.MessageQueue;
import cz.sefira.obelisk.ipc.MessageQueueFactory;
import cz.sefira.obelisk.api.model.OS;
import cz.sefira.obelisk.process.*;
import cz.sefira.obelisk.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AppLauncher {

  private static final Logger logger = LoggerFactory.getLogger(AppLauncher.class.getName());

  private static AppConfig appConfig;

  public static void main(String[] args) {
    try {
      // main thread name - pid
      Thread t = Thread.currentThread();
      t.setName("main-"+ProcessHandle.current().pid());
      // load config
      appConfig = AppConfig.get();
      // logging mode (debug / info)
      Level logLevel = new UserPreferences(appConfig).isDebugMode() ? Level.DEBUG : Level.INFO;
      setLogLevel(logLevel);
      // add message to the queue
      if (args.length > 0) {
        String input = args[0];
        // TODO - encrypt?
        MessageQueue messageQueue = MessageQueueFactory.getInstance(appConfig);
        String msgId = messageQueue.addMessage(input.getBytes(StandardCharsets.UTF_8));
        logger.info("Queued message: "+msgId);
      } else {
        logger.info("Launcher initiated with no message");
      }
      // check lock
      checkForRunningProcess();
      // start app
      AppPreloader preloader = new AppPreloader();
      preloader.launchApp(args);
    } catch (Exception e) {
      // start failed app
      AppPreloader preloader = new AppPreloader();
      preloader.launchApp(new String[]{"--error=true", "--stacktrace="+TextUtils.printException(e)});
      System.exit(1);
    }
  }

  private static void checkForRunningProcess() throws IOException {
    ProcessHandler handler;
    if (OS.isWindows()) {
      handler = new WindowsProcessHandler();
    } else {
      handler = new UnixProcessHandler();
    }
    ProcessService s = new ProcessService(handler, appConfig);
    s.checkRunning();
  }

  private static void setLogLevel(Level logLevel) {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger log = loggerContext.getLogger("ROOT");
    log.setLevel(Level.toLevel(logLevel.levelInt, Level.INFO));
    logger.info("Log level: "+logLevel);
  }

}

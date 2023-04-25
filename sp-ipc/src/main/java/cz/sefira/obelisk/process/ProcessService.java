package cz.sefira.obelisk.process;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.process.ProcessChecker
 *
 * Created: 06.04.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Process service to handle application launching and running only one live instance
 */
public class ProcessService {

  private static final Logger logger = LoggerFactory.getLogger(ProcessService.class.getName());

  public static final String APP_PID_FILE = "app.pid";

  private static final int TOUCH_PERIOD_MILLIS = 1000;
  private static final int ALIVE_PERIOD_MILLIS = 3000;
  private static final int SLEEP_PERIOD_MILLIS = 3500;

  private static final ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "KeepAlive");
    t.setDaemon(false);
    return t;
  });

  private final ProcessHandler handler;
  private final Path pidFile;

  public ProcessService(ProcessHandler handler, AppConfig config) throws IOException {
    this.handler = handler;
    this.pidFile = config.getAppRunDirectory().resolve(APP_PID_FILE);
  }

  /**
   * Check if app is already running, if true then this instances exits (code 0), otherwise
   * it will keep running.
   * In case the old instance is running, but does not keep refreshing the PID file
   * it will be killed and replaced with this instance.
   */
  public void checkRunning() {
    try {
      long currentPid = ProcessHandle.current().pid();
      // check if PID file exists and created new one
      if (!pidFile.toFile().exists()) {
        try (FileChannel fc = FileChannel.open(pidFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
          logger.debug("Created new PID file: "+pidFile.toFile().getAbsolutePath());
        } catch (IOException e) {
          throw new RuntimeException("Unable to create new PID file: "+e.getMessage(), e);
        }
      }

      // check PID file last touch date
      long lastTouch = getLastTouch(pidFile);
      if (lastTouch < ALIVE_PERIOD_MILLIS) {
        logger.info("PID file looks alive, last touch: "+lastTouch+" ms");
        // pid file is alive, but recheck anyway
        logger.info("Rechecking if app is alive");
        Thread.sleep(SLEEP_PERIOD_MILLIS);
        lastTouch = getLastTouch(pidFile);
        if (lastTouch < ALIVE_PERIOD_MILLIS) {
          exit();
        } else {
          logger.info("PID file is too old after re-check: "+lastTouch+" ms");
        }
      } else {
        logger.info("PID file is too old: "+lastTouch+" ms");
      }

      // check PID file content
      try (FileChannel fc = FileChannel.open(pidFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
          StandardOpenOption.READ);
           FileLock lock = fc.tryLock()) {
        if (lock != null) {
          logger.info("Obtained PID file lock");
          long runningPid = -1;
          int fileSize = (int) fc.size();
          if (fileSize > 0) {
            try {
              runningPid = readPID(fc, fileSize);
            } catch (Exception e) {
              logger.error("Unable to read PID file: "+e.getMessage(), e);
            }
          }
          // check the pid that app runs
          if (runningPid == -1 || !handler.isProccessRunning(runningPid)) {
            logger.info("PID "+runningPid+" not running, keeping this instance.");
            keepAlive(fc, currentPid);
          } else {
            // PID file old and app is running? kill the PID and replace it with this instance
            logger.error("PID "+runningPid+" is presumably running, but PID file is not alive. " +
                "Keeping this instance and killing the old process.");
            handler.killProcess(runningPid);
            keepAlive(fc, currentPid);
          }
        } else {
          logger.error("Unable to obtain pid file lock");
          Thread.sleep(SLEEP_PERIOD_MILLIS);
          checkRunning();
        }
      } finally {
        logger.info("Releasing PID file lock");
      }
    } catch (Exception e) {
      throw new RuntimeException(e); // fatal error
    }
  }

  private long getLastTouch(Path pidFile) throws IOException {
    return System.currentTimeMillis() - Files.getLastModifiedTime(pidFile).toMillis();
  }

  private void keepAlive(FileChannel fc, long pid) throws IOException {
    logger.info("Keeping alive PID: "+pid);
    int b = writePID(fc, pid);
    logger.info("Written bytes to PID file: "+b);
    monitor.scheduleAtFixedRate(() -> {
      try {
        Files.setLastModifiedTime(pidFile, FileTime.from(Instant.now()));
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    }, 0, TOUCH_PERIOD_MILLIS, TimeUnit.MILLISECONDS);
  }

  private long readPID(FileChannel fc, int fileSize) throws IOException {
    ByteBuffer byteBuffer = ByteBuffer.allocate(fileSize);
    fc.read(byteBuffer);
    byteBuffer.flip();
    return Long.parseLong(new String(byteBuffer.array()));
  }

  private int writePID(FileChannel fc, long pid) throws IOException {
    ByteBuffer b = ByteBuffer.wrap(String.valueOf(pid).getBytes(StandardCharsets.UTF_8));
    fc.position(0);
    fc.truncate(0);
    return fc.write(b);
  }

  private void exit() {
    logger.info("App is alive, exiting");
    System.exit(0);
  }

}

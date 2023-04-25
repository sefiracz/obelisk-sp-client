package cz.sefira.obelisk.logging;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.logging.LoggerStartupListener
 *
 * Created: 21.04.2023
 * Author: hlavnicka
 */

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import cz.sefira.obelisk.api.AppConfig;

import java.io.IOException;
import java.util.Properties;

/**
 * description
 */
public class LoggerStartupListener extends ContextAwareBase implements LoggerContextListener, LifeCycle {

  private boolean running = false;

  @Override
  public void start() {
    if (running)
      return;
    // set logger path
    Context context = getContext();
    context.putProperty("APP_USER_HOME", AppConfig.get().getAppUserHome().getAbsolutePath());
    running = true;
  }

  @Override
  public void stop() {
    running = false;
  }

  @Override
  public boolean isStarted() {
    return running;
  }

  @Override
  public boolean isResetResistant() {
    return true;
  }

  @Override
  public void onStart(LoggerContext loggerContext) {
  }

  @Override
  public void onReset(LoggerContext loggerContext) {
  }

  @Override
  public void onStop(LoggerContext loggerContext) {
  }

  @Override
  public void onLevelChange(Logger logger, Level level) {
  }

}

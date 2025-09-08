/**
 * © SEFIRA spol. s r.o., 2020-2025
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
import cz.sefira.obelisk.api.config.AppDataProvider;

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
    context.putProperty("APP_LOG_DIR", AppDataProvider.get().getLogDirPath().toString());
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

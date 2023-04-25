package cz.sefira.obelisk.process;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.launcher.ProcessChecker
 *
 * Created: 06.04.2023
 * Author: hlavnicka
 */

/**
 *  Process checker interface
 */
public interface ProcessHandler {

  /**
   * Check if process identified by given pid is running
   * @param pid Process identifier
   * @return True if running
   */
  boolean isProccessRunning(long pid);

  /**
   * Forcibly terminates process identified by given pid
   * @param pid Process identifier
   * @return True if success
   */
  boolean killProcess(long pid);

}

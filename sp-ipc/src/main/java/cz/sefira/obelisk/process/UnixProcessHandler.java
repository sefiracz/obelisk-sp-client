package cz.sefira.obelisk.process;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.process.UnixProcessChecker
 *
 * Created: 06.04.2023
 * Author: hlavnicka
 */

/**
 * Process handler For UNIX-like operating systems
 */
public class UnixProcessHandler implements ProcessHandler {

  @Override
  public boolean isProccessRunning(long pid) {
    return kill(pid, 0);
  }

  @Override
  public boolean killProcess(long pid) {
    return kill(pid, 9);
  }

  /**
   * Terminate process (specified by a pid) with given signal
   * @param pid Process identifier
   * @param signal Signal number
   * @return True if success
   */
  private boolean kill(long pid, long signal) {
    try {
      Runtime runtime = Runtime.getRuntime();
      Process killProcess = runtime.exec(new String[] { "kill", "-"+signal, String.valueOf(pid) });
      int killProcessExitCode = killProcess.waitFor();
      return killProcessExitCode == 0;
    }
    catch (Exception e) {
      return false;
    }
  }

}

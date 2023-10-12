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

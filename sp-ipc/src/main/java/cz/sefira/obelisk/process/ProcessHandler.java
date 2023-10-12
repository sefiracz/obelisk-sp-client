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

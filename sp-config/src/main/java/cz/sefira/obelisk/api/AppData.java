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
package cz.sefira.obelisk.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class AppData implements ExportableConfig {

  protected String applicationName;
  protected String companyName;
  protected String appId;

  public AppData(String applicationName, String companyName, String appId) {
    this.applicationName = applicationName;
    this.companyName = companyName;
    this.appId = appId;
  }

  /**
   * Directory for log files
   */
  public abstract Path getLogDirPath();

  /**
   * Directory where user preferences property files are stored.
   */
  public abstract Path getUserPreferenceDirPath();

  /**
   * Directory where (customizable by admin/deployer) default app preferences are stored.
   * Does not have to be implemented on certain OS.
   */
  public abstract Path getDefaultPreferencesDirPath();

  /**
   * Directory where app might store some of its own data.
   */
  public abstract Path getAppStorageDirPath();

  /**
   * Directory for IPC message queue.
   */
  public abstract Path getQueueDirPath();

  /**
   * Directory where app puts its PID file for process handling.
   */
  public abstract Path getAppProcessDirPath();

  /**
   * Directory where user data will be stored.
   */
  public abstract Path getUserStorageDirPath();

  /**
   * Directory where the app puts its version file.
   */
  public abstract Path getAppVersionDirPath();

  /**
   * Legacy directory of old versions (might be null on specific OS)
   */
  public abstract Path getLegacyAppHomePath();

  /**
   * Checks and creates directories if they don't already exist
   */
  public void checkAndCreateDirectories(Path path) {
    if (path != null && !Files.exists(path)) {
      try {
        Files.createDirectories(path);
      } catch (IOException e) {
        throw new RuntimeException("Unable to create directory structure: "+e.getMessage(), e);
      }
    }
  }

}

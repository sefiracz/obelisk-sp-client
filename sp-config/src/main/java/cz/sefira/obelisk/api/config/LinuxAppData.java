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
package cz.sefira.obelisk.api.config;

import cz.sefira.obelisk.api.AppData;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class LinuxAppData extends AppData {

  private Path appConfig;

  public LinuxAppData(String applicationName, String companyName) {
    super(applicationName, companyName, null);
    getAppConfigDirectory();
  }

  private Path getAppConfigDirectory() {
    // ~/.config/SEFIRA/OBELISK Signing Portal Client/
    if (appConfig == null) {
      appConfig = Paths.get(getUserHome(), ".config", companyName, applicationName);
      checkAndCreateDirectories(appConfig);
    }
    return appConfig;
  }

  private String getUserHome() {
    return System.getProperty("user.home");
  }

  @Override
  public Path getLogDirPath() {
    // ~/.config/SEFIRA/OBELISK Signing Portal Client/logs
    Path logs = getAppConfigDirectory().resolve("logs");
    checkAndCreateDirectories(logs);
    return logs;
  }

  @Override
  public Path getUserPreferenceDirPath() {
    // ~/.config/SEFIRA/OBELISK Signing Portal Client/
    return getAppConfigDirectory();
  }

  @Override
  public Path getDefaultPreferencesDirPath() {
    return null; // Linux version does not support default preferences
  }

  @Override
  public Path getAppStorageDirPath() {
    // ~/.config/SEFIRA/OBELISK Signing Portal Client/.files/
    Path files = getAppConfigDirectory().resolve(".files");
    checkAndCreateDirectories(files);
    return files;
  }

  @Override
  public Path getQueueDirPath() {
    // ~/.config/SEFIRA/OBELISK Signing Portal Client/.files/queue/
    Path queue = getAppConfigDirectory().resolve(".files").resolve("queue");
    checkAndCreateDirectories(queue);
    return queue;
  }

  @Override
  public Path getAppProcessDirPath() {
    // ~/.config/SEFIRA/OBELISK Signing Portal Client/.files/run
    Path run = getAppConfigDirectory().resolve(".files").resolve("run");
    checkAndCreateDirectories(run);
    return run;
  }

  @Override
  public Path getUserStorageDirPath() {
    // ~/.config/SEFIRA/OBELISK Signing Portal Client/.files/storage
    Path storage = getAppConfigDirectory().resolve(".files").resolve("storage");
    checkAndCreateDirectories(storage);
    return storage;
  }

  @Override
  public Path getAppVersionDirPath() {
    // ~/.config/SEFIRA/OBELISK Signing Portal Client/
    return getAppConfigDirectory();
  }

  @Override
  public Path getLegacyAppHomePath() {
    return null;
  }

  @Override
  public List<Path> exportable() {
    return List.of(getAppConfigDirectory());
  }

}

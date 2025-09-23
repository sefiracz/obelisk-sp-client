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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class WindowsAppData extends AppData {

  private Path appDataDir;
  private Path filesDir;

  public WindowsAppData(String applicationName, String companyName) {
    super(applicationName, companyName, null);
    getUserDataDirectoryPath(applicationName);
  }

  /**
   * Application's local AppData directory
   * @return User data directory C:/Users/<username>/AppData/Local/SEFIRA/OBELISK Signing Portal Client/
   */
  private Path getUserDataDirectoryPath(String appName) {
    if (appDataDir == null) {
      // ~/AppData/Local/
      String localAppData = System.getenv("LocalAppData");
      if (!Files.exists(Paths.get(localAppData))) {
        localAppData = Paths.get(System.getProperty("user.home"), "AppData", "Local").toString();
      }
      appDataDir = Paths.get(localAppData, companyName, appName);
    }
    // create appData dir if it does not exist
    checkAndCreateDirectories(appDataDir);
    return appDataDir;
  }

  private Path getHiddenFilesDirectoryPath() {
    // C:/Users/<username>/AppData/Local/SEFIRA/OBELISK Signing Portal Client/.files
    if (filesDir == null) {
      filesDir = getUserDataDirectoryPath(applicationName).resolve(".files");
      if (!Files.exists(filesDir)) {
        try {
          checkAndCreateDirectories(filesDir);
          Files.setAttribute(filesDir, "dos:hidden", true, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
          throw new RuntimeException("Unable to create directory structure: "+e.getMessage(), e);
        }
      }
    }
    return filesDir;
  }

  @Override
  public Path getLogDirPath() {
    // C:/Users/<username>/AppData/Local/SEFIRA/OBELISK Signing Portal Client/logs
    Path logs = getUserDataDirectoryPath(applicationName).resolve("logs");
    checkAndCreateDirectories(logs);
    return logs;
  }

  @Override
  public Path getUserPreferenceDirPath() {
    // C:/Users/<username>/AppData/Local/SEFIRA/OBELISK Signing Portal Client/
    return getUserDataDirectoryPath(applicationName);
  }

  @Override
  public Path getDefaultPreferencesDirPath() {
    // C:/ProgramData/SEFIRA/OBELISK Signing Portal Client/
    String commonAppData = System.getenv("AllUsersProfile");
    if (commonAppData != null) {
      return Paths.get(commonAppData).resolve(companyName).resolve(applicationName);
    }
    return null; // not found ?
  }

  @Override
  public Path getAppStorageDirPath() {
    // C:/Users/<username>/AppData/Local/SEFIRA/OBELISK Signing Portal Client/.files
    Path files = getHiddenFilesDirectoryPath();
    checkAndCreateDirectories(files);
    return files;
  }


  @Override
  public Path getQueueDirPath() {
    // C:/Users/<username>/AppData/Local/SEFIRA/OBELISK Signing Portal Client/.files/queue
    Path queue = getHiddenFilesDirectoryPath().resolve("queue");
    checkAndCreateDirectories(queue);
    return queue;
  }

  @Override
  public Path getAppProcessDirPath() {
    // C:/Users/<username>/AppData/Local/SEFIRA/OBELISK Signing Portal Client/.files/run
    Path run = getHiddenFilesDirectoryPath().resolve("run");
    checkAndCreateDirectories(run);
    return run;
  }

  @Override
  public Path getUserStorageDirPath() {
    // C:/Users/<username>/AppData/Local/SEFIRA/OBELISK Signing Portal Client/.files/storage
    Path storage = getHiddenFilesDirectoryPath().resolve("storage");
    checkAndCreateDirectories(storage);
    return storage;
  }

  @Override
  public Path getAppVersionDirPath() {
    // C:/Users/<username>/AppData/Local/SEFIRA/OBELISK Signing Portal Client/
    return getUserDataDirectoryPath(applicationName);
  }

  @Override
  public Path getLegacyAppHomePath() {
    // C:/Users/<username>/AppData/Local/SEFIRA/OBELISK Signing Portal/
    return getUserDataDirectoryPath("OBELISK Signing Portal");
  }

  @Override
  public List<Path> exportable() {
    return List.of(getUserDataDirectoryPath(applicationName));
  }
}

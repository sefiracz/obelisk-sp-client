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

public class MacOSAppData extends AppData {

  public MacOSAppData(String applicationName, String companyName, String appId) {
    super(applicationName, companyName, appId);
  }

  private String getUserHome() {
    return System.getProperty("user.home");
  }

  private String getTmpDir() {
    return System.getProperty("java.io.tmpdir");
  }

  private Path getUserLibrary() {
    return Paths.get(getUserHome(), "Library");
  }

  @Override
  public Path getLogDirPath() {
    // ~/Library/Logs/cz.sefira.obelisk.SigningPortal/
    Path logs = getUserLibrary().resolve("Logs").resolve(appId);
    checkAndCreateDirectories(logs);
    return logs;
  }

  @Override
  public Path getUserPreferenceDirPath() {
    // ~/Library/Preferences/cz.sefira.obelisk.SigningPortal/
    Path prefs = getUserLibrary().resolve("Preferences").resolve(appId);
    checkAndCreateDirectories(prefs);
    return prefs;
  }

  @Override
  public Path getDefaultPreferencesDirPath() {
    return null; // macOS version does not support default preferences
  }

  @Override
  public Path getAppStorageDirPath() {
    // ~/Library/cz.sefira.obelisk.SigningPortal/
    Path files = getUserLibrary().resolve(applicationName);
    checkAndCreateDirectories(files);
    return files;
  }

  @Override
  public Path getQueueDirPath() {
    // /var/folder/.../cz.sefira.obelisk.SigningPortal/queue/
    Path queue = Paths.get(getTmpDir(), appId, "queue");
    checkAndCreateDirectories(queue);
    return queue;
  }

  @Override
  public Path getAppProcessDirPath() {
    // /var/folder/.../cz.sefira.obelisk.SigningPortal/run/
    Path run = Paths.get(getTmpDir(), appId, "run");
    checkAndCreateDirectories(run);
    return run;
  }

  @Override
  public Path getUserStorageDirPath() {
    // ~/Library/cz.sefira.obelisk.SigningPortal/storage/
    Path storage = getUserLibrary().resolve(applicationName).resolve("storage");
    checkAndCreateDirectories(storage);
    return storage;
  }

  @Override
  public Path getAppVersionDirPath() {
    // ~/Library/cz.sefira.obelisk.SigningPortal/
    Path version = getUserLibrary().resolve(applicationName);
    checkAndCreateDirectories(version);
    return version;
  }

  @Override
  public Path getLegacyAppHomePath() {
    return null;
  }

  @Override
  public List<Path> exportable() {
    return List.of(getLogDirPath(), getUserPreferenceDirPath(), getAppVersionDirPath());
  }

}

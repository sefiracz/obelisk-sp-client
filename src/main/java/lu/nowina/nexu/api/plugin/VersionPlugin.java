/**
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package lu.nowina.nexu.api.plugin;

import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.view.DialogMessage;
import lu.nowina.nexu.view.StandaloneDialog;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class VersionPlugin implements NexuPlugin {

  private static final Logger logger = LoggerFactory.getLogger(VersionPlugin.class.getName());

  @Override
  public List<InitializationMessage> init(String pluginId, NexuAPI api) {
    try {
      final File nexuHome = api.getAppConfig().getNexuHome();
      final String currentVersion = api.getAppConfig().getApplicationVersion();

      final File versionFile = new File(nexuHome, "version");
      // first test if file exists
      if (!versionFile.exists()) {
        writeVersion(currentVersion, versionFile);
      } else {
        // get stored version
        String storedVersion = readVersion(versionFile);
        // check correct version
        if (!currentVersion.equals(storedVersion)) {
          // analyze update path
//          boolean needsUpgrade = false; // TODO analyze upgrade needs and figure out from->to upgrade path
//          if(needsUpgrade) {
//            // TODO - safely make data transformations step by step to current version according to upgrade path
//          }
          // change version number
          writeVersion(currentVersion, versionFile);
        }
      }
    } catch (Exception e) {
      logger.error("Exception when trying to process version value", e);
      StandaloneDialog.showDialog(api,
              new DialogMessage("version.error.message", DialogMessage.Level.ERROR), true);
      System.exit(1);
    }
    return Collections.emptyList();
  }

  /**
   * Read version value from version file
   * @param versionFile File with version value
   * @return Version number
   * @throws IOException
   */
  private String readVersion(File versionFile) throws IOException {
    try (InputStream in = new FileInputStream(versionFile)) {
      return IOUtils.toString(in, StandardCharsets.UTF_8); // read version value from version file
    }
  }

  /**
   * Write new version number to version file
   * @param version Version number
   * @param versionFile Version file
   */
  private void writeVersion(String version, File versionFile) throws IOException {
    try (OutputStream out = new FileOutputStream(versionFile)) {
      out.write(version.getBytes(StandardCharsets.UTF_8)); // write new version to version file
    }
  }

}

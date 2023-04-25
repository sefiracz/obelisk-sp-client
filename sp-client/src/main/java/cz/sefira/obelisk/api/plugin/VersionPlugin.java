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
package cz.sefira.obelisk.api.plugin;

import cz.sefira.obelisk.ProductStorage;
import cz.sefira.obelisk.api.AbstractProduct;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.plugin.version.LegacyProductStorage;
import cz.sefira.obelisk.view.DialogMessage;
import cz.sefira.obelisk.view.StandaloneDialog;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

public class VersionPlugin implements AppPlugin {

  private static final Logger logger = LoggerFactory.getLogger(VersionPlugin.class.getName());

  @Override
  public List<InitErrorMessage> init(String pluginId, PlatformAPI api) {
    try {
      final File appUserHome = api.getAppConfig().getAppUserHome();
      final String currentVersion = api.getAppConfig().getApplicationVersion();
      logger.info("Current version: "+currentVersion);
      final File versionFile = new File(appUserHome, "version");
      // first test if file exists
      if (!versionFile.exists()) {
        migrateLegacyVersionDatabase(api);
        writeVersion(currentVersion, versionFile);
      } else {
        // get stored version
        String storedVersion = readVersion(versionFile);
        // check correct version
        if (!currentVersion.equals(storedVersion)) {
          // analyze update path
          versionUpgrade(api, storedVersion, currentVersion);
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

  private void migrateLegacyVersionDatabase(PlatformAPI api) {
    final File legacyHome = api.getAppConfig().getLegacyAppUserHome();
    if (legacyHome != null && legacyHome.exists()) {
      LegacyProductStorage legacyStorage = new LegacyProductStorage();
      ProductStorage<AbstractProduct> storage = api.getProductStorage(AbstractProduct.class);
      for (AbstractProduct p : legacyStorage.load(legacyHome.toPath())) {
        storage.add(p);
      }
    }
  }

  private void versionUpgrade(PlatformAPI api, String storedVersion, String currentVersion) {
    // NEXT VERSION MIGRATION CODE
    // TODO
  }

  /**
   * Read version value from version file
   * @param versionFile File with version value
   * @return Version number
   * @throws IOException
   */
  private String readVersion(File versionFile) throws IOException {
    try (InputStream in = Files.newInputStream(versionFile.toPath())) {
      return IOUtils.toString(in, StandardCharsets.UTF_8); // read version value from version file
    }
  }

  /**
   * Write new version number to version file
   * @param version Version number
   * @param versionFile Version file
   */
  private void writeVersion(String version, File versionFile) throws IOException {
    try (OutputStream out = Files.newOutputStream(versionFile.toPath())) {
      out.write(version.getBytes(StandardCharsets.UTF_8)); // write new version to version file
    }
  }

}

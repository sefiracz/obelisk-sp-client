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
package cz.sefira.obelisk.prefs;

/*
 * Copyright 2025 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.prefs.ZipExportStream
 *
 * Created: 08/09/2025
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.config.AppDataProvider;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Export zipped app data to output
 */
public class ZipExport implements AutoCloseable {

  private final ZipOutputStream zos;

  public ZipExport(OutputStream out) throws IOException {
    zos = new ZipOutputStream(out, StandardCharsets.UTF_8);
    zos.setMethod(ZipOutputStream.DEFLATED);
    zos.setLevel(Deflater.BEST_COMPRESSION);
  }

  public void zipDirectory(File folder, String parentFolder)
      throws IOException {
    File[] list = folder.listFiles();
    if (list != null) {
      for (File file : list) {
        // skip all webview profile files
        if (file.isDirectory() && (file.getName().startsWith("jcef") || file.getName().startsWith("webview"))
            && file.getParentFile().getName().equals(AppDataProvider.get().getAppStorageDirPath().getFileName().toString())) {
          continue;
        }
        // replace user-preferences with current runtime values (and redact any secrets) and skip the actual file
        if (file.getName().equals("user-preferences.properties")) {
          UserPreferences prefs = PreferencesFactory.getInstance(AppConfig.get());
          zos.putNextEntry(new ZipEntry(parentFolder + "/" + file.getName()));
          zos.write(prefs.exportValues().getBytes(StandardCharsets.UTF_8));
          zos.closeEntry();
          continue;
        }
        if (file.isDirectory()) {
          zipDirectory(file, parentFolder + "/" + file.getName());
        }
        else {
          zos.putNextEntry(new ZipEntry(parentFolder + "/" + file.getName()));
          try (InputStream f = Files.newInputStream(file.toPath())) {
            IOUtils.copy(f, zos);
          }
          zos.closeEntry();
        }
      }
    }
  }

  @Override
  public void close() throws IOException {
    zos.flush();
    zos.close();
  }

}

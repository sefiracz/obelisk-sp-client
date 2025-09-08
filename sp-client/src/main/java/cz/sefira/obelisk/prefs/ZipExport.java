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
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Export zipped app data to output
 */
public class ZipExport implements AutoCloseable {

  private final OutputStream out;

  private final ZipParameters zipParams;
  private final ByteArrayOutputStream baos;
  private final ZipOutputStream zos;

  public ZipExport(OutputStream out) throws IOException {
    this.out = out;
    zipParams = new ZipParameters();
    zipParams.setCompressionMethod(CompressionMethod.DEFLATE);
    zipParams.setCompressionLevel(CompressionLevel.MAXIMUM);
    baos = new ByteArrayOutputStream();
    zos = new ZipOutputStream(baos, StandardCharsets.UTF_8);
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
          zipParams.setFileNameInZip(parentFolder + "/" + file.getName());
          zos.putNextEntry(zipParams);
          zos.write(prefs.exportValues().getBytes(StandardCharsets.UTF_8));
          zos.closeEntry();
          continue;
        }
        if (file.isDirectory()) {
          zipDirectory(file, parentFolder + "/" + file.getName());
        }
        else {
          zipParams.setFileNameInZip(parentFolder + "/" + file.getName());
          zos.putNextEntry(zipParams);
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
    out.write(baos.toByteArray());
    out.flush();
    out.close();
  }

}

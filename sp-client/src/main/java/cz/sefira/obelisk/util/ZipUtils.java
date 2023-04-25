package cz.sefira.obelisk.util;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.util.ZipUtils
 *
 * Created: 24.03.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.util.annotation.NotNull;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * ZIP utility methods
 */
public class ZipUtils {

  public static byte[] zipDirectory(@NotNull File directory, @NotNull String name,
                                    @NotNull String prefs, char[] password) throws IOException {
    ZipParameters zipParams = new ZipParameters();
    zipParams.setCompressionMethod(CompressionMethod.DEFLATE);
    zipParams.setCompressionLevel(CompressionLevel.MAXIMUM);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
      // write AppHome directory
      zipDirectory(directory, directory.getName(), zos, zipParams);
      // write user preferences
      zipUserPrefs(prefs.getBytes(StandardCharsets.UTF_8), directory.getName(), zos, zipParams);
    }
    // return if no encryption
    if (password == null) {
      return baos.toByteArray();
    }
    // return encrypted
    return zipAndEncrypt(name, baos.toByteArray(), password);
  }

  private static void zipDirectory(File folder, String parentFolder, ZipOutputStream zos, ZipParameters zipParams)
      throws IOException {
    File[] list = folder.listFiles();
    if (list != null) {
      for (File file : list) {
        if (file.isDirectory()) {
          zipDirectory(file, parentFolder + "/" + file.getName(), zos, zipParams);
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

  private static void zipUserPrefs(byte[] prefs, String parentFolder, ZipOutputStream zos, ZipParameters zipParams)
      throws IOException {
    zipParams.setFileNameInZip(parentFolder + "/config.properties");
    zos.putNextEntry(zipParams);
    zos.write(prefs);
    zos.closeEntry();
  }

  private static byte[] zipAndEncrypt(String name, byte[] data, char[] password) throws IOException {
    ZipParameters zipParams = new ZipParameters();
    zipParams.setCompressionMethod(CompressionMethod.DEFLATE);
    zipParams.setCompressionLevel(CompressionLevel.MAXIMUM);
    zipParams.setEncryptFiles(true);
    zipParams.setEncryptionMethod(EncryptionMethod.AES);
    zipParams.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(baos, password, StandardCharsets.UTF_8)) {
      zipParams.setFileNameInZip(name);
      zos.putNextEntry(zipParams);
      zos.write(data);
    }
    return baos.toByteArray();
  }

}

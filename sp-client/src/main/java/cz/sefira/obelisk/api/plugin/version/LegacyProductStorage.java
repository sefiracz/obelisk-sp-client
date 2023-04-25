package cz.sefira.obelisk.api.plugin.version;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.plugin.version.LegacyStorage
 *
 * Created: 17.04.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AbstractProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only legacy product storage
 */
public class LegacyProductStorage {

  private static final Logger logger = LoggerFactory.getLogger(LegacyProductStorage.class.getName());

  public List<AbstractProduct> load(Path legacyHome) {
    List<AbstractProduct> products = new ArrayList<>();
    products.addAll(loadDatabase(legacyHome.resolve("database-windows.xml")));
    products.addAll(loadDatabase(legacyHome.resolve("database-keystore.xml")));
    products.addAll(loadDatabase(legacyHome.resolve("database-smartcard.xml")));
    products.addAll(loadDatabase(legacyHome.resolve("database-macos.xml")));
    return products;
  }

  private List<AbstractProduct> loadDatabase(Path databaseFile) {
    List<AbstractProduct> products = new ArrayList<>();
    if (databaseFile.toFile().exists() && databaseFile.toFile().canRead()) {
      try (InputStream in = Files.newInputStream(databaseFile)) {
        LegacyDatabaseHandler handler = new LegacyDatabaseHandler();
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        saxParser.parse(in, handler);
        List<AbstractProduct> migration = handler.getProducts();
        logger.info("Migrating "+databaseFile.getFileName().toString()+" product database, records found: "+migration.size());
        products.addAll(migration);
      } catch (Exception e) {
        logger.error("Failed to migrate "+databaseFile.getFileName().toString(), e);
        return products;
      }
    }
    return products;
  }

}

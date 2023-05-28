package cz.sefira.obelisk.storage;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.storage.StorageHandler
 *
 * Created: 27.05.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AppConfig;
import one.microstream.persistence.types.PersistenceRefactoringMappingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Storage handler
 */
public class StorageHandler implements Closeable {

  private static final Logger logger = LoggerFactory.getLogger(StorageHandler.class.getName());

  private final ProductStorage<?> productStorage;
  private final SmartcardStorage smartcardStorage;
  private final EventsStorage eventsStorage;
  private final SSLCacheStorage sslCacheStorage;

  public StorageHandler() throws IOException {
    Path storage = AppConfig.get().getAppStorageDirectory();
    productStorage = new ProductStorage<>(storage.resolve("products"));
    smartcardStorage = new SmartcardStorage(storage.resolve("smartcards"));
    eventsStorage = new EventsStorage(storage.resolve("events"));
    sslCacheStorage = new SSLCacheStorage(storage.resolve("sslCache"));
  }

  public ProductStorage<?> getProductStorage() {
    return productStorage;
  }

  public SmartcardStorage getSmartcardStorage() {
    return smartcardStorage;
  }

  public EventsStorage getEventsStorage() {
    return eventsStorage;
  }

  public SSLCacheStorage getSslCacheStorage() {
    return sslCacheStorage;
  }

  @Override
  public void close() {
    logger.info("Stopping products storage");
    productStorage.close();
    logger.info("Stopping smartcards storage");
    smartcardStorage.close();
    logger.info("Stopping events storage");
    eventsStorage.close();
    logger.info("Stopping SSL cache storage");
    sslCacheStorage.close();
  }
}

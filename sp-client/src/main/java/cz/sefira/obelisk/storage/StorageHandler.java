/**
 * © SEFIRA spol. s r.o., 2020-2023
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
    sslCacheStorage = new SSLCacheStorage(storage.resolve("ssl"));
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

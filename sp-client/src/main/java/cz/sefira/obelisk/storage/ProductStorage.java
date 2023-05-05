package cz.sefira.obelisk.storage;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.storage.ProductStorage
 *
 * Created: 06.03.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AbstractProduct;
import cz.sefira.obelisk.generic.QuickAccessProductsMap;
import cz.sefira.obelisk.util.annotation.NotNull;
import one.microstream.persistence.internal.LoggingLegacyTypeMappingResultor;
import one.microstream.persistence.types.PersistenceLegacyTypeMappingResultor;
import one.microstream.persistence.types.Storer;
import one.microstream.storage.embedded.types.EmbeddedStorage;
import one.microstream.storage.embedded.types.EmbeddedStorageFoundation;
import one.microstream.storage.embedded.types.EmbeddedStorageManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Product (location of certificate+key in device) storage
 */
public class ProductStorage<T> implements AutoCloseable {

  private final List<AbstractProduct> products = new ArrayList<>();

  private final EmbeddedStorageManager storage;

  public ProductStorage(Path store) {
    EmbeddedStorageFoundation<?> foundation = EmbeddedStorage.Foundation(store);
    foundation.getConnectionFoundation().setLegacyTypeMappingResultor(
        LoggingLegacyTypeMappingResultor.New(
            PersistenceLegacyTypeMappingResultor.New()
        )
    );
    this.storage = foundation.createEmbeddedStorageManager(products).start();
    for (AbstractProduct product : products) {
      QuickAccessProductsMap.access().put(product.getCertificateId(), product);
    }
  }

  public synchronized void add(@NotNull AbstractProduct product) {
    if (!getProducts().contains(product)) {
      getProducts().add(product);
      QuickAccessProductsMap.access().put(product.getCertificateId(), product);
      commitChange();
    }
  }

  public synchronized void remove(@NotNull AbstractProduct product) {
    getProducts().remove(product);
    QuickAccessProductsMap.access().remove(product.getCertificateId(), product);
    commitChange();
  }

  public List<T> getProducts(Class<T> cl) {
    List<T> list = new ArrayList<>();
    for (AbstractProduct p : getProducts()) {
      if (p.getClass() == cl) {
        list.add(cl.cast(p));
      }
    }
    return list;
  }

  public T getProduct(@NotNull T product) {
    List<T> list = getProducts((Class<T>) product.getClass());
    int idx = list.indexOf(product);
    if (idx != -1) {
      return list.get(idx);
    }
    return null;
  }

  private List<AbstractProduct> getProducts() {
    return products;
  }

  private void commitChange() {
    Storer storer = storage.createEagerStorer();
    storer.store(getProducts());
    storer.commit();
  }

  @Override
  public void close() {
    try {
      storage.close();
      storage.shutdown();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
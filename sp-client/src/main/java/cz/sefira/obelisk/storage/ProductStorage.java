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
import one.microstream.storage.embedded.types.EmbeddedStorage;
import one.microstream.storage.embedded.types.EmbeddedStorageFoundation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Product (location of certificate+key in device) storage
 */
public class ProductStorage<T> extends AbstractStorage {

  private static final Logger logger = LoggerFactory.getLogger(ProductStorage.class.getName());

  private final List<AbstractProduct> products = new ArrayList<>();

  public ProductStorage(Path store) {
    EmbeddedStorageFoundation<?> foundation = EmbeddedStorage.Foundation(store);
    foundation.getConnectionFoundation().setLegacyTypeMappingResultor(
        LoggingLegacyTypeMappingResultor.New(
            PersistenceLegacyTypeMappingResultor.New()
        )
    );
    this.storage = foundation.createEmbeddedStorageManager(products).start();
    logger.info("Product storage size: "+products.size());
    for (AbstractProduct product : products) {
      QuickAccessProductsMap.access().put(product.getCertificateId(), product);
    }
  }

  public synchronized void add(@NotNull AbstractProduct product) {
    if (!getProducts().contains(product)) {
      getProducts().add(product);
      QuickAccessProductsMap.access().put(product.getCertificateId(), product);
      commitChange(getProducts());
    }
  }

  public synchronized void remove(@NotNull AbstractProduct product) {
    getProducts().remove(product);
    QuickAccessProductsMap.access().remove(product.getCertificateId(), product);
    commitChange(getProducts());
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

}
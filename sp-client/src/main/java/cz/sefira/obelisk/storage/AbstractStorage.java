package cz.sefira.obelisk.storage;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.storage.AbstractStorage
 *
 * Created: 27.05.2023
 * Author: hlavnicka
 */

import one.microstream.persistence.types.PersistenceRefactoringMappingProvider;
import one.microstream.persistence.types.Storer;
import one.microstream.storage.embedded.types.EmbeddedStorageManager;

/**
 * Abstract embedded storage
 */
public abstract class AbstractStorage implements AutoCloseable {

  protected EmbeddedStorageManager storage;

  protected void commitChange(Object root) {
    Storer storer = storage.createEagerStorer();
    storer.store(root);
    storer.commit();
  }

  private PersistenceRefactoringMappingProvider createMappings() {
    // example refactoring mapping (package change)
    //KeyValue<String, String> mapping = KeyValue.New("cz.sefira.obelisk.storage.EventsRoot", "cz.sefira.obelisk.storage.model.EventsRoot");
    //XGettingTable<String, String> mappings = HashTable.New(mapping);
    //return PersistenceRefactoringMappingProvider.New(mappings);
    return null;
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

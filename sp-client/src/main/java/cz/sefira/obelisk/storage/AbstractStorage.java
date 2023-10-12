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

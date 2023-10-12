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
 * cz.sefira.obelisk.storage.ProductStorage
 *
 * Created: 06.03.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.ws.model.SmartcardInfo;
import one.microstream.persistence.internal.LoggingLegacyTypeMappingResultor;
import one.microstream.persistence.types.PersistenceLegacyTypeMappingResultor;
import one.microstream.storage.embedded.types.EmbeddedStorage;
import one.microstream.storage.embedded.types.EmbeddedStorageFoundation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Supported smartcards storage
 */
public class SmartcardStorage extends AbstractStorage {

  private static final Logger logger = LoggerFactory.getLogger(SmartcardStorage.class.getName());

  private final List<SmartcardInfo> smartcards = new ArrayList<>();

  public SmartcardStorage(Path store) {
    EmbeddedStorageFoundation<?> foundation = EmbeddedStorage.Foundation(store);
    foundation.getConnectionFoundation().setLegacyTypeMappingResultor(
        LoggingLegacyTypeMappingResultor.New(
            PersistenceLegacyTypeMappingResultor.New()
        )
    );
    this.storage = foundation.createEmbeddedStorageManager(smartcards).start();
    logger.info("Supported smartcards: "+smartcards.size());
  }

  public final synchronized void setSmartcards(List<SmartcardInfo> smartcardList) {
    for (SmartcardInfo smartcard : smartcardList) {
      int index = smartcards.indexOf(smartcard);
      // this ATR is stored
      if (index > -1) {
        SmartcardInfo stored = smartcards.get(index);
        // but info differs
        if (!stored.compare(smartcard)) {
          // replace the stored value with the new one
          smartcards.remove(stored);
          smartcards.add(smartcard);
        }
      } else {
        smartcards.add(smartcard);
      }
    }
    commitChange(this.smartcards);
  }

  public Map<String, SmartcardInfo> getSmartcardInfosMap() {
    Map<String, SmartcardInfo> infosMap = new ConcurrentHashMap<>();
    for(SmartcardInfo smartcardInfo : smartcards) {
      infosMap.put(smartcardInfo.getAtr(), smartcardInfo);
    }
    return infosMap;
  }

}
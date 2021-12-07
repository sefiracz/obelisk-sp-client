/**
 * © SEFIRA spol. s r.o., 2020-2021
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
package cz.sefira.obelisk.macos.keystore;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.macos.keystore.MacOSKeystoreDatabase
 *
 * Created: 06.12.2021
 * Author: hlavnicka
 */

import cz.sefira.obelisk.DatabaseEventHandler;
import cz.sefira.obelisk.ProductDatabase;
import cz.sefira.obelisk.api.AbstractProduct;
import cz.sefira.obelisk.api.NexuAPI;
import cz.sefira.obelisk.generic.RegisteredProducts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * description
 */

@XmlRootElement(name = "database")
@XmlAccessorType(XmlAccessType.FIELD)
public class MacOSKeychainDatabase implements ProductDatabase {

  private static final Logger LOGGER = LoggerFactory.getLogger(MacOSKeychainDatabase.class.getName());

  @XmlElement(name = "keychain")
  private List<MacOSKeychain> keystores;

  @XmlTransient
  private DatabaseEventHandler onAddRemoveAction;

  /**
   * Adds a new {@link MacOSKeychain} to the database.
   *
   * @param keystore The keystore to add.
   */
  public final void add(final MacOSKeychain keystore) {
    if (!getKeystores().contains(keystore)) {
      getKeystores().add(keystore);
    }
    RegisteredProducts.getMap().put(keystore.getCertificateId(), keystore);
    onAddRemove();
  }


  private void onAddRemove() {
    if (onAddRemoveAction != null) {
      onAddRemoveAction.execute(this);
    }
    else {
      LOGGER.warn("No DatabaseEventHandler define, the database cannot be stored");
    }
  }

  @Override
  public void initialize() {
    for (MacOSKeychain keystore : getKeystores()) {
      RegisteredProducts.getMap().put(keystore.getCertificateId(), keystore);
    }
  }

  @Override
  public void setOnAddRemoveAction(DatabaseEventHandler eventHandler) {
    this.onAddRemoveAction = eventHandler;
  }


  @Override
  public void remove(NexuAPI api, AbstractProduct product) {

  }

  private List<MacOSKeychain> getKeystores() {
    if (keystores == null) {
      this.keystores = new ArrayList<>();
    }
    return keystores;
  }

  @Override
  public List<AbstractProduct> getProducts() {
    return Collections.unmodifiableList(getKeystores());
  }
}

/**
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Licensed under EUPL Version 1.1 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package lu.nowina.nexu.windows.keystore;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.windows.keystore.WindowsKeystoreDatabase
 *
 * Created: 05.01.2021
 * Author: hlavnicka
 */

import lu.nowina.nexu.DatabaseEventHandler;
import lu.nowina.nexu.ProductDatabase;
import lu.nowina.nexu.api.AbstractProduct;
import lu.nowina.nexu.api.ConfiguredKeystore;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.generic.RegisteredProducts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@XmlRootElement(name = "database")
@XmlAccessorType(XmlAccessType.FIELD)
public class WindowsKeystoreDatabase implements ProductDatabase {

  private static final Logger LOGGER = LoggerFactory.getLogger(WindowsKeystoreDatabase.class.getName());

  @XmlElement(name = "winkeystore")
  private List<WindowsKeystore> keystores;

  @XmlTransient
  private DatabaseEventHandler onAddRemoveAction;

  /**
   * Adds a new {@link ConfiguredKeystore} to the database.
   * @param keystore The keystore to add.
   */
  public final void add(final WindowsKeystore keystore) {
    if(!getKeystores().contains(keystore)) {
      getKeystores().add(keystore);
    }
    RegisteredProducts.getMap().put(keystore.getCertificateId(), keystore);
    onAddRemove();
  }

  /**
   * Removes the given {@link ConfiguredKeystore} from the database.
   * @param keystore The keystore to remove.
   */
  public final void remove(NexuAPI api, final AbstractProduct keystore) {
    getKeystores().remove(keystore);
    RegisteredProducts.getMap().remove(keystore.getCertificateId(), keystore);
    onAddRemove();
  }

  private void onAddRemove() {
    if(onAddRemoveAction != null) {
      onAddRemoveAction.execute(this);
    } else {
      LOGGER.warn("No DatabaseEventHandler define, the database cannot be stored");
    }
  }

  private List<WindowsKeystore> getKeystores() {
    if (keystores == null) {
      this.keystores = new ArrayList<>();
    }
    return keystores;
  }

  public List<AbstractProduct> getProducts() {
    return Collections.unmodifiableList(getKeystores());
  }

  @Override
  public void setOnAddRemoveAction(DatabaseEventHandler eventHandler) {
    this.onAddRemoveAction = eventHandler;
  }

  /**
   * Initialize runtime HashMap of CertificateId to configured Products
   */
  public void initialize() {
    for (WindowsKeystore keystore : getKeystores()) {
      RegisteredProducts.getMap().put(keystore.getCertificateId(), keystore);
    }
  }
}

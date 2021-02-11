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
import lu.nowina.nexu.generic.ProductsMap;
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
    if(!getKeystores0().contains(keystore)) {
      getKeystores0().add(keystore);
    }
    ProductsMap.getMap().put(keystore.getCertificateId(), keystore);
    onAddRemove();
  }

  /**
   * Removes the given {@link ConfiguredKeystore} from the database.
   * @param keystore The keystore to remove.
   */
  public final void remove(final AbstractProduct keystore) {
    getKeystores0().remove(keystore);
    ProductsMap.getMap().remove(keystore.getCertificateId(), keystore);
    onAddRemove();
  }

  private void onAddRemove() {
    if(onAddRemoveAction != null) {
      onAddRemoveAction.execute(this);
    } else {
      LOGGER.warn("No DatabaseEventHandler define, the database cannot be stored");
    }
  }

  private List<WindowsKeystore> getKeystores0() {
    if (keystores == null) {
      this.keystores = new ArrayList<>();
    }
    return keystores;
  }

  public List<AbstractProduct> getKeystores() {
    return Collections.unmodifiableList(getKeystores0());
  }

  @Override
  public void setOnAddRemoveAction(DatabaseEventHandler eventHandler) {
    this.onAddRemoveAction = eventHandler;
  }

  /**
   * Initialize runtime HashMap of CertificateId to configured Products
   */
  public void initialize() {
    for (WindowsKeystore keystore : getKeystores0()) {
      ProductsMap.getMap().put(keystore.getCertificateId(), keystore);
    }
  }
}

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
package lu.nowina.nexu.generic;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.generic.RegisteredProducts
 *
 * Created: 08.01.2021
 * Author: hlavnicka
 */

import lu.nowina.nexu.api.AbstractProduct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Map of all stored keystores (certificate/key)
 */
public class RegisteredProducts {

  private static volatile RegisteredProducts products;

  private final Map<String, List<AbstractProduct>> map;

  private RegisteredProducts() {
    map = new HashMap<>();
  }

  public synchronized static RegisteredProducts getMap() {
    if(products == null) {
      products = new RegisteredProducts();
    }
    return products;
  }

  public synchronized void put(String certificateId, AbstractProduct keystore) {
    List<AbstractProduct> list = map.get(certificateId);
    if(list == null) {
      List<AbstractProduct> empty = new ArrayList<>();
      empty.add(keystore);
      map.put(certificateId, empty);
    } else if (!list.contains(keystore)) {
      list.add(keystore);
      map.put(certificateId, list);
    }
  }

  public synchronized void remove(String certificateId, AbstractProduct keystore) {
    List<AbstractProduct> list = map.get(certificateId);
    list.remove(keystore);
    map.put(certificateId, list);
  }

  public List<AbstractProduct> get(String certificateId) {
    return map.get(certificateId);
  }

  public List<AbstractProduct> getAllProducts() {
    List<AbstractProduct> all = new ArrayList<>();
    for(List<AbstractProduct> value : map.values()) {
      all.addAll(value);
    }
    return all;
  }

}

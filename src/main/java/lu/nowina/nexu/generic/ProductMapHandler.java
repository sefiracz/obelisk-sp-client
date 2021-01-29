package lu.nowina.nexu.generic;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.generic.KeystoresMap
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
public class ProductMapHandler {

  private static volatile ProductMapHandler handler;

  private final Map<String, List<AbstractProduct>> map;

  private ProductMapHandler() {
    map = new HashMap<>();
  }

  public synchronized static ProductMapHandler getInstance() {
    if(handler == null) {
      handler = new ProductMapHandler();
    }
    return handler;
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

  public List<AbstractProduct> getAll() {
    List<AbstractProduct> all = new ArrayList<>();
    for(List<AbstractProduct> value : map.values()) {
      all.addAll(value);
    }
    return all;
  }



}

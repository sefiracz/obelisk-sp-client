package lu.nowina.nexu.flow.operation;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.flow.operation.SelectProductOperation
 *
 * Created: 04.01.2021
 * Author: hlavnicka
 */

import lu.nowina.nexu.api.AbstractProduct;
import lu.nowina.nexu.api.ConfiguredKeystore;
import lu.nowina.nexu.api.KeystoreType;
import lu.nowina.nexu.api.Product;
import lu.nowina.nexu.api.flow.OperationResult;

/**
 * description
 */
public class SelectProductOperation extends AbstractCompositeOperation<Product> {

  private AbstractProduct abstractProduct;

  @Override
  public void setParams(Object... params) {
    abstractProduct = (AbstractProduct) params[0];
  }

  @Override
  public OperationResult<Product> perform() {
    return new OperationResult<Product>(abstractProduct);
  }
}

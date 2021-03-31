/**
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package lu.nowina.nexu.flow.operation;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.flow.operation.SaveFullSelectionOperation
 *
 * Created: 04.01.2021
 * Author: hlavnicka
 */

import eu.europa.esig.dss.DigestAlgorithm;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.KSPrivateKeyEntry;
import lu.nowina.nexu.Utils;
import lu.nowina.nexu.api.*;
import lu.nowina.nexu.api.flow.OperationResult;
import lu.nowina.nexu.pkcs11.IAIKPrivateKeyEntry;
import org.apache.commons.codec.binary.Base64;

import java.util.Map;

/**
 * Saves entirely configured product with certificate information and key name
 * to be able to select the key in the future without/minimal user interaction
 *
 * <p>Expected parameters:
 *  <ol>
 *  <li>{@link Product}</li>
 *  <li>{@link ProductAdapter}</li>
 *  <li>{@link DSSPrivateKeyEntry}</li>
 *  <li>{@link Map}</li>
 *  </ol>
 * </p>
 */
public class SaveFullSelectionOperation extends AbstractCompositeOperation<Boolean> {

  private AbstractProduct product;
  private ProductAdapter productAdapter;
  private DSSPrivateKeyEntry key;
  private Map<TokenOperationResultKey, Object> map;

  @Override
  @SuppressWarnings("unchecked")
  public void setParams(Object... params) {
    try {
      this.product = (AbstractProduct) params[0];
      this.productAdapter = (ProductAdapter) params[1];
      this.key = (DSSPrivateKeyEntry) params[2];
      this.map = (Map<TokenOperationResultKey, Object>) params[3];
    } catch(final ClassCastException | ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Expected parameters: Product, ProductAdapter, Key, Map");
    }
  }

  @Override
  public OperationResult<Boolean> perform() {
    byte[] id = key.getCertificate().getDigest(DigestAlgorithm.SHA256);
    product.setCertificateId(Utils.encodeHexString(id));
    if (key instanceof KSPrivateKeyEntry) {
      product.setKeyAlias(((KSPrivateKeyEntry) key).getAlias());
    }
    if (key instanceof IAIKPrivateKeyEntry) {
      product.setKeyAlias(((IAIKPrivateKeyEntry) key).getKeyLabel());
    }
    product.setCertificate(Base64.encodeBase64String(key.getCertificate().getEncoded()));
    // save fully-configured product
    productAdapter.saveProduct(product, map);
    return new OperationResult<>(true);
  }
}

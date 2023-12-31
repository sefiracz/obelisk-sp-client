/**
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package cz.sefira.obelisk.flow.operation;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.flow.operation.SaveFullSelectionOperation
 *
 * Created: 04.01.2021
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.Product;
import cz.sefira.obelisk.api.ProductAdapter;
import cz.sefira.obelisk.api.flow.OperationResult;
import cz.sefira.obelisk.token.macos.KeychainPrivateKey;
import cz.sefira.obelisk.token.pkcs11.IAIKPrivateKeyEntry;
import cz.sefira.obelisk.dss.DigestAlgorithm;
import cz.sefira.obelisk.dss.token.DSSPrivateKeyEntry;
import cz.sefira.obelisk.token.keystore.KSPrivateKeyEntry;
import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.util.TextUtils;
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
    byte[] id = key.getCertificateToken().getDigest(DigestAlgorithm.SHA256);
    product.setCertificateId(TextUtils.encodeHexString(id));
    if (key instanceof KSPrivateKeyEntry) {
      product.setKeyAlias(((KSPrivateKeyEntry) key).getAlias());
    }
    if (key instanceof KeychainPrivateKey) {
      product.setKeyAlias(((KeychainPrivateKey) key).getAlias());
    }
    if (key instanceof IAIKPrivateKeyEntry) {
      product.setKeyAlias(((IAIKPrivateKeyEntry) key).getKeyLabel());
    }
    product.setCertificate(Base64.encodeBase64String(key.getCertificateToken().getEncoded()));
    // save fully-configured product
    productAdapter.saveProduct(product, map);
    return new OperationResult<>(true);
  }
}

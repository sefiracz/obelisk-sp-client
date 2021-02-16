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
import eu.europa.esig.dss.token.SignatureTokenConnection;
import lu.nowina.nexu.Utils;
import lu.nowina.nexu.api.*;
import lu.nowina.nexu.api.flow.OperationResult;
import lu.nowina.nexu.pkcs11.IAIKPrivateKeyEntry;
import org.apache.commons.codec.binary.Base64;

import java.util.Map;

/**
 * description
 */
public class SaveFullSelectionOperation extends AbstractCompositeOperation<Boolean> {

  private SignatureTokenConnection token;
  private NexuAPI api;
  private AbstractProduct product;
  private ProductAdapter productAdapter;
  private DSSPrivateKeyEntry key;
  private Map<TokenOperationResultKey, Object> map;

  @Override
  public void setParams(Object... params) {
    try {
      this.token = (SignatureTokenConnection) params[0];
      this.api = (NexuAPI) params[1];
      this.product = (AbstractProduct) params[2];
      this.productAdapter = (ProductAdapter) params[3];
      this.key = (DSSPrivateKeyEntry) params[4];
      this.map = (Map<TokenOperationResultKey, Object>) params[5];
    } catch(final ClassCastException | ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Expected parameters: NexuAPI, Product, ProductAdapter, Key, Map");
    }
  }

  @Override
  public OperationResult<Boolean> perform() {
    byte[] id = key.getCertificate().getDigest(DigestAlgorithm.SHA256);
    String certificateId = Utils.encodeHexString(id);
    product.setCertificateId(certificateId);
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

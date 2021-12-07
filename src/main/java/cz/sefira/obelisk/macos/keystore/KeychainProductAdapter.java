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
 * cz.sefira.obelisk.macos.keystore.KeychainProductAdapter
 *
 * Created: 06.12.2021
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.api.flow.FutureOperationInvocation;
import cz.sefira.obelisk.api.flow.NoOpFutureOperationInvocation;
import cz.sefira.obelisk.flow.operation.TokenOperationResultKey;
import eu.europa.esig.dss.token.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Product adapter for {@link MacOSKeychain}.
 */
public class KeychainProductAdapter implements ProductAdapter {

  private final NexuAPI api;

  public KeychainProductAdapter(final NexuAPI api) {
    this.api = api;
  }

  @Override
  public boolean accept(Product product) {
    return (product instanceof MacOSKeychain);
  }

  @Override
  public String getLabel(NexuAPI api, Product product, PasswordInputCallback callback) {
    return product.getLabel();
  }

  @Override
  public SignatureTokenConnection connect(NexuAPI api, Product product, PasswordInputCallback callback) {
    return new KeychainSignatureTokenAdapter();
  }

  @Override
  public List<DSSPrivateKeyEntry> getKeys(SignatureTokenConnection token, CertificateFilter certificateFilter) {
    return new CertificateFilterHelper().filterKeys(token, certificateFilter);
  }

  @Override
  public DSSPrivateKeyEntry getKey(SignatureTokenConnection token, String keyAlias) {
    List<DSSPrivateKeyEntry> keys = token.getKeys();
    for (DSSPrivateKeyEntry key : keys) {
      if (key instanceof KeychainPrivateKey && ((KeychainPrivateKey) key).getAlias().equalsIgnoreCase(keyAlias)) {
        return key;
      }
    }
    return null;
  }

  @Override
  public FutureOperationInvocation<Product> getConfigurationOperation(NexuAPI api, Product product) {
    return new NoOpFutureOperationInvocation<Product>(product);
  }

  @Override
  public SystrayMenuItem getExtensionSystrayMenuItem(NexuAPI api) {
    return null;
  }

  @Override
  public List<? extends Product> detectProducts() {
    final List<Product> products = new ArrayList<>();
    getProductDatabase().getProducts();
    products.add(new MacOSKeychain());
    return products;
  }


  @Override
  public MacOSKeychainDatabase getProductDatabase() {
    return api.loadDatabase(MacOSKeychainDatabase.class, "database-macos.xml");
  }

  private void saveKeystore(final MacOSKeychain keystore) {
    getProductDatabase().add(keystore);
  }

  @Override
  public void saveProduct(AbstractProduct product, Map<TokenOperationResultKey, Object> map) {
    saveKeystore((MacOSKeychain) product);
  }
}

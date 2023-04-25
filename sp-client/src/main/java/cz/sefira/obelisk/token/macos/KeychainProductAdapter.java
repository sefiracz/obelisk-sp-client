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
package cz.sefira.obelisk.token.macos;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.macos.keystore.KeychainProductAdapter
 *
 * Created: 06.12.2021
 * Author: hlavnicka
 */

import cz.sefira.obelisk.ProductStorage;
import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.api.flow.FutureOperationInvocation;
import cz.sefira.obelisk.api.flow.NoOpFutureOperationInvocation;
import cz.sefira.obelisk.api.ws.model.CertificateFilter;
import cz.sefira.obelisk.flow.operation.TokenOperationResultKey;
import cz.sefira.obelisk.generic.SessionManager;
import cz.sefira.obelisk.dss.token.DSSPrivateKeyEntry;
import cz.sefira.obelisk.dss.token.PasswordInputCallback;
import cz.sefira.obelisk.dss.token.SignatureTokenConnection;
import cz.sefira.obelisk.systray.SystrayMenuItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Product adapter for {@link MacOSKeychain}.
 */
public class KeychainProductAdapter implements ProductAdapter {

  private static final Logger logger = LoggerFactory.getLogger(KeychainProductAdapter.class.getName());

  private final PlatformAPI api;

  public KeychainProductAdapter(final PlatformAPI api) {
    this.api = api;
  }

  @Override
  public boolean accept(Product product) {
    return (product instanceof MacOSKeychain);
  }

  @Override
  public String getLabel(PlatformAPI api, Product product, PasswordInputCallback callback) {
    return product.getLabel();
  }

  @Override
  public SignatureTokenConnection connect(PlatformAPI api, Product product, PasswordInputCallback callback) {
    SignatureTokenConnection tokenConnection = SessionManager.getManager().getInitializedTokenForProduct((MacOSKeychain)product);
    if (tokenConnection == null) {
      tokenConnection = new KeychainSignatureTokenAdapter();
    }
    SessionManager.getManager().setToken((MacOSKeychain) product, tokenConnection);
    return tokenConnection;
  }

  @Override
  public List<DSSPrivateKeyEntry> getKeys(SignatureTokenConnection token, CertificateFilter certificateFilter) {
    return new CertificateFilterHelper().filterKeys(token, certificateFilter);
  }

  @Override
  public DSSPrivateKeyEntry getKey(SignatureTokenConnection token, String keyAlias, X509Certificate certificate) {
    List<DSSPrivateKeyEntry> keys = token.getKeys();
    for (DSSPrivateKeyEntry key : keys) {
      if(certificate.equals(key.getCertificateToken().getCertificate()) && key instanceof KeychainPrivateKey) {
        String alias = ((KeychainPrivateKey) key).getAlias();
        if (keyAlias != null && !alias.equalsIgnoreCase(keyAlias)) {
          logger.warn("Aliases do not equal: " + alias + " != " + keyAlias);
        }
        return key;
      }
    }
    return null;
  }

  @Override
  public FutureOperationInvocation<Product> getConfigurationOperation(PlatformAPI api, Product product) {
    return new NoOpFutureOperationInvocation<Product>(product);
  }

  @Override
  public SystrayMenuItem getExtensionSystrayMenuItem(PlatformAPI api) {
    return null;
  }

  @Override
  public List<? extends Product> detectProducts() {
    final List<Product> products = new ArrayList<>();
    products.add(new MacOSKeychain());
    return products;
  }

  public ProductStorage<MacOSKeychain> getProductStorage() {
    return api.getProductStorage(MacOSKeychain.class);
  }

  @Override
  public void saveProduct(AbstractProduct product, Map<TokenOperationResultKey, Object> map) {
    getProductStorage().add(product);
  }

  @Override
  public void removeProduct(AbstractProduct product) {
    getProductStorage().remove(product);
  }
}

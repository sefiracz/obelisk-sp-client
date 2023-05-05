/**
 * © Nowina Solutions, 2015-2016
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package cz.sefira.obelisk.token.windows;

import cz.sefira.obelisk.storage.ProductStorage;
import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.api.flow.FutureOperationInvocation;
import cz.sefira.obelisk.api.flow.NoOpFutureOperationInvocation;
import cz.sefira.obelisk.api.ws.model.CertificateFilter;
import cz.sefira.obelisk.token.keystore.KSPrivateKeyEntry;
import cz.sefira.obelisk.flow.exceptions.PKCS11TokenException;
import cz.sefira.obelisk.flow.operation.TokenOperationResultKey;
import cz.sefira.obelisk.token.keystore.EmptyKeyEntry;
import cz.sefira.obelisk.generic.SessionManager;
import cz.sefira.obelisk.dss.token.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * Product adapter for {@link WindowsKeystore}.
 *
 * @author simon.ghisalberti
 *
 */
public class WindowsKeystoreProductAdapter implements ProductAdapter {

	private static final Logger logger = LoggerFactory.getLogger(WindowsKeystoreProductAdapter.class.getName());

  private final PlatformAPI api;

	public WindowsKeystoreProductAdapter(final PlatformAPI api) {
		this.api = api;
	}

	@Override
	public boolean accept(Product product) {
		return (product instanceof WindowsKeystore);
	}

	@Override
	public String getLabel(PlatformAPI api, Product product, PasswordInputCallback callback) {
		return product.getLabel();
	}

	@Override
	public SignatureTokenConnection connect(PlatformAPI api, Product product, PasswordInputCallback callback) {
		SignatureTokenConnection tokenConnection = SessionManager.getManager().getInitializedTokenForProduct((WindowsKeystore)product);
		if (tokenConnection == null) {
			tokenConnection = new WindowsSignatureTokenAdapter();
		}
		SessionManager.getManager().setToken((WindowsKeystore) product, tokenConnection);
		return tokenConnection;
	}

	@Override
	public List<DSSPrivateKeyEntry> getKeys(SignatureTokenConnection token, CertificateFilter certificateFilter) {
		return new CertificateFilterHelper().filterKeys(token, certificateFilter);
	}

	@Override
	public DSSPrivateKeyEntry getKey(SignatureTokenConnection token, String keyAlias, X509Certificate certificate) {
		List<DSSPrivateKeyEntry> keys = token.getKeys();
		for(DSSPrivateKeyEntry key : keys) {
			if(certificate.equals(key.getCertificateToken().getCertificate())) {
				if (key instanceof KSPrivateKeyEntry) {
					String alias = ((KSPrivateKeyEntry) key).getAlias();
					if (keyAlias != null && !alias.equalsIgnoreCase(keyAlias)) {
						logger.warn("Aliases do not equal: " + alias + " != " + keyAlias);
					}
					return key;
				}
				if (key instanceof EmptyKeyEntry && ((EmptyKeyEntry) key).getAlias().equalsIgnoreCase(keyAlias)) {
					throw new PKCS11TokenException("No private key available"); // reusing exception -> minidriver use case
				}
			}
		}
		return null;
	}

	@Override
	public FutureOperationInvocation<Product> getConfigurationOperation(PlatformAPI api, Product product) {
		return new NoOpFutureOperationInvocation<Product>(product);
	}

  @Override
	public List<Product> detectProducts() {
		final List<Product> products = new ArrayList<>();
		products.add(new WindowsKeystore());
		return products;
	}

	public ProductStorage<WindowsKeystore> getProductStorage() {
		return api.getProductStorage(WindowsKeystore.class);
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

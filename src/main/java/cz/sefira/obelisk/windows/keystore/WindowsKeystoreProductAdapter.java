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
package cz.sefira.obelisk.windows.keystore;

import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.api.flow.FutureOperationInvocation;
import cz.sefira.obelisk.api.flow.NoOpFutureOperationInvocation;
import cz.sefira.obelisk.flow.exceptions.PKCS11TokenException;
import cz.sefira.obelisk.flow.operation.TokenOperationResultKey;
import cz.sefira.obelisk.generic.EmptyKeyEntry;
import cz.sefira.obelisk.generic.SessionManager;
import eu.europa.esig.dss.token.*;

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

  private final NexuAPI api;

	public WindowsKeystoreProductAdapter(final NexuAPI api) {
		this.api = api;
	}

	@Override
	public boolean accept(Product product) {
		return (product instanceof WindowsKeystore);
	}

	@Override
	public String getLabel(NexuAPI api, Product product, PasswordInputCallback callback) {
		return product.getLabel();
	}

	@Override
	public SignatureTokenConnection connect(NexuAPI api, Product product, PasswordInputCallback callback) {
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
			if(certificate.equals(key.getCertificate().getCertificate())) {
				if (key instanceof KSPrivateKeyEntry &&
						(keyAlias == null || ((KSPrivateKeyEntry) key).getAlias().equalsIgnoreCase(keyAlias))) {
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
	public FutureOperationInvocation<Product> getConfigurationOperation(NexuAPI api, Product product) {
		return new NoOpFutureOperationInvocation<Product>(product);
	}

  @Override
  public SystrayMenuItem getExtensionSystrayMenuItem(NexuAPI api) {
    return null;
  }

  @Override
	public List<Product> detectProducts() {
		final List<Product> products = new ArrayList<>();
		getProductDatabase().getProducts();
		products.add(new WindowsKeystore());
		return products;
	}

	public WindowsKeystoreDatabase getProductDatabase() {
		return api.loadDatabase(WindowsKeystoreDatabase.class, "database-windows.xml");
	}

	private void saveKeystore(final WindowsKeystore keystore) {
		getProductDatabase().add(keystore);
	}

	@Override
	public void saveProduct(AbstractProduct product, Map<TokenOperationResultKey, Object> map) {
		saveKeystore((WindowsKeystore) product);
	}

	@Override
	public void removeProduct(AbstractProduct product) {
		getProductDatabase().remove(api, product);
	}

}

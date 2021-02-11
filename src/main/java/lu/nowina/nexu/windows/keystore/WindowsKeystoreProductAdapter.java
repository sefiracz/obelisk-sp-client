/**
 * © Nowina Solutions, 2015-2016
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
package lu.nowina.nexu.windows.keystore;

import eu.europa.esig.dss.DigestAlgorithm;
import eu.europa.esig.dss.token.*;
import lu.nowina.nexu.EntityDatabaseLoader;
import lu.nowina.nexu.api.*;
import lu.nowina.nexu.api.flow.FutureOperationInvocation;
import lu.nowina.nexu.api.flow.NoOpFutureOperationInvocation;
import lu.nowina.nexu.flow.operation.TokenOperationResultKey;

import java.io.File;
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
public class WindowsKeystoreProductAdapter extends AbstractProductAdapter {

	public WindowsKeystoreProductAdapter(File nexuHome) {
		super(nexuHome);
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
	public String getLabel(NexuAPI api, Product product, PasswordInputCallback callback, MessageDisplayCallback messageCallback) {
		throw new IllegalStateException("This product adapter does not support message display callback.");
	}

	@Override
	public boolean supportMessageDisplayCallback(Product product) {
		return false;
	}

	@Override
	public SignatureTokenConnection connect(NexuAPI api, Product product, PasswordInputCallback callback) {
		return new MSCAPISignatureToken();
	}

	@Override
	public SignatureTokenConnection connect(NexuAPI api, Product product, PasswordInputCallback callback, MessageDisplayCallback messageCallback) {
		throw new IllegalStateException("This product adapter does not support message display callback.");
	}

	@Override
	public boolean canReturnIdentityInfo(Product product) {
		return false;
	}

	@Override
	public GetIdentityInfoResponse getIdentityInfo(SignatureTokenConnection token) {
		throw new IllegalStateException("This product adapter cannot return identity information.");
	}

	@Override
	public boolean supportCertificateFilter(Product product) {
		return true;
	}

	@Override
	public List<DSSPrivateKeyEntry> getKeys(SignatureTokenConnection token, CertificateFilter certificateFilter) {
		return new CertificateFilterHelper().filterKeys(token, certificateFilter);
	}

	@Override
	public DSSPrivateKeyEntry getKey(SignatureTokenConnection token, String keyAlias) {
		List<DSSPrivateKeyEntry> keys = token.getKeys();
		for(DSSPrivateKeyEntry key : keys) {
			if(key instanceof KSPrivateKeyEntry && ((KSPrivateKeyEntry) key).getAlias().equalsIgnoreCase(keyAlias)) {
				return key;
			}
		}
		return null;
	}

	@Override
	public boolean canReturnSuportedDigestAlgorithms(Product product) {
		return false;
	}

	@Override
	public List<DigestAlgorithm> getSupportedDigestAlgorithms(Product product) {
		throw new IllegalStateException("This product adapter cannot return list of supported digest algorithms.");
	}

	@Override
	public DigestAlgorithm getPreferredDigestAlgorithm(Product product) {
		throw new IllegalStateException("This product adapter cannot return list of supported digest algorithms.");
	}

	@Override
	public FutureOperationInvocation<Product> getConfigurationOperation(NexuAPI api, Product product) {
		return new NoOpFutureOperationInvocation<Product>(product);
	}

	@Override
	public FutureOperationInvocation<Boolean> getSaveOperation(NexuAPI api, Product product) {
		return new NoOpFutureOperationInvocation<Boolean>(true);
	}

	@Override
	public List<Product> detectProducts() {
		final List<Product> products = new ArrayList<>();
		getProductDatabase().getKeystores();
		products.add(new WindowsKeystore());
		return products;
	}

	public WindowsKeystoreDatabase getProductDatabase() {
		return EntityDatabaseLoader.load(WindowsKeystoreDatabase.class, new File(nexuHome, "database-windows.xml"));
	}

	public void saveKeystore(final WindowsKeystore keystore) {
		getProductDatabase().add(keystore);
	}

	@Override
	public void saveKeystore(AbstractProduct keystore, Map<TokenOperationResultKey, Object> map) {
		saveKeystore((WindowsKeystore) keystore);
	}

}

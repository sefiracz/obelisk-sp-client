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
package lu.nowina.nexu.keystore;

import eu.europa.esig.dss.*;
import eu.europa.esig.dss.token.*;
import lu.nowina.nexu.NexuException;
import lu.nowina.nexu.ProductDatabaseLoader;
import lu.nowina.nexu.api.*;
import lu.nowina.nexu.api.flow.FutureOperationInvocation;
import lu.nowina.nexu.api.flow.NoOpFutureOperationInvocation;
import lu.nowina.nexu.flow.operation.TokenOperationResultKey;
import lu.nowina.nexu.view.core.NonBlockingUIOperation;
import lu.nowina.nexu.view.core.UIOperation;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore.PasswordProtection;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Product adapter for {@link ConfiguredKeystore}.
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public class KeystoreProductAdapter extends AbstractProductAdapter {

	public KeystoreProductAdapter(final File nexuHome) {
		super(nexuHome);
	}

	@Override
	public boolean accept(Product product) {
		return (product instanceof ConfiguredKeystore) || (product instanceof NewKeystore);
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
		if (product instanceof NewKeystore) {
			throw new IllegalArgumentException("Given product was not configured!");
		}
		final ConfiguredKeystore configuredKeystore = (ConfiguredKeystore) product;
		if(callback instanceof NexuPasswordInputCallback) {
			((NexuPasswordInputCallback) callback).setProduct((AbstractProduct) product);
		}
		return new KeystoreTokenProxy(configuredKeystore, callback);
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
	@SuppressWarnings("unchecked")
	public FutureOperationInvocation<Product> getConfigurationOperation(NexuAPI api, Product product) {
		if (product instanceof NewKeystore) {
			return UIOperation.getFutureOperationInvocation(UIOperation.class, "/fxml/configure-keystore.fxml", api.getAppConfig().getApplicationName());
		} else {
			return new NoOpFutureOperationInvocation<Product>(product);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public FutureOperationInvocation<Boolean> getSaveOperation(NexuAPI api, Product product) {
		if (product instanceof NewKeystore) {
			throw new IllegalArgumentException("Given product was not configured!");
		} else {
			final ConfiguredKeystore keystore = (ConfiguredKeystore) product;
			if(keystore.isToBeSaved()) {
				return UIOperation.getFutureOperationInvocation(UIOperation.class, "/fxml/save-keystore.fxml",
					api.getAppConfig().getApplicationName(), this, keystore);
			} else {
				return new NoOpFutureOperationInvocation<Boolean>(true);
			}
		}
	}

	@Override
	public List<Product> detectProducts() {
		final List<Product> products = new ArrayList<>();
//		products.addAll(getDatabase().getKeystores()); // TODO - asi nechceme zobrazovat zapamatovane klicenky
		getDatabase().getKeystores(); // reloads database
		products.add(new NewKeystore());
		return products;
	}

	public KeystoreDatabase getDatabase() {
		return ProductDatabaseLoader.load(KeystoreDatabase.class, new File(nexuHome, "database-keystore.xml"));
	}

	public void saveKeystore(final ConfiguredKeystore keystore) {
		getDatabase().add(keystore);
	}

	@Override
	public void saveKeystore(AbstractProduct keystore, Map<TokenOperationResultKey, Object> map) {
		saveKeystore((ConfiguredKeystore) keystore);
	}

	private static class KeystoreTokenProxy implements SignatureTokenConnection {

		private SignatureTokenConnection proxied;
		private final ConfiguredKeystore configuredKeystore;
		private final PasswordInputCallback callback;

		public KeystoreTokenProxy(ConfiguredKeystore configuredKeystore, PasswordInputCallback callback) {
			super();
			this.configuredKeystore = configuredKeystore;
			this.callback = callback;
		}

		private void initSignatureTokenConnection() {
			if(proxied != null) {
				return;
			}
			try {
				switch(configuredKeystore.getType()) {
				case PKCS12:
					proxied = new Pkcs12SignatureToken(new URL(configuredKeystore.getUrl()).openStream(),
							new PasswordProtection(callback.getPassword()));
					break;
				case JKS:
					proxied = new JKSSignatureToken(new URL(configuredKeystore.getUrl()).openStream(),
							new PasswordProtection(callback.getPassword()));
					break;
				case JCEKS:
					proxied = new KeyStoreSignatureTokenConnection(new URL(configuredKeystore.getUrl()).openStream(),
							"JCEKS", new PasswordProtection(callback.getPassword()));
				  break;
				default:
					throw new IllegalStateException("Unhandled keystore type: " + configuredKeystore.getType());
				}
			} catch (MalformedURLException e) {
				throw new NexuException(e);
			} catch (IOException e) {
				throw new NexuException(e);
			}
		}

		@Override
		public void close() {
			final SignatureTokenConnection stc = proxied;
			// Always nullify proxied even in case of exception when calling close()
			proxied = null;
			if(stc != null) {
				stc.close();
			}
		}

		@Override
		public List<DSSPrivateKeyEntry> getKeys() throws DSSException {
			initSignatureTokenConnection();
			return proxied.getKeys();
		}

		@Override
		public SignatureValue sign(ToBeSigned toBeSigned, DigestAlgorithm digestAlgorithm, DSSPrivateKeyEntry keyEntry)
				throws DSSException {
			initSignatureTokenConnection();
			return proxied.sign(toBeSigned, digestAlgorithm, keyEntry);
		}

		@Override
		public SignatureValue sign(ToBeSigned toBeSigned, DigestAlgorithm digestAlgorithm, MaskGenerationFunction mgf, DSSPrivateKeyEntry keyEntry) throws DSSException {
			initSignatureTokenConnection();
			return proxied.sign(toBeSigned, digestAlgorithm, mgf, keyEntry);
		}
	}
}

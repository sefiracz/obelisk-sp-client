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
package cz.sefira.obelisk.keystore;

import cz.sefira.obelisk.NexuException;
import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.api.flow.FutureOperationInvocation;
import cz.sefira.obelisk.api.flow.NoOpFutureOperationInvocation;
import cz.sefira.obelisk.flow.operation.TokenOperationResultKey;
import cz.sefira.obelisk.generic.SessionManager;
import cz.sefira.obelisk.view.core.UIOperation;
import eu.europa.esig.dss.*;
import eu.europa.esig.dss.token.*;
import cz.sefira.obelisk.flow.exceptions.KeystoreNotFoundException;
import cz.sefira.obelisk.flow.exceptions.UnsupportedKeystoreTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.KeyStore.PasswordProtection;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Product adapter for {@link ConfiguredKeystore}.
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public class KeystoreProductAdapter implements ProductAdapter {

	private static final Logger logger = LoggerFactory.getLogger(KeystoreProductAdapter.class.getName());

	private final NexuAPI api;

	public KeystoreProductAdapter(final NexuAPI api) {
		this.api = api;
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
	public SignatureTokenConnection connect(NexuAPI api, Product product, PasswordInputCallback callback) {
		if (product instanceof NewKeystore) {
			throw new IllegalArgumentException("Given product was not configured!");
		}
		return new KeystoreTokenProxy((ConfiguredKeystore) product, callback);
	}

	@Override
	public List<DSSPrivateKeyEntry> getKeys(SignatureTokenConnection token, CertificateFilter certificateFilter) {
		return new CertificateFilterHelper().filterKeys(token, certificateFilter);
	}

	@Override
	public DSSPrivateKeyEntry getKey(SignatureTokenConnection token, String keyAlias, X509Certificate certificate) {
		List<DSSPrivateKeyEntry> keys = token.getKeys();
		for(DSSPrivateKeyEntry key : keys) {
			if(certificate.equals(key.getCertificate().getCertificate()) &&
					key instanceof KSPrivateKeyEntry && ((KSPrivateKeyEntry) key).getAlias().equalsIgnoreCase(keyAlias)) {
				return key;
			}
		}
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public FutureOperationInvocation<Product> getConfigurationOperation(NexuAPI api, Product product) {
		if (product instanceof NewKeystore) {
			return UIOperation
          .getFutureOperationInvocation(UIOperation.class, "/fxml/configure-keystore.fxml", api.getAppConfig().getApplicationName());
		} else {
			return new NoOpFutureOperationInvocation<Product>(product);
		}
	}

  @Override
  public SystrayMenuItem getExtensionSystrayMenuItem(NexuAPI api) {
    return null;
  }

  @Override
	public List<Product> detectProducts() {
		final List<Product> products = new ArrayList<>();
		getProductDatabase().getProducts(); // reloads database
		products.add(new NewKeystore());
		return products;
	}

  @Override
  public void saveProduct(AbstractProduct product, Map<TokenOperationResultKey, Object> map) {
    saveKeystore((ConfiguredKeystore) product);
  }

	@Override
	public void removeProduct(AbstractProduct product) {
		getProductDatabase().remove(api, product);
	}

	public KeystoreDatabase getProductDatabase() {
		return api.loadDatabase(KeystoreDatabase.class, "database-keystore.xml");
	}

	public void saveKeystore(final ConfiguredKeystore keystore) {
		getProductDatabase().add(keystore);
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
      proxied = SessionManager.getManager().getInitializedTokenForProduct(configuredKeystore);
			String path = configuredKeystore.getUrl();
			try {
				path = Paths.get(new URI(path)).toFile().getAbsolutePath();
			}
			catch (URISyntaxException ex) {
				logger.error(ex.getMessage(), ex);
			}
      if(proxied == null) {
        try {
          switch (configuredKeystore.getType()) {
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
              throw new UnsupportedKeystoreTypeException("Unsupported keystore type", path);
          }
        } catch (FileNotFoundException e) {
          throw new KeystoreNotFoundException("Keystore file not found", path);
        } catch (IOException e) {
          throw new NexuException(e);
        }
      }
      SessionManager.getManager().setToken(configuredKeystore, proxied);
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

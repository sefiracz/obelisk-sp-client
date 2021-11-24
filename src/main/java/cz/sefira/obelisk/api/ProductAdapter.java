/**
 * © Nowina Solutions, 2015-2015
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
package cz.sefira.obelisk.api;

import cz.sefira.obelisk.flow.operation.TokenOperationResultKey;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.PasswordInputCallback;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import cz.sefira.obelisk.ProductDatabase;
import cz.sefira.obelisk.api.flow.FutureOperationInvocation;
import cz.sefira.obelisk.api.flow.Operation;

import java.util.List;
import java.util.Map;

/**
 * A <code>ProductAdapter</code> can manage some specific {@link Product}s.
 *
 * @author David Naramski
 */
public interface ProductAdapter {

	/**
	 * Queries the <code>ProductAdapter</code> to know if he is able to manage the given {@link Product}.
	 * @param product The target product.
	 * @return <code>true</code> if the <code>ProductAdapter</code> can manage the given {@link Product}.
	 */
	boolean accept(Product product);

	/**
	 * Returns a label for the given <code>product</code>.
	 * @param api The unique instance of {@link NexuAPI}.
	 * @param product The target product.
	 * @param callback Password input callback.
	 * @return A label for the given <code>product</code>.
	 */
	String getLabel(NexuAPI api, Product product, PasswordInputCallback callback);

	/**
	 * Creates a {@link SignatureTokenConnection} for the given product.
	 * @param api The unique instance of {@link NexuAPI}.
	 * @param product The target product.
	 * @param callback Password input callback.
	 * @return A {@link SignatureTokenConnection} for the given product.
	 */
	SignatureTokenConnection connect(NexuAPI api, Product product, PasswordInputCallback callback);

	/**
	 * Returns the keys of <code>token</code> matching the <code>certificateFilter</code>.
	 * @param token The token to use to retrieve the keys.
	 * @param certificateFilter Filter that must be matched by returned keys.
	 * @return The keys of <code>token</code> matching the <code>certificateFilter</code>.
	 */
	List<DSSPrivateKeyEntry> getKeys(SignatureTokenConnection token, CertificateFilter certificateFilter);

	/**
	 * Returns the key from <code>token</code> matching the <code>keyAlias</code>.
	 * @param token The token to use to retrieve the key.
	 * @param keyAlias Key alias that we are looking for.
	 * @return The key from <code>token</code> matching the <code>keyAlias</code>.
	 */
	DSSPrivateKeyEntry getKey(SignatureTokenConnection token, String keyAlias);

	/**
	 * Returns the specification of the operation to call to configure <code>product</code>.
	 * <p>Returned operation must return a configured product.
	 * @param api The unique instance of {@link NexuAPI}.
	 * @param product The product for which one would like to retrieve the configuration {@link Operation}.
	 * @return The specification of the operation to call to configure <code>product</code>.
	 */
	FutureOperationInvocation<Product> getConfigurationOperation(NexuAPI api, Product product);

	/**
	 * Each <code>ProductAdapter</code> is given the capability to enrich the systray menu with an item
	 * specific to it. This method is used to retrieve this item.
	 * @return The menu item specific to this <code>ProductAdapter</code> or <code>null</code> if none.
	 */
	SystrayMenuItem getExtensionSystrayMenuItem(final NexuAPI api);

	/**
	 * Detects products that will <strong>maybe</strong> be accepted by this <code>ProductAdapter</code>.
	 * @return Products that will <strong>maybe</strong> be accepted by this <code>ProductAdapter</code>.
	 */
	List<? extends Product> detectProducts();

  /**
   * Saves the product to its appropriate database
   * @param product Product to be saved
   * @param map Token information
   */
	void saveProduct(AbstractProduct product, Map<TokenOperationResultKey, Object> map);

	ProductDatabase getProductDatabase();
}

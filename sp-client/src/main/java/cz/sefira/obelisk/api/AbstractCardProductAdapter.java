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
package cz.sefira.obelisk.api;

import cz.sefira.obelisk.api.flow.FutureOperationInvocation;
import cz.sefira.obelisk.api.flow.NoOpFutureOperationInvocation;
import cz.sefira.obelisk.dss.token.PasswordInputCallback;
import cz.sefira.obelisk.dss.token.SignatureTokenConnection;
import cz.sefira.obelisk.token.pkcs11.DetectedCard;

import java.util.Collections;
import java.util.List;

/**
 * Convenient base class for {@link ProductAdapter}s supporting {@link DetectedCard}s.
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public abstract class AbstractCardProductAdapter implements ProductAdapter {

	public AbstractCardProductAdapter() {
	}

	@Override
	public final boolean accept(Product product) {
		return (product instanceof DetectedCard) && accept((DetectedCard) product);
	}

	protected abstract boolean accept(DetectedCard card);

	@Override
	public String getLabel(PlatformAPI api, Product product, PasswordInputCallback callback) {
		return getLabel(api, (DetectedCard) product, callback);
	}

	protected abstract String getLabel(PlatformAPI api, DetectedCard card, PasswordInputCallback callback);

	@Override
	public final SignatureTokenConnection connect(PlatformAPI api, Product product, PasswordInputCallback callback) {
		return connect(api, (DetectedCard) product, callback);
	}

	protected abstract SignatureTokenConnection connect(PlatformAPI api, DetectedCard card, PasswordInputCallback callback);

	@Override
	public final FutureOperationInvocation<Product> getConfigurationOperation(PlatformAPI api, Product product) {
		return getConfigurationOperation(api, (DetectedCard) product);
	}

	protected FutureOperationInvocation<Product> getConfigurationOperation(PlatformAPI api, DetectedCard card) {
		return new NoOpFutureOperationInvocation<Product>(card);
	}

	/**
	 * This implementation returns an empty list.
	 */
	@Override
	public List<Product> detectProducts() {
		return Collections.emptyList();
	}
}

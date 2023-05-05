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

import cz.sefira.obelisk.storage.EventsStorage;
import cz.sefira.obelisk.storage.ProductStorage;
import cz.sefira.obelisk.api.flow.OperationFactory;
import cz.sefira.obelisk.api.model.EnvironmentInfo;
import cz.sefira.obelisk.api.plugin.VersionPlugin;
import cz.sefira.obelisk.api.ws.model.*;
import cz.sefira.obelisk.api.ws.ssl.SSLCertificateProvider;
import cz.sefira.obelisk.token.pkcs11.DetectedCard;
import cz.sefira.obelisk.token.pkcs11.PKCS11Manager;
import cz.sefira.obelisk.view.core.UIDisplay;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import java.util.List;

/**
 * The API exposes the functionalities to the developer of Plugin.
 *
 * @author David Naramski
 */
@SuppressWarnings("restriction")
public interface PlatformAPI {

  // PCSC Terminal API
	CardTerminal getCardTerminal(DetectedCard card);

	List<DetectedCard> detectCards(boolean showBusy);

	DetectedCard getPresentCard(DetectedCard selector) throws CardException;

	void detectCardTerminal(DetectedCard card);

  // Product API

	List<Product> detectProducts();

	void detectAll();

	List<Match> matchingProductAdapters(Product p);

	EnvironmentInfo getEnvironmentInfo();

	void registerProductAdapter(ProductAdapter adapter);

	// Flow executions

	Execution<GetCertificateResponse> getCertificate(GetCertificateRequest request);

	Execution<SignatureResponse> sign(SignatureRequest request);

	Execution<Boolean> checkSession(SessionValue sessionValue);

	// Utils API

	VersionPlugin getVersionPlugin();

	String getLabel(Product p);

	UIDisplay getDisplay();

	void supportedSmartcardInfos(List<SmartcardInfo> infos, byte[] digest);

	PKCS11Manager getPKCS11Manager();

  OperationFactory getOperationFactory();

	<T extends AbstractProduct> ProductStorage<T> getProductStorage(Class<T> c);

	EventsStorage getEventsStorage();

	void setSslCertificateProvider(SSLCertificateProvider sslCertificateProvider);

	SSLCertificateProvider getSslCertificateProvider();

	void pushNotification(Notification notification);

	void closeNotification();
}

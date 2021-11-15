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
package lu.nowina.nexu.api;

import eu.europa.esig.dss.token.SignatureTokenConnection;
import lu.nowina.nexu.EntityDatabase;
import lu.nowina.nexu.api.flow.OperationFactory;
import lu.nowina.nexu.api.plugin.HttpPlugin;
import lu.nowina.nexu.pkcs11.PKCS11Manager;
import lu.nowina.nexu.view.core.UIDisplay;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import java.util.List;

/**
 * The API exposes the functionalities to the developer of Plugin.
 *
 * @author David Naramski
 */
@SuppressWarnings("restriction")
public interface NexuAPI {

  // PCSC Terminal API
	CardTerminal getCardTerminal(DetectedCard card);

	List<DetectedCard> detectCards(boolean showBusy);

	DetectedCard getPresentCard(DetectedCard selector) throws CardException;

	void detectCardTerminal(DetectedCard card);

  // Product API

	List<Product> detectProducts();

	void detectAll();

	List<Match> matchingProductAdapters(Product p);

	List<SystrayMenuItem> getExtensionSystrayMenuItem();

	EnvironmentInfo getEnvironmentInfo();

	void registerProductAdapter(ProductAdapter adapter);

	TokenId registerTokenConnection(SignatureTokenConnection connection);

	SignatureTokenConnection getTokenConnection(TokenId tokenId);

	// Flow executions

	Execution<GetCertificateResponse> getCertificate(GetCertificateRequest request);

	Execution<GetTokenResponse> getToken(GetTokenRequest request);

	Execution<SignatureResponse> sign(SignatureRequest request);

  Execution<SmartcardListResponse> smartcardList(SmartcardListRequest request);

	// Utils API

	AppConfig getAppConfig();

	HttpPlugin getHttpPlugin(String pluginId);

	String getLabel(Product p);

	UIDisplay getDisplay();

	void supportedSmartcardInfos(List<SmartcardInfo> infos, byte[] digest);

	PKCS11Manager getPKCS11Manager();

  OperationFactory getOperationFactory();

  <T extends EntityDatabase> T loadDatabase(Class<T> c, String filename);
}

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
package cz.sefira.obelisk.object.model;

import org.mapstruct.Mapper;

/**
 * This test <code>MapStruct</code> mapper is used only to ease the maintenance
 * of the public object model of NexU.
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
@Mapper(uses={UtilMappers.class})
public interface FromAPIToPublicObjectModelTestMapper {

	// Get certificate
	GetCertificateRequest mapGetCertificateRequest(cz.sefira.obelisk.api.GetCertificateRequest req);
	GetCertificateResponse mapGetCertificateResponse(cz.sefira.obelisk.api.GetCertificateResponse resp);
	Execution<GetCertificateResponse> mapGetCertificateResponse(
      cz.sefira.obelisk.api.Execution<cz.sefira.obelisk.api.GetCertificateResponse> resp);

	// Sign
	SignatureRequest mapSignatureRequest(cz.sefira.obelisk.api.SignatureRequest req);
	SignatureResponse mapSignatureResponse(cz.sefira.obelisk.api.SignatureResponse resp);
	Execution<SignatureResponse> mapSignatureResponse(
      cz.sefira.obelisk.api.Execution<cz.sefira.obelisk.api.SignatureResponse> resp);

	// Util
	CertificateFilter mapCertificateFilter(cz.sefira.obelisk.api.CertificateFilter certificateFilter);
	Feedback mapFeedback(cz.sefira.obelisk.api.Feedback feedback);
	TokenId mapTokenId(cz.sefira.obelisk.api.TokenId tokenId);
	ToBeSigned mapToBeSigned(eu.europa.esig.dss.ToBeSigned toBeSigned);
	FeedbackStatus mapFeedbackStatus(cz.sefira.obelisk.api.FeedbackStatus feedbackStatus);
	EnvironmentInfo mapEnvironmentInfo(cz.sefira.obelisk.api.EnvironmentInfo environmentInfo);
	JREVendor mapJREVendor(cz.sefira.obelisk.api.JREVendor jreVendor);
	Arch mapArch(cz.sefira.obelisk.api.Arch arch);
	OS mapOS(cz.sefira.obelisk.api.OS os);
	Purpose mapPurpose(cz.sefira.obelisk.api.Purpose purpose);
	SignatureValue mapSignatureValue(eu.europa.esig.dss.SignatureValue signatureValue);
}

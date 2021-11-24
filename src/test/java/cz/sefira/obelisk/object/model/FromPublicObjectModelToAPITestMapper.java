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
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * This test <code>MapStruct</code> mapper is used only to ease the maintenance
 * of the public object model of NexU.
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
@Mapper(uses={UtilMappers.class})
public interface FromPublicObjectModelToAPITestMapper {

	// Get certificate
	cz.sefira.obelisk.api.GetCertificateRequest mapGetCertificateRequest(GetCertificateRequest req);
	cz.sefira.obelisk.api.GetCertificateResponse mapGetCertificateResponse(GetCertificateResponse resp);
	cz.sefira.obelisk.api.Execution<cz.sefira.obelisk.api.GetCertificateResponse> mapGetCertificateResponse(Execution<GetCertificateResponse> resp, @MappingTarget cz.sefira.obelisk.api.Execution<cz.sefira.obelisk.api.GetCertificateResponse> to);

	// Sign
	cz.sefira.obelisk.api.SignatureRequest mapSignatureRequest(SignatureRequest req);
	cz.sefira.obelisk.api.SignatureResponse mapSignatureResponse(SignatureResponse resp, @MappingTarget cz.sefira.obelisk.api.SignatureResponse to);
	cz.sefira.obelisk.api.Execution<cz.sefira.obelisk.api.SignatureResponse> mapSignatureResponse(Execution<SignatureResponse> resp, @MappingTarget cz.sefira.obelisk.api.Execution<cz.sefira.obelisk.api.SignatureResponse> to);

	// Util
	cz.sefira.obelisk.api.CertificateFilter mapCertificateFilter(CertificateFilter certificateFilter);
	@Mapping(target="apiParameter", ignore=true)
	@Mapping(target="detected", ignore=true)
	@Mapping(target="selectedAPI", ignore=true)
	@Mapping(target="selectedCard", ignore=true)
	@Mapping(target="exception", ignore=true)
  cz.sefira.obelisk.api.Feedback mapFeedback(Feedback feedback);
	cz.sefira.obelisk.api.TokenId mapTokenId(TokenId tokenId);
	eu.europa.esig.dss.ToBeSigned mapToBeSigned(ToBeSigned toBeSigned);
	cz.sefira.obelisk.api.FeedbackStatus mapFeedbackStatus(FeedbackStatus feedbackStatus);
	cz.sefira.obelisk.api.EnvironmentInfo mapEnvironmentInfo(EnvironmentInfo environmentInfo);
	cz.sefira.obelisk.api.JREVendor mapJREVendor(JREVendor jreVendor);
	cz.sefira.obelisk.api.Arch mapArch(Arch arch);
	cz.sefira.obelisk.api.OS mapOS(OS os);
	cz.sefira.obelisk.api.Purpose mapPurpose(Purpose purpose);
	eu.europa.esig.dss.SignatureValue mapSignatureValue(SignatureValue signatureValue);
}

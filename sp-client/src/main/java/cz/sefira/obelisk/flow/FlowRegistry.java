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
package cz.sefira.obelisk.flow;

import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.view.core.UIDisplay;

public interface FlowRegistry {

	static final String SIGNATURE_FLOW = "signature";

	static final String CERTIFICATE_FLOW = "certificate";

	static final String CHECK_SESSION_FLOW = "checkSession";
	
	<I, O> Flow<I, O> getFlow(String code, UIDisplay display, PlatformAPI api);

}
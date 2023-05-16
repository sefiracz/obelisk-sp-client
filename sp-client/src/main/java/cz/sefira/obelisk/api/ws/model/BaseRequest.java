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
package cz.sefira.obelisk.api.ws.model;

import java.util.List;

/**
 * 
 * @author david.naramski
 *
 */
public class BaseRequest {

	private String operation;
	private String description;
	private boolean userInteraction = true;
	private SessionValue session;
	private List<SmartcardInfo> smartcards;

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isUserInteraction() {
		return userInteraction;
	}

	public void setUserInteraction(boolean userInteraction) {
		this.userInteraction = userInteraction;
	}

	public SessionValue getSession() {
		return session;
	}

	public void setSession(SessionValue session) {
		this.session = session;
	}

	public List<SmartcardInfo> getSmartcards() {
		return smartcards;
	}

	public void setSmartcards(List<SmartcardInfo> smartcards) {
		this.smartcards = smartcards;
	}

}

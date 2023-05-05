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
package cz.sefira.obelisk.generic;

import cz.sefira.obelisk.api.model.OS;
import cz.sefira.obelisk.api.model.ScAPI;

/**
 * The ConnectionInfo contains the information needed to configure the connection to a SmartCard with the generic API.
 *
 * @author david.naramski
 *
 */
public class ConnectionInfo {

	private OS os;
	private ScAPI selectedApi;
	private String apiParam;

	public ConnectionInfo() {
	}

	public ConnectionInfo(OS os, ScAPI selectedApi, String apiParam) {
		this.os = os;
		this.selectedApi = selectedApi;
		this.apiParam = apiParam;
	}

	public OS getOs() {
		return os;
	}

	public void setOs(OS os) {
		this.os = os;
	}

	public ScAPI getSelectedApi() {
		return selectedApi;
	}

	public void setSelectedApi(ScAPI selectedApi) {
		this.selectedApi = selectedApi;
	}

	public String getApiParam() {
		return apiParam;
	}

	public void setApiParam(String apiParam) {
		this.apiParam = apiParam;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ConnectionInfo that = (ConnectionInfo) o;

		if (getOs() != that.getOs()) return false;
		if (getSelectedApi() != that.getSelectedApi()) return false;
		return getApiParam().equals(that.getApiParam());
	}

	@Override
	public int hashCode() {
		int result = getOs().hashCode();
		result = 31 * result + getSelectedApi().hashCode();
		result = 31 * result + getApiParam().hashCode();
		return result;
	}
}

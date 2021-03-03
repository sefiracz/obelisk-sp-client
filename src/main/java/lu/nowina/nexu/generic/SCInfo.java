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
package lu.nowina.nexu.generic;

import lu.nowina.nexu.api.DetectedCard;
import lu.nowina.nexu.api.EnvironmentInfo;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SCInfo contains information about a detected SmartCard, such as known ATR, label and connection information
 *
 * @author david.naramski
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SCInfo extends DetectedCard {

	@XmlElementWrapper(name = "connectionInfos")
	@XmlElement(name = "connectionInfo")
	private List<ConnectionInfo> infos;

	public SCInfo() {}

	public SCInfo(DetectedCard c) {
		this.setAtr(c.getAtr());
		this.setCertificateId(c.getCertificateId());
		this.setCertificate(c.getCertificate());
		this.setType(c.getType());
		this.setKeyAlias(c.getKeyAlias());
		this.setTerminal(c.getTerminal());
		this.setTerminalIndex(c.getTerminalIndex());
		this.setTerminalLabel(c.getTerminalLabel());
		this.setTokenLabel(c.getTokenLabel());
		this.setTokenSerial(c.getTokenSerial());
		this.setTokenManufacturer(c.getTokenManufacturer());
	}

	public ConnectionInfo getConnectionInfo(EnvironmentInfo env) {
		for (ConnectionInfo info : getInfos()) {
			if (info.getEnv().matches(env)) {
				return info;
			}
		}
		return null;
	}

	public List<ConnectionInfo> getInfos() {
		if (infos == null) {
			infos = new ArrayList<>();
		}
		return infos;
	}

}

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

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;
import java.util.ResourceBundle;

/**
 * This enum gathers various keystore types supported by NexU.
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
@XmlType(name = "keystoreType")
@XmlEnum
public enum KeystoreType {

	@XmlEnumValue("JKS") JKS("JKS", "keystore.type.simple.keystore.jks"),
	@XmlEnumValue("PKCS12") PKCS12("PKCS#12", "keystore.type.simple.keystore.pfx"),
	@XmlEnumValue("JCEKS") JCEKS("JCEKS","keystore.type.simple.keystore.jceks"),
	@XmlEnumValue("PKCS11") PKCS11("PKCS#11","keystore.type.simple.smartcard.pkcs11"),
	@XmlEnumValue("WINDOWS-MY") WINDOWS("WINDOWS-MY","keystore.type.simple.keystore.windows"),
	@XmlEnumValue("UNKNOWN") UNKNOWN("UNKNOWN", "");

	private final String label;
	private final String simpleLabelCode;
	
	KeystoreType(final String label, final String simpleLabelCode) {
		this.label = label;
		this.simpleLabelCode = simpleLabelCode;
	}

	public String getLabel() {
		return label;
	}

	public String getSimpleLabel() {
		return ResourceBundle.getBundle("bundles/nexu").getString(simpleLabelCode);
	}
}

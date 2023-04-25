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
package cz.sefira.obelisk.api.model;

import java.util.ResourceBundle;

public enum KeystoreType {

	JKS("JKS", "keystore.type.simple.keystore.jks"),
	PKCS12("PKCS#12", "keystore.type.simple.keystore.pfx"),
	JCEKS("JCEKS","keystore.type.simple.keystore.jceks"),
	PKCS11("PKCS#11","keystore.type.simple.smartcard.pkcs11"),
	WINDOWS("WINDOWS-MY","keystore.type.simple.keystore.windows"),
	MACOSX("KeychainStore", "keystore.type.simple.keystore.macos"),
	UNKNOWN("UNKNOWN", "");

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

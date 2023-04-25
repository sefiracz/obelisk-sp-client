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
package cz.sefira.obelisk.token.macos;

import cz.sefira.obelisk.api.AbstractProduct;
import cz.sefira.obelisk.api.model.KeystoreType;
import org.apache.commons.lang.StringEscapeUtils;

import java.util.ResourceBundle;

/**
 * Represents a MacOS keychain instance.
 *
 * @author simon.ghisalberti
 *
 */
public class MacOSKeychain extends AbstractProduct {

	public MacOSKeychain() {
		super();
		this.type = KeystoreType.MACOSX;
	}

	@Override
	public String getSimpleLabel() {
		return StringEscapeUtils.unescapeJava(ResourceBundle.getBundle("bundles/nexu")
				.getString("product.selection.add.new.macos.keystore"));
	}

	@Override
	public String getLabel() {
		return StringEscapeUtils.unescapeJava(ResourceBundle.getBundle("bundles/nexu")
				.getString("product.selection.macos.keystore"));
	}

}

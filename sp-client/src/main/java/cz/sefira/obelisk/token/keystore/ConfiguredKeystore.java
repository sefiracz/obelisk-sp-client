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
package cz.sefira.obelisk.token.keystore;

import cz.sefira.obelisk.api.AbstractProduct;
import org.apache.commons.lang.StringEscapeUtils;
import java.text.MessageFormat;

import java.util.ResourceBundle;

/**
 * Represents a configured keystore.
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public class ConfiguredKeystore extends AbstractProduct {

	private String url;

	public ConfiguredKeystore() {
		super();
	}

	/**
	 * Returns the URL towards the configured keystore.
	 * @return The URL towards the configured keystore.
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Sets the URL towards the configured keystore.
	 * @param url The URL towards the configured keystore.
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public String getTooltip() {
		return getSimpleLabel();
	}

	@Override
	public String getSimpleLabel() {
		return this.getUrl().substring(this.getUrl().lastIndexOf('/') + 1) +" ("+this.getType().getLabel()+")";
	}

	@Override
	public String getLabel() {
		return StringEscapeUtils.unescapeJava(MessageFormat.format(
				ResourceBundle.getBundle("bundles/nexu").getString("product.selection.configured.keystore.button.label"),
				this.getType().getLabel(), this.getUrl().substring(this.getUrl().lastIndexOf('/') + 1)));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ConfiguredKeystore that = (ConfiguredKeystore) o;

		if (getCertificateId() != null && that.getCertificateId() != null) {
			if (!getCertificateId().equalsIgnoreCase(that.getCertificateId())) return false;
		}
		if (getCertificate() != null && that.getCertificate() != null) {
			if (!getCertificate().equals(that.getCertificate())) return false;
		}
//		if (getKeyAlias() != null && that.getKeyAlias() != null) {
//			if (!getKeyAlias().equals(that.getKeyAlias())) return false;
//		}
		return getUrl().equals(that.getUrl());
	}

	@Override
	public int hashCode() {
		int result = getCertificateId().hashCode();
		result = 31 * result + getKeyAlias().hashCode();
		result = 31 * result + getUrl().hashCode();
		return result;
	}
}

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
package cz.sefira.obelisk.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cz.sefira.obelisk.dss.DigestAlgorithm;
import cz.sefira.obelisk.dss.x509.CertificateToken;

public class GsonHelper {

	private static final Gson customGson = new GsonBuilder().disableHtmlEscaping()
			.registerTypeHierarchyAdapter(byte[].class, new ByteArrayTypeAdapter())
			.registerTypeAdapter(CertificateToken.class, new CertificateTypeAdapter())
			.registerTypeAdapter(DigestAlgorithm.class, new DigestAlgorithmAdapter())
			.create();

	public static String toJson(Object o) {
		return customGson.toJson(o);
	}

	public static <T> T fromJson(String json, Class<T> clasz) {
		return customGson.fromJson(json, clasz);
	}

}

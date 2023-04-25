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

import com.google.gson.*;

import cz.sefira.obelisk.dss.DSSException;
import cz.sefira.obelisk.dss.x509.CertificateToken;
import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Type;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CertificateTypeAdapter implements JsonSerializer<CertificateToken>, JsonDeserializer<CertificateToken> {

	@Override
	public CertificateToken deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		try {
			byte[] certBytes = Base64.decodeBase64(json.getAsString());
			X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X509")
					.generateCertificate(new ByteArrayInputStream(certBytes));
			return new CertificateToken(certificate);
		}
		catch (CertificateException e) {
			throw new DSSException(e);
		}
	}

	@Override
	public JsonElement serialize(CertificateToken src, Type typeOfSrc, JsonSerializationContext context) {
		return new JsonPrimitive(Base64.encodeBase64String(src.getEncoded()));
	}

}

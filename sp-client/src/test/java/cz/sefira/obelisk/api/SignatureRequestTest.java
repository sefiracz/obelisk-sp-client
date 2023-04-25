/**
 * Â© Nowina Solutions, 2015-2015
 *
 * ConceÌ�deÌ�e sous licence EUPL, version 1.1 ou â€“ deÌ€s leur approbation par la Commission europeÌ�enne - versions ulteÌ�rieures de lâ€™EUPL (la Â«LicenceÂ»).
 * Vous ne pouvez utiliser la preÌ�sente Å“uvre que conformeÌ�ment aÌ€ la Licence.
 * Vous pouvez obtenir une copie de la Licence aÌ€ lâ€™adresse suivante:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Sauf obligation leÌ�gale ou contractuelle eÌ�crite, le logiciel distribueÌ� sous la Licence est distribueÌ� Â«en lâ€™eÌ�tatÂ»,
 * SANS GARANTIES OU CONDITIONS QUELLES QUâ€™ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques speÌ�cifiques relevant de la Licence.
 */
package cz.sefira.obelisk.api;

import com.google.gson.Gson;
import cz.sefira.obelisk.api.ws.model.SignParameters;
import cz.sefira.obelisk.api.ws.model.SignatureRequest;
import cz.sefira.obelisk.json.GsonHelper;
import cz.sefira.obelisk.dss.DigestAlgorithm;
import org.junit.Assert;
import org.junit.Test;

public class SignatureRequestTest {

	@Test
	public void test1() {

		SignatureRequest obj = new SignatureRequest();
		SignParameters signParams = new SignParameters();
		signParams.setDigestAlgorithm(DigestAlgorithm.SHA1);
		obj.setSignParams(signParams);

		Gson gson = new Gson();
		String text = gson.toJson(obj);

		Assert.assertEquals("{\"signParams\":{\"digestAlgorithm\":\"SHA1\",\"useRsaPss\":false},\"userInteraction\":false}", text);

		SignatureRequest obj2 = gson.fromJson(text, SignatureRequest.class);

		Assert.assertEquals(obj.getSignParams().getDigestAlgorithm(), obj2.getSignParams().getDigestAlgorithm());

	}

	@Test
	public void test2() {

		SignatureRequest obj = new SignatureRequest();
		SignParameters signParams = new SignParameters();
		signParams.setDigestAlgorithm(DigestAlgorithm.SHA1);
		signParams.setToBeSigned("HelloWorld".getBytes());
		obj.setSignParams(signParams);

		String text = GsonHelper.toJson(obj);

		System.out.println(text);

		SignatureRequest obj2 = GsonHelper.fromJson(text, SignatureRequest.class);

		Assert.assertEquals(obj.getSignParams().getDigestAlgorithm(), obj2.getSignParams().getDigestAlgorithm());


	}

}

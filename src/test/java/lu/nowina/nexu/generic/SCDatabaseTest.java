/**
 * © Nowina Solutions, 2015-2015
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

import eu.europa.esig.dss.DigestAlgorithm;
import lu.nowina.nexu.api.DetectedCard;
import lu.nowina.nexu.api.EnvironmentInfo;
import lu.nowina.nexu.api.ScAPI;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.bind.JAXBContext;

public class SCDatabaseTest {

	@Test
	public void test1() throws Exception {

		SCDatabase db = new SCDatabase();

		ConnectionInfo cInfo = new ConnectionInfo();
		cInfo.setApiParam("param");
		cInfo.setSelectedApi(ScAPI.MSCAPI);
		cInfo.setEnv(EnvironmentInfo.buildFromSystemProperties(System.getProperties()));
		db.add(new DetectedCard("ADSF123FSDFS", 0), cInfo);

		db.getInfo("ADSF123FSDFS", null, null).getSupportedDigestAlgorithm().add(DigestAlgorithm.SHA1);
		db.getInfo("ADSF123FSDFS", null, null).getSupportedDigestAlgorithm().add(DigestAlgorithm.MD5);

		JAXBContext ctx = JAXBContext.newInstance(SCDatabase.class);
		ctx.createMarshaller().marshal(db, System.out);

	}

	@Test
	public void test2() throws Exception {

		SCDatabase db = new SCDatabase();

		ConnectionInfo cInfo = new ConnectionInfo();
		cInfo.setApiParam("param");
		cInfo.setSelectedApi(ScAPI.MSCAPI);
		cInfo.setEnv(EnvironmentInfo.buildFromSystemProperties(System.getProperties()));
		db.add(new DetectedCard("ATR1", 0), cInfo);

		Assert.assertEquals(1, db.getKeystores().size());
		Assert.assertEquals(1, ((SCInfo)db.getKeystores().get(0)).getInfos().size());

		ConnectionInfo cInfo2 = new ConnectionInfo();
		cInfo2.setApiParam("param");
		cInfo2.setSelectedApi(ScAPI.MSCAPI);
		cInfo2.setEnv(EnvironmentInfo.buildFromSystemProperties(System.getProperties()));
		db.add(new DetectedCard("ATR1", 0), cInfo2);

		Assert.assertEquals(1, db.getKeystores().size());
		Assert.assertEquals(2, ((SCInfo)db.getKeystores().get(0)).getInfos().size());

		ConnectionInfo cInfo3 = new ConnectionInfo();
		cInfo3.setApiParam("param");
		cInfo3.setSelectedApi(ScAPI.MSCAPI);
		cInfo3.setEnv(EnvironmentInfo.buildFromSystemProperties(System.getProperties()));
		db.add(new DetectedCard("ATR2", 0), cInfo3);

		Assert.assertEquals(2, db.getKeystores().size());
		Assert.assertEquals(2, ((SCInfo)db.getKeystores().get(0)).getInfos().size());
		Assert.assertEquals(1, ((SCInfo)db.getKeystores().get(1)).getInfos().size());
		Assert.assertTrue(db.getInfo("ATR1", null, null) == db.getKeystores().get(0));
		Assert.assertTrue(db.getInfo("ATR2", null, null) == db.getKeystores().get(1));

	}

}

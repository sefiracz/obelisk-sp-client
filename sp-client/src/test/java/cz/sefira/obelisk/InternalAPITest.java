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
package cz.sefira.obelisk;

import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.api.model.EnvironmentInfo;
import cz.sefira.obelisk.dss.token.DSSPrivateKeyEntry;
import cz.sefira.obelisk.dss.token.SignatureTokenConnection;
import cz.sefira.obelisk.token.pkcs11.DetectedCard;
import cz.sefira.obelisk.view.core.UIDisplay;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;

public class InternalAPITest {

	@Test
	public void testEnvironment() throws Exception {

		InternalAPI api = new InternalAPI(null, null, null, null, null, mock(AppConfig.class));

		EnvironmentInfo info = api.getEnvironmentInfo();
		Assert.assertNotNull(info.getOs());
		Assert.assertNotNull(info.getArch());
		Assert.assertNotNull(info.getOsName());
		Assert.assertNotNull(info.getOsVersion());
		Assert.assertNotNull(info.getJreVendor());

	}

	/**
	 * No card are detected
	 *
	 * @throws Exception
	 */
	@Test
	public void testDetectCards1() throws Exception {

		CardDetector detector = Mockito.mock(CardDetector.class);
    Mockito.when(detector.detectCards(true)).thenReturn(Arrays.asList());
		Assert.assertEquals(0, detector.detectCards(true).size());

	}

	@Test
	public void testDetectCards2() throws Exception {

		CardDetector detector = Mockito.mock(CardDetector.class);
		Mockito.when(detector.detectCards(true)).thenReturn(Arrays.asList(new DetectedCard("ATR", 0)));

		Assert.assertEquals(1, detector.detectCards(true).size());

	}

	@Test
	public void testDetectCards3() throws Exception {

		CardDetector detector = Mockito.mock(CardDetector.class);

		Mockito.when(detector.detectCards(true)).thenReturn(Arrays.asList(new DetectedCard("ATR1", 0), new DetectedCard("ATR2", 0)));

		Assert.assertEquals(2, detector.detectCards(true).size());

	}

	@Test
	public void testMatchingProductAdapter1() throws Exception {

		DetectedCard card = new DetectedCard("ATR", 0);

		UIDisplay display = Mockito.mock(UIDisplay.class);
		InternalAPI api = new InternalAPI(display, null, null, null, null, mock(AppConfig.class));

		SignatureTokenConnection c = new MockSignatureTokenConnection((DSSPrivateKeyEntry[])null);

		ProductAdapter adapter1 = Mockito.mock(ProductAdapter.class);
		Mockito.when(adapter1.accept(card)).thenReturn(Boolean.TRUE);
		Mockito.when(adapter1.connect(api, card, display.getPasswordInputCallback(card))).thenReturn(c);

		List<Match> matches = api.matchingProductAdapters(card);

		Assert.assertEquals(0, matches.size());

		api.registerProductAdapter(adapter1);

		matches = api.matchingProductAdapters(card);

		Assert.assertEquals(1, matches.size());

	}

}

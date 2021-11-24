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
package cz.sefira.obelisk.flow;

import cz.sefira.obelisk.NexuException;
import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.api.flow.BasicOperationStatus;
import eu.europa.esig.dss.DigestAlgorithm;
import eu.europa.esig.dss.ToBeSigned;
import eu.europa.esig.dss.token.JKSSignatureToken;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import cz.sefira.obelisk.AbstractConfigureLoggerTest;
import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.api.flow.Operation;
import cz.sefira.obelisk.api.flow.OperationFactory;
import cz.sefira.obelisk.api.flow.OperationResult;
import cz.sefira.obelisk.flow.operation.BasicOperationFactory;
import cz.sefira.obelisk.view.core.UIDisplay;
import cz.sefira.obelisk.view.core.UIOperation;
import org.junit.Assert;
import org.junit.Test;

import java.security.KeyStore.PasswordProtection;
import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SignatureFlowTest extends AbstractConfigureLoggerTest {

	@Test
	public void testCardRecognized() throws Exception {
		UIDisplay display = mock(UIDisplay.class);

		ProductAdapter adapter = mock(ProductAdapter.class);

		SignatureTokenConnection token = new JKSSignatureToken(this.getClass().getResourceAsStream("/keystore.jks"),
				new PasswordProtection("password".toCharArray()));

		NexuAPI api = mock(NexuAPI.class);
		DetectedCard detectedCard = new DetectedCard("atr", 0);
		when(api.detectCards(true)).thenReturn(Arrays.asList(detectedCard));
		when(api.matchingProductAdapters(detectedCard)).thenReturn(Arrays.asList(new Match(adapter, detectedCard)));
		when(api.registerTokenConnection(token)).thenReturn(new TokenId("id"));
		when(api.getTokenConnection(new TokenId("id"))).thenReturn(token);
		final AppConfig appConfig = new AppConfig();
		when(api.getAppConfig()).thenReturn(appConfig);

		when(adapter.connect(eq(api), eq(detectedCard), any())).thenReturn(token);

		SignatureRequest req = new SignatureRequest();
		req.setToBeSigned(new ToBeSigned("hello".getBytes()));
		req.setDigestAlgorithm(DigestAlgorithm.SHA256);

		final OperationFactory noUIOperationFactory = new NoUIOperationFactory();
		((NoUIOperationFactory) noUIOperationFactory).setDisplay(display);

		SignatureFlow flow = new SignatureFlow(display, api);
		flow.setOperationFactory(noUIOperationFactory);
		Execution<SignatureResponse> resp = flow.process(api, req);
		Assert.assertNotNull(resp);
		Assert.assertTrue(resp.isSuccess());
		Assert.assertNotNull(resp.getResponse());
		Assert.assertNotNull(resp.getResponse().getSignatureValue());

	}

	@Test(expected = NexuException.class)
	public void testInputValidation1() throws Exception {

		UIDisplay display = mock(UIDisplay.class);

		NexuAPI api = mock(NexuAPI.class);

		SignatureRequest req = new SignatureRequest();
		req.setDigestAlgorithm(DigestAlgorithm.SHA256);

		SignatureFlow flow = new SignatureFlow(display, api);
		flow.process(api, req);

	}

	@Test(expected = NexuException.class)
	public void testInputValidation2() throws Exception {

		UIDisplay display = mock(UIDisplay.class);

		NexuAPI api = mock(NexuAPI.class);

		SignatureRequest req = new SignatureRequest();
		req.setToBeSigned(new ToBeSigned());

		SignatureFlow flow = new SignatureFlow(display, api);
		flow.process(api, req);

	}

	@Test(expected = NexuException.class)
	public void testInputValidation3() throws Exception {

		UIDisplay display = mock(UIDisplay.class);

		NexuAPI api = mock(NexuAPI.class);

		SignatureRequest req = new SignatureRequest();
		req.setToBeSigned(new ToBeSigned("hello".getBytes()));

		SignatureFlow flow = new SignatureFlow(display, api);
		flow.process(api, req);

	}

	private static class NoUIOperationFactory extends BasicOperationFactory {

		@SuppressWarnings("rawtypes")
		private final Operation successOperation;

		public NoUIOperationFactory() {
			this.successOperation = mock(Operation.class);
			when(successOperation.perform()).thenReturn(new OperationResult<Void>(BasicOperationStatus.SUCCESS));
		}

		@Override
		@SuppressWarnings("unchecked")
		public <R, T extends Operation<R>> Operation<R> getOperation(Class<T> clazz, Object... params) {
			if (UIOperation.class.isAssignableFrom(clazz)) {
				return successOperation;
			} else {
				return super.getOperation(clazz, params);
			}
		}
	}
}

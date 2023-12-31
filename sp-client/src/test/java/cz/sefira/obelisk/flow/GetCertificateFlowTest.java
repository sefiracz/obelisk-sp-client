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

import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.api.flow.*;
import cz.sefira.obelisk.api.model.KeystoreType;
import cz.sefira.obelisk.api.model.ScAPI;
import cz.sefira.obelisk.api.ws.model.GetCertificateRequest;
import cz.sefira.obelisk.api.ws.model.GetCertificateResponse;
import cz.sefira.obelisk.dss.token.PasswordInputCallback;
import cz.sefira.obelisk.flow.operation.*;
import cz.sefira.obelisk.dss.token.SignatureTokenConnection;
import cz.sefira.obelisk.generic.SessionManager;
import cz.sefira.obelisk.token.keystore.ConfiguredKeystore;
import cz.sefira.obelisk.token.keystore.KeyStoreSignatureTokenConnection;
import cz.sefira.obelisk.token.keystore.KeystoreProductAdapter;
import cz.sefira.obelisk.token.keystore.NewKeystore;
import cz.sefira.obelisk.token.pkcs11.DetectedCard;
import cz.sefira.obelisk.view.core.UIDisplay;
import cz.sefira.obelisk.view.core.UIOperation;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import java.security.KeyStore.PasswordProtection;
import java.util.*;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetCertificateFlowTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testNewKeystore() throws Exception {
        final UIDisplay display = mock(UIDisplay.class);
        when(display.getPasswordInputCallback(null))
        .thenReturn(new PrefilledPasswordCallback(new PasswordProtection("password".toCharArray())));

        final PlatformAPI api = mock(PlatformAPI.class);
        final AppConfig appConfig = AppConfig.get();
        when(AppConfig.get()).thenReturn(appConfig);

        final Product selectedProduct = new NewKeystore();
        when(api.detectCards(true)).thenReturn(Collections.emptyList());
        when(api.matchingProductAdapters(selectedProduct)).thenReturn(
                Arrays.asList(new Match(new KeystoreProductAdapter(api), selectedProduct)));
        final Collection<SignatureTokenConnection> coll = new ArrayList<>();

        final ConfiguredKeystore configuredProduct = new ConfiguredKeystore();
        configuredProduct.setType(KeystoreType.JKS);
        configuredProduct.setUrl(this.getClass().getResource("/keystore.jks").toString());
        final OperationFactory operationFactory = new NoUIOperationFactory(selectedProduct, configuredProduct);
        ((NoUIOperationFactory) operationFactory).setDisplay(display);

        final GetCertificateFlow flow = new GetCertificateFlow(display, api);
        flow.setOperationFactory(operationFactory);
        final Execution<GetCertificateResponse> resp = flow.process(api, new GetCertificateRequest());

        try(final SignatureTokenConnection token = new KeyStoreSignatureTokenConnection(
                this.getClass().getResourceAsStream("/keystore.jks"), "JKS", new PasswordProtection("password".toCharArray()))){
            Assert.assertNotNull(resp);
            Assert.assertTrue(resp.isSuccess());
            Assert.assertNotNull(resp.getResult());
            Assert.assertEquals(token.getKeys().get(0).getCertificateToken(), resp.getResult().getCertificate());
//            Assert.assertEquals(token.getKeys().get(0).getEncryptionAlgorithm(),
//                    resp.getResponse().getEncryptionAlgorithm());
//            Assert.assertEquals(new TokenId("id"), resp.getResponse().getTokenId());
//            Assert.assertEquals(token.getKeys().get(0).getCertificate().getDSSIdAsString(), resp.getResponse().getKeyId());
//            Assert.assertNull(resp.getResponse().getPreferredDigest());
//            Assert.assertNull(resp.getResponse().getSupportedDigests());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNotRecognizedRequestSupport() throws Exception {
        final UIDisplay display = mock(UIDisplay.class);

        final PlatformAPI api = mock(PlatformAPI.class);
        final DetectedCard product = new DetectedCard("atr", 0);
        when(api.detectCards(true)).thenReturn(Arrays.asList(product));

        final AppConfig appConfig = AppConfig.get();
        when(AppConfig.get()).thenReturn(appConfig);

        final OperationFactory operationFactory = mock(OperationFactory.class);

        final Operation<Object> selectProductOperation = mock(Operation.class);
        when(selectProductOperation.perform()).thenReturn(new OperationResult<Object>(product));
        when(operationFactory.getOperation(eq(UIOperation.class), eq("/fxml/product-selection.fxml"),
                any(Object[].class))).thenReturn(selectProductOperation);

        final Operation<List<Match>> getMatchingCardAdaptersOperation = mock(Operation.class);
        when(getMatchingCardAdaptersOperation.perform())
        .thenReturn(new OperationResult<List<Match>>(Collections.emptyList()));
        when(operationFactory.getOperation(GetMatchingProductAdaptersOperation.class, Arrays.asList(product), api))
        .thenReturn(getMatchingCardAdaptersOperation);

        final Operation<List<Match>> configureProductOperation = mock(Operation.class);
        when(configureProductOperation.perform()).thenReturn(new OperationResult<List<Match>>(Collections.emptyList()));
        when(operationFactory.getOperation(ConfigureProductOperation.class, Collections.emptyList(), api))
        .thenReturn(configureProductOperation);

        final CreateTokenOperation createTokenOperation = new CreateTokenOperation();
        createTokenOperation.setParams(api, Collections.emptyList());
        createTokenOperation.setDisplay(display);
        createTokenOperation.setOperationFactory(operationFactory);
        when(operationFactory.getOperation(eq(CreateTokenOperation.class), eq("/fxml/api-selection.fxml"), any(Object[].class)))
        .thenReturn(createTokenOperation);
        when(operationFactory.getOperation(eq(CreateTokenOperation.class), eq(api), any(Object.class)))
        .thenReturn(createTokenOperation);

        final Operation<Object> returnFalseOperation = mock(Operation.class);
        when(returnFalseOperation.perform()).thenReturn(new OperationResult<Object>(false));
        when(operationFactory.getOperation(eq(UIOperation.class), eq("/fxml/unsupported-product.fxml"),
                any(Object[].class))).thenReturn(returnFalseOperation);

        final Operation<Object> returnScAPIOperation = mock(Operation.class);
        when(returnScAPIOperation.perform()).thenReturn(new OperationResult<Object>(ScAPI.PKCS_11));
        when(operationFactory.getOperation(eq(UIOperation.class), eq("/fxml/api-selection.fxml"),
                eq("unsupported.product.message"), any(String.class))).thenReturn(returnFalseOperation);



        when(operationFactory.getOperation(eq(UIOperation.class), eq("/fxml/message.fxml"),
                eq("unsupported.product.message"), any(String.class))).thenReturn(returnFalseOperation);



        when(operationFactory.getOperation(eq(UIOperation.class), eq("/fxml/message.fxml"),
                any(Object[].class))).thenReturn(returnScAPIOperation);

        final OperationResult<Object> or =  mock(OperationResult.class);
        when(or.getStatus()).thenReturn(BasicOperationStatus.USER_CANCEL);
        when(or.getResult()).thenReturn(Boolean.FALSE);
        final Operation<Object> returnOtherFalseOperation = mock(Operation.class);
        when(returnFalseOperation.perform()).thenReturn(or);
        when(operationFactory.getOperation(eq(UIOperation.class), eq("/fxml/pkcs11-params.fxml"),
                any(Object[].class))).thenReturn(returnOtherFalseOperation);




        final Operation<Object> successOperation = mock(Operation.class);
        when(successOperation.perform()).thenReturn(new OperationResult<Object>(BasicOperationStatus.SUCCESS));
        when(operationFactory.getOperation(eq(UIOperation.class), eq("/fxml/provide-feedback.fxml"),
                any(Object[].class))).thenReturn(successOperation);

        final GetCertificateFlow flow = new GetCertificateFlow(display, api);
        flow.setOperationFactory(operationFactory);

        final GetCertificateRequest req = new GetCertificateRequest();
        final Execution<GetCertificateResponse> resp = flow.process(api, req);
        Assert.assertNotNull(resp);
        Assert.assertFalse(resp.isSuccess());
        Assert.assertEquals(CoreOperationStatus.UNSUPPORTED_PRODUCT.getCode(), resp.getError());
    }

    @Test
    public void testCardRecognized() throws Exception {
        final UIDisplay display = mock(UIDisplay.class);
        final ProductAdapter adapter = mock(ProductAdapter.class);

        final SignatureTokenConnection token = new KeyStoreSignatureTokenConnection(
                this.getClass().getResourceAsStream("/keystore.jks"), "JKS", new PasswordProtection("password".toCharArray()));

        final PlatformAPI api = mock(PlatformAPI.class);
        final AppConfig appConfig = AppConfig.get();
        when(AppConfig.get()).thenReturn(appConfig);
        final DetectedCard detectedCard = new DetectedCard("atr", 0);
        when(adapter.getConfigurationOperation(api, detectedCard))
        .thenReturn(new NoOpFutureOperationInvocation<Product>(detectedCard));

        when(api.detectCards(true)).thenReturn(Arrays.asList(detectedCard));
        when(api.matchingProductAdapters(detectedCard)).thenReturn(Arrays.asList(new Match(adapter, detectedCard)));
        SessionManager.getManager().setToken(detectedCard, token);
        when(SessionManager.getManager().getInitializedTokenForProduct(detectedCard)).thenReturn(token);
        when(adapter.connect(eq(api), eq(detectedCard), any())).thenReturn(token);

        final OperationFactory operationFactory = new NoUIOperationFactory(detectedCard, null);
        ((NoUIOperationFactory) operationFactory).setDisplay(display);

        final GetCertificateFlow flow = new GetCertificateFlow(display, api);
        flow.setOperationFactory(operationFactory);
        final Execution<GetCertificateResponse> resp = flow.process(api, new GetCertificateRequest());
        final SignatureTokenConnection testToken = new KeyStoreSignatureTokenConnection(
                this.getClass().getResourceAsStream("/keystore.jks"), "JKS", new PasswordProtection("password".toCharArray()));
        Assert.assertNotNull(resp);
        Assert.assertTrue(resp.isSuccess());
        Assert.assertNotNull(resp.getResult());
        Assert.assertEquals(testToken.getKeys().get(0).getCertificateToken(), resp.getResult().getCertificate());
//        Assert.assertEquals(testToken.getKeys().get(0).getEncryptionAlgorithm(),
//                resp.getResponse().getEncryptionAlgorithm());
//        Assert.assertEquals(new TokenId("id"), resp.getResponse().getTokenId());
//        Assert.assertEquals(testToken.getKeys().get(0).getCertificate().getDSSIdAsString(),
//                resp.getResponse().getKeyId());
//        Assert.assertNull(resp.getResponse().getPreferredDigest());
//        Assert.assertNull(resp.getResponse().getSupportedDigests());
    }

    private static class NoUIOperationFactory extends BasicOperationFactory {

        @SuppressWarnings("rawtypes")
        private final Operation successOperation;
        @SuppressWarnings("rawtypes")
        private final Operation selectedProductOperation;
        @SuppressWarnings("rawtypes")
        private final Operation configureProductOperation;

        public NoUIOperationFactory(final Product selectedProduct, final Product configuredProduct) {
            this.successOperation = mock(Operation.class);
            when(this.successOperation.perform()).thenReturn(new OperationResult<Void>(BasicOperationStatus.SUCCESS));
            this.selectedProductOperation = mock(Operation.class);
            when(this.selectedProductOperation.perform()).thenReturn(new OperationResult<Product>(selectedProduct));

            if (configuredProduct != null) {
                this.configureProductOperation = mock(Operation.class);
                when(this.configureProductOperation.perform()).thenReturn(new OperationResult<Product>(configuredProduct));
            } else {
                this.configureProductOperation = null;
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <R, T extends Operation<R>> Operation<R> getOperation(final Class<T> clazz, final Object... params) {
            if (UIOperation.class.isAssignableFrom(clazz)) {
                switch ((String) params[0]) {
                    case "/fxml/product-selection.fxml":
                        return this.selectedProductOperation;
                    case "/fxml/configure-keystore.fxml":
                        return this.configureProductOperation;
                    default:
                        return this.successOperation;
                }
            } else {
                return super.getOperation(clazz, params);
            }
        }
    }

    private class PrefilledPasswordCallback implements PasswordInputCallback, Destroyable {

        private final PasswordProtection password;

        /**
         * The default constructor for PrefillPasswordCallback.
         *
         * @param password
         *            the password to use
         */
        public PrefilledPasswordCallback(PasswordProtection password) {
            this.password = password;
        }

        @Override
        public char[] getPassword() {
            return password.getPassword();
        }

        @Override
        public void destroy() throws DestroyFailedException {
            password.destroy();
        }

        @Override
        public boolean isDestroyed() {
            return password.isDestroyed();
        }

    }
}

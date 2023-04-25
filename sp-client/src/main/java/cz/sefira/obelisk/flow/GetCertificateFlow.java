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

import com.sun.jna.Platform;
import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.api.flow.BasicOperationStatus;
import cz.sefira.obelisk.api.flow.Operation;
import cz.sefira.obelisk.api.flow.OperationResult;
import cz.sefira.obelisk.api.ws.model.GetCertificateRequest;
import cz.sefira.obelisk.api.ws.model.GetCertificateResponse;
import cz.sefira.obelisk.flow.operation.*;
import cz.sefira.obelisk.generic.SessionManager;
import cz.sefira.obelisk.api.model.OS;
import cz.sefira.obelisk.token.macos.MacOSKeychain;
import cz.sefira.obelisk.token.windows.WindowsKeystore;
import cz.sefira.obelisk.view.DialogMessage;
import cz.sefira.obelisk.view.core.UIDisplay;
import cz.sefira.obelisk.view.core.UIOperation;
import cz.sefira.obelisk.dss.token.DSSPrivateKeyEntry;
import cz.sefira.obelisk.dss.token.SignatureTokenConnection;
import cz.sefira.obelisk.dss.x509.CertificateToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

class GetCertificateFlow extends AbstractCoreFlow<GetCertificateRequest, GetCertificateResponse> {

    static final Logger logger = LoggerFactory.getLogger(GetCertificateFlow.class);

    public GetCertificateFlow(final UIDisplay display, final PlatformAPI api) {
        super(display, api);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Execution<GetCertificateResponse> process(final PlatformAPI api, final GetCertificateRequest req) throws Exception {
    	SignatureTokenConnection token = null;
    	try {
    		while (true) {
          // SELECT PRODUCT DEVICE
          AbstractProduct selectedProduct = null;
          if (!req.isUserInteraction()) {
            // auto select product device
            if (OS.isWindows()) {
              selectedProduct = new WindowsKeystore();
            } else if (Platform.isMac()) {
              selectedProduct = new MacOSKeychain();
            } else {
              // TODO - hledat ručně?
            }
          } else {
            // let user select a product device (keystores, windows, smartcards)
            final Operation<Product> operation = this.getOperationFactory().getOperation(UIOperation.class,
                "/fxml/product-selection.fxml", new Object[] {api, getOperationFactory()});
            final OperationResult<Product> selectProductOperationResult = operation.perform();
            if (selectProductOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
              selectedProduct = (AbstractProduct) selectProductOperationResult.getResult();
            }
            else if (selectProductOperationResult.getStatus().equals(CoreOperationStatus.BACK)) {
              continue; // refresh button
            }
            else {
              return this.handleErrorOperationResult(selectProductOperationResult);
            }
          }

          // find usable adapters (keystore, windows, generic card adapter)
          List<Match> matchingProductAdapters;
          final OperationResult<List<Match>> getMatchingCardAdaptersOperationResult = this.getOperationFactory()
              .getOperation(GetMatchingProductAdaptersOperation.class, Collections.singletonList(selectedProduct), api).perform();
          if (getMatchingCardAdaptersOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
            matchingProductAdapters = getMatchingCardAdaptersOperationResult.getResult();
          } else {
            return this.handleErrorOperationResult(getMatchingCardAdaptersOperationResult);
          }

          // configure the product
          final OperationResult<List<Match>> configureProductOperationResult = this.getOperationFactory()
              .getOperation(ConfigureProductOperation.class, matchingProductAdapters, api).perform();
          if (configureProductOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
            matchingProductAdapters = configureProductOperationResult.getResult();
          } else {
            return this.handleErrorOperationResult(configureProductOperationResult);
          }

          // create token
          Map<TokenOperationResultKey, Object> map;
          final OperationResult<Map<TokenOperationResultKey, Object>> createTokenOperationResult = this.getOperationFactory()
              .getOperation(CreateTokenOperation.class, api, matchingProductAdapters, selectedProduct).perform();
          if (createTokenOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
            map = createTokenOperationResult.getResult();
            token = (SignatureTokenConnection) map.get(TokenOperationResultKey.TOKEN);
          } else if (createTokenOperationResult.getStatus().equals(CoreOperationStatus.BACK)) {
            continue; // go back from advanced token configuration
          } else {
            return this.handleErrorOperationResult(createTokenOperationResult);
          }
//          // connect to token
//          final OperationResult<SignatureTokenConnection> getTokenConnectionOperationResult = this.getOperationFactory()
//              .getOperation(GetTokenConnectionOperation.class, api, tokenId).perform();
//          if (getTokenConnectionOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
//            token = getTokenConnectionOperationResult.getResult();
//          } else {
//            return this.handleErrorOperationResult(getTokenConnectionOperationResult);
//          }

          // select key/certificate
          final Product product = (Product) map.get(TokenOperationResultKey.SELECTED_PRODUCT);
          final ProductAdapter productAdapter = (ProductAdapter) map.get(TokenOperationResultKey.SELECTED_PRODUCT_ADAPTER);
          final OperationResult<DSSPrivateKeyEntry> selectPrivateKeyOperationResult = this.getOperationFactory()
              .getOperation(UserSelectPrivateKeyOperation.class,
                  token, api, productAdapter, req.getCertificateFilter(), req.isUserInteraction()).perform();
          if (selectPrivateKeyOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
            final DSSPrivateKeyEntry key = selectPrivateKeyOperationResult.getResult();

            // save full config
            this.getOperationFactory().getOperation(SaveFullSelectionOperation.class, product, productAdapter,
                    key, map).perform();

            final GetCertificateResponse resp = new GetCertificateResponse();
            // signing certificate
            final CertificateToken certificate = key.getCertificateToken();
            resp.setCertificate(certificate);
            // certificate chain
            final CertificateToken[] certificateChain = key.getCertificateChain();
            if (certificateChain != null) {
              resp.setCertificateChain(certificateChain);
            }

            // certificate obtained dialog - if user interaction is wanted (no auto-select mode)
            if (req.isUserInteraction()) {
              DialogMessage certFlowFinished = new DialogMessage("certificates.flow.finished",
                  DialogMessage.Level.SUCCESS, 400, 165);
              certFlowFinished.setShowDoNotShowCheckbox(true, false, "cert-flow-finished");
              this.getOperationFactory().getMessageDialog(api, certFlowFinished, false);
            }

            return new Execution<>(resp, selectedProduct);
          } else if (selectPrivateKeyOperationResult.getStatus().equals(CoreOperationStatus.BACK)) {
            continue; // go back from key selection
          } else {
            return this.handleErrorOperationResult(selectPrivateKeyOperationResult);
          }
    		}
    	} catch (final Exception e) {
    		logger.error("Flow error", e);
        SessionManager.getManager().destroy();
    		throw this.handleException(e);
    	}
    }
}

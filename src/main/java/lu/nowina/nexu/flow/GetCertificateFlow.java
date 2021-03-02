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
package lu.nowina.nexu.flow;

import eu.europa.esig.dss.DSSASN1Utils;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import eu.europa.esig.dss.x509.CertificateToken;
import lu.nowina.nexu.Utils;
import lu.nowina.nexu.api.*;
import lu.nowina.nexu.api.flow.BasicOperationStatus;
import lu.nowina.nexu.api.flow.Operation;
import lu.nowina.nexu.api.flow.OperationResult;
import lu.nowina.nexu.flow.operation.*;
import lu.nowina.nexu.view.DialogMessage;
import lu.nowina.nexu.view.core.UIDisplay;
import lu.nowina.nexu.view.core.UIOperation;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.x500.X500Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

class GetCertificateFlow extends AbstractCoreFlow<GetCertificateRequest, GetCertificateResponse> {

    static final Logger logger = LoggerFactory.getLogger(GetCertificateFlow.class);

    public GetCertificateFlow(final UIDisplay display, final NexuAPI api) {
        super(display, api);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Execution<GetCertificateResponse> process(final NexuAPI api, final GetCertificateRequest req) throws Exception {
    	SignatureTokenConnection token = null;
    	try {
    		while (true) {
    		  // check session validity
          final OperationResult<Boolean> sessionValidityResult = this.getOperationFactory()
                  .getOperation(CheckSessionValidityOperation.class, req.getSessionValue()).perform();
          if(sessionValidityResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {

            // select product device
            final AbstractProduct selectedProduct;
            // select a product (keystores, windows, smartcards)
            final Operation<Product> operation = this.getOperationFactory().getOperation(UIOperation.class,
                    "/fxml/product-selection.fxml", new Object[]{api.getAppConfig().getApplicationName(), api});
            final OperationResult<Product> selectProductOperationResult = operation.perform();
            if (selectProductOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
              selectedProduct = (AbstractProduct) selectProductOperationResult.getResult();

            } else if (selectProductOperationResult.getStatus().equals(CoreOperationStatus.BACK)) {
              continue; // refresh button
            } else {
              return this.handleErrorOperationResult(selectProductOperationResult);
            }

            // find usable adapters (keystore, windows, generic card adapter)
            final OperationResult<List<Match>> getMatchingCardAdaptersOperationResult = this.getOperationFactory()
                .getOperation(GetMatchingProductAdaptersOperation.class, Arrays.asList(selectedProduct), api).perform();
            if (getMatchingCardAdaptersOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
              List<Match> matchingProductAdapters = getMatchingCardAdaptersOperationResult.getResult();

              // configure the product
              final OperationResult<List<Match>> configureProductOperationResult = this.getOperationFactory()
                  .getOperation(ConfigureProductOperation.class, matchingProductAdapters, api).perform();
              if (configureProductOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
                matchingProductAdapters = configureProductOperationResult.getResult();

                // create token
                final OperationResult<Map<TokenOperationResultKey, Object>> createTokenOperationResult = this.getOperationFactory()
                    .getOperation(CreateTokenOperation.class, api, matchingProductAdapters, selectedProduct).perform();
                if (createTokenOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
                  final Map<TokenOperationResultKey, Object> map = createTokenOperationResult.getResult();
                  final TokenId tokenId = (TokenId) map.get(TokenOperationResultKey.TOKEN_ID);

                  // connect to token
                  final OperationResult<SignatureTokenConnection> getTokenConnectionOperationResult = this.getOperationFactory()
                      .getOperation(GetTokenConnectionOperation.class, api, tokenId).perform();
                  if (getTokenConnectionOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
                    token = getTokenConnectionOperationResult.getResult();

                    // select key/certificate
                    final Product product = (Product) map.get(TokenOperationResultKey.SELECTED_PRODUCT);
                    final ProductAdapter productAdapter = (ProductAdapter) map.get(TokenOperationResultKey.SELECTED_PRODUCT_ADAPTER);
                    final OperationResult<DSSPrivateKeyEntry> selectPrivateKeyOperationResult = this.getOperationFactory()
                        .getOperation(UserSelectPrivateKeyOperation.class, token, api, product, productAdapter, req.getCertificateFilter()).perform();
                    if (selectPrivateKeyOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
                      final DSSPrivateKeyEntry key = selectPrivateKeyOperationResult.getResult();

                      // save full config
                      this.getOperationFactory().getOperation(SaveFullSelectionOperation.class, product, productAdapter,
                              key, map).perform();

                      final GetCertificateResponse resp = new GetCertificateResponse();
                      // signing certificate
                      final CertificateToken certificate = key.getCertificate();
                      resp.setCertificate(certificate);
                      resp.setEncryptionAlgorithm(certificate.getEncryptionAlgorithm());
                      // certificate chain
                      final CertificateToken[] certificateChain = key.getCertificateChain();
                      if (certificateChain != null) {
                        resp.setCertificateChain(certificateChain);
                      }
                      // subject info
                      X500Principal subjectX500Principal = certificate.getSubjectX500Principal();
                      resp.setSubjectCN(DSSASN1Utils.extractAttributeFromX500Principal(BCStyle.CN, subjectX500Principal));
                      resp.setSubjectOrg(DSSASN1Utils.extractAttributeFromX500Principal(BCStyle.O, subjectX500Principal));
                      resp.setNotBefore(Utils.formatXsDateTime(certificate.getNotBefore()));
                      resp.setNotAfter(Utils.formatXsDateTime(certificate.getNotAfter()));
                      resp.setSerialNumber(certificate.getSerialNumber());
                      // issuer info
                      X500Principal issuerX500Principal = certificate.getIssuerX500Principal();
                      resp.setIssuerCN(DSSASN1Utils.extractAttributeFromX500Principal(BCStyle.CN, issuerX500Principal));
                      resp.setIssuerOrg(DSSASN1Utils.extractAttributeFromX500Principal(BCStyle.O, issuerX500Principal));

                      this.getOperationFactory().getMessageDialog(api, new DialogMessage(
                              "certificates.flow.finished", DialogMessage.Level.INFORMATION,
                              400, 150), true);

                      return new Execution<GetCertificateResponse>(resp);
                    } else if (selectPrivateKeyOperationResult.getStatus().equals(CoreOperationStatus.BACK)) {
                      continue; // go back from key selection
                    } else {
                      return this.handleErrorOperationResult(selectPrivateKeyOperationResult);
                    }
                  } else {
                    return this.handleErrorOperationResult(getTokenConnectionOperationResult);
                  }
                } else if (createTokenOperationResult.getStatus().equals(CoreOperationStatus.BACK)) {
                  continue; // go back from advanced token configuration
                } else {
                  return this.handleErrorOperationResult(createTokenOperationResult);
                }
              } else {
                return this.handleErrorOperationResult(configureProductOperationResult);
              }
            } else {
              return this.handleErrorOperationResult(getMatchingCardAdaptersOperationResult);
            }
          } else {
            return this.handleErrorOperationResult(sessionValidityResult); // INVALID SESSION ?
          }
    		}
    	} catch (final Exception e) {
    		logger.error("Flow error", e);
				closeToken(token);
    		throw this.handleException(e);
    	}
    }
}

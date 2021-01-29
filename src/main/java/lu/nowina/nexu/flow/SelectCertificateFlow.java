package lu.nowina.nexu.flow;

/*
 * Copyright 2020 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.flow.SelectCertificateFlow
 *
 * Created: 16.12.2020
 * Author: hlavnicka
 */

import eu.europa.esig.dss.DigestAlgorithm;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import eu.europa.esig.dss.x509.CertificateToken;
import lu.nowina.nexu.Utils;
import lu.nowina.nexu.api.*;
import lu.nowina.nexu.api.flow.BasicOperationStatus;
import lu.nowina.nexu.api.flow.Operation;
import lu.nowina.nexu.api.flow.OperationResult;
import lu.nowina.nexu.flow.operation.*;
import lu.nowina.nexu.generic.ProductMapHandler;
import lu.nowina.nexu.view.core.UIDisplay;
import lu.nowina.nexu.view.core.UIOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * description
 */
public class SelectCertificateFlow extends AbstractCoreFlow<SelectCertificateRequest, SelectCertificateResponse> {

  static final Logger logger = LoggerFactory.getLogger(SelectCertificateFlow.class);

  public SelectCertificateFlow(final UIDisplay display, final NexuAPI api) {
    super(display, api);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Execution<SelectCertificateResponse> process(final NexuAPI api, final SelectCertificateRequest req) throws Exception {
    api.detectAll();
    CertificateToken certificateToken = req.getCertificate();
    byte[] digest = certificateToken.getDigest(DigestAlgorithm.SHA256);
    String certificateId = Utils.encodeHexString(digest);
    List<AbstractProduct> products = ProductMapHandler.getInstance().get(certificateId);
    AbstractProduct selectedProduct;
    // manual select certificate/key
    if(products == null || products.isEmpty()) {
      // given certificate not known - information dialog
      final OperationResult<Object> result = this.getOperationFactory().getOperation(UIOperation.class,
          "/fxml/unknown-certificate.fxml", new Object[] {api.getAppConfig().getApplicationName(),
              "certificates.flow.manual", certificateToken
      }).perform();
      if(result.getStatus().equals(BasicOperationStatus.USER_CANCEL)) {
        return this.handleErrorOperationResult(result);
      }
      // manual select certificate/key
      GetCertificateRequest request = new GetCertificateRequest();
      request.setCertificateFilter(new CertificateFilter(certificateToken.getDigest(DigestAlgorithm.SHA1)));
      Execution<GetCertificateResponse> getCertificate = api.getCertificate(request);
      if(!getCertificate.isSuccess()) {
        // manual selection error
        return this.handleErrorOperationResult(getCertificate.getOperationResult());
      }
      products = ProductMapHandler.getInstance().get(certificateId);
    }

    if(products == null || products.isEmpty()) {
      throw new IllegalStateException("Unable to find a manually stored keystore/device (product).");
    } else if (products.size() > 1) {
      // private key is found on multiple devices - selection dialog
      final Operation<AbstractProduct> operation = this.getOperationFactory()
          .getOperation(UIOperation.class, "/fxml/product-collision.fxml", api, products);
      final OperationResult<AbstractProduct> selectProductOperationResult = operation.perform();
      if (selectProductOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
        selectedProduct = selectProductOperationResult.getResult();
      } else {
        return this.handleErrorOperationResult(selectProductOperationResult);
      }
    } else {
      selectedProduct = products.get(0);
    }

    SignatureTokenConnection token = null;
    try {
      while (true) {
        final OperationResult<List<Match>> getMatchingCardAdaptersOperationResult = this.getOperationFactory()
            .getOperation(GetMatchingProductAdaptersOperation.class, Arrays.asList(selectedProduct), api).perform();
        if (getMatchingCardAdaptersOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
          List<Match> matchingProductAdapters = getMatchingCardAdaptersOperationResult.getResult();

          final OperationResult<List<Match>> configureProductOperationResult = this.getOperationFactory()
              .getOperation(ConfigureProductOperation.class, matchingProductAdapters, api).perform();
          if (configureProductOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
            matchingProductAdapters = configureProductOperationResult.getResult();

            final OperationResult<Map<TokenOperationResultKey, Object>> createTokenOperationResult = this.getOperationFactory()
                .getOperation(CreateTokenOperation.class, api, matchingProductAdapters, selectedProduct).perform();
            if (createTokenOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
              final Map<TokenOperationResultKey, Object> map = createTokenOperationResult.getResult();
              final TokenId tokenId = (TokenId) map.get(TokenOperationResultKey.TOKEN_ID);

              final OperationResult<SignatureTokenConnection> getTokenConnectionOperationResult = this.getOperationFactory()
                  .getOperation(GetTokenConnectionOperation.class, api, tokenId).perform();
              if (getTokenConnectionOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
                token = getTokenConnectionOperationResult.getResult();

                final Product product = (Product) map.get(TokenOperationResultKey.SELECTED_PRODUCT);
                final ProductAdapter productAdapter = (ProductAdapter) map.get(TokenOperationResultKey.SELECTED_PRODUCT_ADAPTER);
                final OperationResult<DSSPrivateKeyEntry> selectPrivateKeyOperationResult = this.getOperationFactory()
                    .getOperation(AutoSelectPrivateKeyOperation.class, token, api, product, productAdapter, selectedProduct.getKeyAlias()).perform();
                if (selectPrivateKeyOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
                  final DSSPrivateKeyEntry key = selectPrivateKeyOperationResult.getResult();

                  final SelectCertificateResponse resp = new SelectCertificateResponse();
                  resp.setTokenId(tokenId);

                  final CertificateToken certificate = key.getCertificate();
                  resp.setCertificate(certificate);
                  resp.setKeyId(certificate.getDSSIdAsString());
                  resp.setEncryptionAlgorithm(certificate.getEncryptionAlgorithm());

                  final CertificateToken[] certificateChain = key.getCertificateChain();
                  if (certificateChain != null) {
                    resp.setCertificateChain(certificateChain);
                  }

                  if (productAdapter.canReturnSuportedDigestAlgorithms(product)) {
                    resp.setSupportedDigests(productAdapter.getSupportedDigestAlgorithms(product));
                    resp.setPreferredDigest(productAdapter.getPreferredDigestAlgorithm(product));
                  }

                  return new Execution<SelectCertificateResponse>(resp);
                } else if (selectPrivateKeyOperationResult.getStatus().equals(CoreOperationStatus.BACK)) {
                  continue;
                } else {
                  return this.handleErrorOperationResult(selectPrivateKeyOperationResult);
                }
              } else {
                return this.handleErrorOperationResult(getTokenConnectionOperationResult);
              }
            } else {
              return this.handleErrorOperationResult(createTokenOperationResult);
            }
          } else {
            return this.handleErrorOperationResult(configureProductOperationResult);
          }
        } else {
          return this.handleErrorOperationResult(getMatchingCardAdaptersOperationResult);
        }
      }
    } catch (final Exception e) {
      logger.error("Flow error", e);
      throw this.handleException(e);
    } finally {
      if (token != null) {
        if (req.isCloseToken()) {
          try {
            token.close();
          } catch (final Exception e) {
            logger.error("Exception when closing token", e);
          }
        }
      }
    }
  }
}

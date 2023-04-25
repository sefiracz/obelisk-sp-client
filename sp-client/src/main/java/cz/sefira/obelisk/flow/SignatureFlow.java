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

import cz.sefira.obelisk.AppException;
import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.api.flow.BasicOperationStatus;
import cz.sefira.obelisk.api.flow.Operation;
import cz.sefira.obelisk.api.flow.OperationResult;
import cz.sefira.obelisk.api.ws.model.*;
import cz.sefira.obelisk.dss.DigestAlgorithm;
import cz.sefira.obelisk.dss.x509.CertificateToken;
import cz.sefira.obelisk.flow.operation.*;
import cz.sefira.obelisk.generic.QuickAccessProductsMap;
import cz.sefira.obelisk.generic.SessionManager;
import cz.sefira.obelisk.util.TextUtils;
import cz.sefira.obelisk.view.core.UIDisplay;
import cz.sefira.obelisk.dss.SignatureValue;
import cz.sefira.obelisk.dss.token.DSSPrivateKeyEntry;
import cz.sefira.obelisk.dss.token.SignatureTokenConnection;
import cz.sefira.obelisk.view.core.UIOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

class SignatureFlow extends AbstractCoreFlow<SignatureRequest, SignatureResponse> {

  private static final Logger logger = LoggerFactory.getLogger(SignatureFlow.class.getName());

  public SignatureFlow(UIDisplay display, PlatformAPI api) {
    super(display, api);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Execution<SignatureResponse> process(PlatformAPI api, SignatureRequest req) throws Exception {
    SignatureTokenConnection token = null;
    try {
      if (req.getSignParams() == null) {
        throw new AppException("SignParams is null");
      }
      SignParameters signParams = req.getSignParams();
      // check mandatory values
      if (signParams.getCertificate() == null || signParams.getCertificate().getCertificate() == null) {
        throw new AppException("Certificate is null");
      }
      // check mandatory values
      if (signParams.getToBeSigned() == null) {
        throw new AppException("ToBeSigned is null");
      }
      if ((signParams.getDigestAlgorithm() == null)) {
        throw new AppException("Digest algorithm expected");
      }

      // find correct product device
      CertificateToken certificateToken = signParams.getCertificate();
      byte[] digest = certificateToken.getDigest(DigestAlgorithm.SHA256);
      String certificateId = TextUtils.encodeHexString(digest);

      List<AbstractProduct> products = QuickAccessProductsMap.access().get(certificateId);
      ProductAdapter productAdapter = null;
      AbstractProduct selectedProduct = null;

      // manual select certificate/key
      if (products == null || products.isEmpty()) { // TODO - possibly to do without user interaction??
        // given certificate not known - information dialog
        final OperationResult<Object> result = this.getOperationFactory().getOperation(UIOperation.class,
            "/fxml/unknown-certificate.fxml", new Object[] {api, certificateToken}).perform();
        if (result.getStatus().equals(BasicOperationStatus.USER_CANCEL)) {
          return this.handleErrorOperationResult(result);
        }
        // manual select certificate/key
        GetCertificateRequest request = new GetCertificateRequest();
        request.setCertificateFilter(new CertificateFilter(certificateToken.getDigest(DigestAlgorithm.SHA256)));
        request.setUserInteraction(true);
        Execution<GetCertificateResponse> getCertificate = api.getCertificate(request);
        if (!getCertificate.isSuccess()) {
          // manual selection error
          return this.handleErrorOperationResult(getCertificate.getOperationResult());
        }
        products = QuickAccessProductsMap.access().get(certificateId);
      }

      if (products == null || products.isEmpty()) {
        return this.handleErrorOperationResult(new OperationResult<>(CoreOperationStatus.NO_PRODUCT_FOUND));
      }
      else if (products.size() > 1) {
        // private key is found on multiple devices - selection dialog
        while (true) {
          final Operation<AbstractProduct> operation = this.getOperationFactory()
              .getOperation(UIOperation.class, "/fxml/product-collision.fxml", api, getOperationFactory(), products);
          final OperationResult<AbstractProduct> selectProductOperationResult = operation.perform();
          if (selectProductOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
            selectedProduct = selectProductOperationResult.getResult();
            break;
          }
          else if (!selectProductOperationResult.getStatus().equals(CoreOperationStatus.BACK)) {
            return this.handleErrorOperationResult(selectProductOperationResult);
          }
        }
      }
      else {
        selectedProduct = products.get(0);
      }

      List<Match> matchingProductAdapters;
      final OperationResult<List<Match>> getMatchingCardAdaptersOperationResult = this.getOperationFactory().getOperation(GetMatchingProductAdaptersOperation.class, Collections.singletonList(selectedProduct), api).perform();
      if (getMatchingCardAdaptersOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
        matchingProductAdapters = getMatchingCardAdaptersOperationResult.getResult();
      } else {
        return this.handleErrorOperationResult(getMatchingCardAdaptersOperationResult);
      }
      // configure the product
      final OperationResult<List<Match>> configureProductOperationResult = this.getOperationFactory().getOperation(ConfigureProductOperation.class, matchingProductAdapters, api).perform();
      if (configureProductOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
        matchingProductAdapters = configureProductOperationResult.getResult();
      } else {
        return this.handleErrorOperationResult(configureProductOperationResult);
      }
      // find token
      token = SessionManager.getManager().getInitializedTokenForProduct(selectedProduct);
      if (token == null) {
        // Create token
        final OperationResult<Map<TokenOperationResultKey, Object>> createTokenOperationResult = this.getOperationFactory().getOperation(CreateTokenOperation.class, api, matchingProductAdapters, selectedProduct, certificateId).perform();
        if (createTokenOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
          final Map<TokenOperationResultKey, Object> map = createTokenOperationResult.getResult();
          token = (SignatureTokenConnection) map.get(TokenOperationResultKey.TOKEN);
          productAdapter = (ProductAdapter) map.get(TokenOperationResultKey.SELECTED_PRODUCT_ADAPTER);
        }
        else {
          return this.handleErrorOperationResult(createTokenOperationResult);
        }
      }
      logger.info("Token: " + token);

      // select key
      DSSPrivateKeyEntry key;
      final OperationResult<DSSPrivateKeyEntry> selectPrivateKeyOperationResult = getOperationFactory().getOperation(TokenPrivateKeyOperation.class, token, api, certificateId).perform();
      if (selectPrivateKeyOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
        key = selectPrivateKeyOperationResult.getResult();
        logger.info("Key " + key + " " + key.getCertificateToken().getCertificate().getSubjectDN() + " from " + key.getCertificateToken().getCertificate().getIssuerDN());
      }
      // TODO mandatory user interaction at signature flow?
      else if (req.isUserInteraction() && selectPrivateKeyOperationResult.getStatus().equals(CoreOperationStatus.NO_KEY)) {
        final Operation<Boolean> notFoundOperation = this.getOperationFactory()
            .getOperation(UIOperation.class,
                "/fxml/certificate-not-found.fxml", api, certificateToken);
        final OperationResult<Boolean> notFoundOperationResult = notFoundOperation.perform();
        if (notFoundOperationResult.getStatus().equals(BasicOperationStatus.USER_CANCEL) ||
            !notFoundOperationResult.getResult()) {
          // user will fix profile
          return this.handleErrorOperationResult(selectPrivateKeyOperationResult);
        }
        else {
          // repeat the process with certificate forgotten to make user find it again
          productAdapter.removeProduct(selectedProduct);
          QuickAccessProductsMap.access().remove(certificateId, selectedProduct);
          return process(api, req);
        }
      }
      else {
        return this.handleErrorOperationResult(selectPrivateKeyOperationResult);
      }

      // sign data
      final OperationResult<SignatureValue> signOperationResult = getOperationFactory().getOperation(
          SignOperation.class, token, api, signParams, key).perform();
      if(signOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
        final SignatureValue value = signOperationResult.getResult();
        logger.info("Signature created: " + value);
        SignatureResponse resp = new SignatureResponse(value, key.getCertificateToken(), key.getCertificateChain());
        return new Execution<>(resp, selectedProduct);
      } else {
        return handleErrorOperationResult(signOperationResult);
      }
    } catch (Exception e) {
      logger.error("Flow error", e);
      SessionManager.getManager().destroy();
      throw handleException(e);
    }
  }
}

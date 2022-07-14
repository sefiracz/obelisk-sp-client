/**
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package cz.sefira.obelisk.flow;

/*
 * Copyright 2020 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.flow.SelectCertificateFlow
 *
 * Created: 16.12.2020
 * Author: hlavnicka
 */

import cz.sefira.obelisk.NexuException;
import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.api.flow.BasicOperationStatus;
import cz.sefira.obelisk.flow.operation.*;
import cz.sefira.obelisk.macos.keystore.MacOSKeychain;
import cz.sefira.obelisk.windows.keystore.WindowsKeystore;
import eu.europa.esig.dss.DigestAlgorithm;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import eu.europa.esig.dss.x509.CertificateToken;
import cz.sefira.obelisk.Utils;
import cz.sefira.obelisk.api.flow.Operation;
import cz.sefira.obelisk.api.flow.OperationResult;
import cz.sefira.obelisk.generic.RegisteredProducts;
import cz.sefira.obelisk.generic.SessionManager;
import cz.sefira.obelisk.view.core.UIDisplay;
import cz.sefira.obelisk.view.core.UIOperation;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Automatically open device and select a private key based on certificate request
 */
public class GetTokenFlow extends AbstractCoreFlow<GetTokenRequest, GetTokenResponse> {

  static final Logger logger = LoggerFactory.getLogger(GetTokenFlow.class);

  public GetTokenFlow(final UIDisplay display, final NexuAPI api) {
    super(display, api);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Execution<GetTokenResponse> process(final NexuAPI api, final GetTokenRequest req) throws Exception {
    SignatureTokenConnection token = null;
    try {
      // check session validity
      final OperationResult<Boolean> sessionValidityResult = this.getOperationFactory()
              .getOperation(CheckSessionValidityOperation.class, req.getSessionValue()).perform();
      if(sessionValidityResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {

        // check mandatory values
        if (req.getCertificate() == null || req.getCertificate().getCertificate() == null) {
          throw new NexuException("Certificate is null");
        }

        // find correct product device
        CertificateToken certificateToken = req.getCertificate();
        byte[] digest = certificateToken.getDigest(DigestAlgorithm.SHA256);
        String certificateId = Utils.encodeHexString(digest);
        List<AbstractProduct> products = RegisteredProducts.getMap().get(certificateId);
        AbstractProduct selectedProduct = null;

        try {
          // try the given location first
          if ((products == null || products.isEmpty()) && req.getCertificateLocation() != null &&
              req.getCertificateLocation().getType() != null) {
            CertificateLocation certificateLocation = req.getCertificateLocation();
            String alias = certificateLocation.getAlias();
            KeystoreType type = certificateLocation.getType();
            String certificate = Base64.encodeBase64String(certificateToken.getEncoded());
            switch (type) {
              case JKS:
              case JCEKS:
              case PKCS12:
                selectedProduct = new ConfiguredKeystore();
                String param = certificateLocation.getParam();
                File keystoreFile = new File(param);
                if (!keystoreFile.exists() || !keystoreFile.isFile() || !keystoreFile.canRead()) {
                  throw new IllegalStateException("Keystore file not available");
                }
                ((ConfiguredKeystore)selectedProduct).setUrl(keystoreFile.toURI().toString());
                break;
              case PKCS11:
                selectedProduct = new DetectedCard();
                // TODO - check token info is correct and the needed attributes are present
                CertificateLocation.TokenInfo tokenInfo = certificateLocation.getTokenInfo();
                ((DetectedCard)selectedProduct).setAtr(tokenInfo.getAtr());
                ((DetectedCard)selectedProduct).setTokenLabel(tokenInfo.getTokenLabel());
                ((DetectedCard)selectedProduct).setTokenSerial(tokenInfo.getTokenSerial());
                ((DetectedCard)selectedProduct).setTokenManufacturer(tokenInfo.getTokenManufacturer());
                break;
              case WINDOWS:
                selectedProduct = new WindowsKeystore();
                break;
              case MACOSX:
                selectedProduct = new MacOSKeychain();
                break;
              case UNKNOWN:
                throw new IllegalStateException("Unknown keystore type");
            }
            selectedProduct.setKeyAlias(alias);
            selectedProduct.setCertificate(certificate);
            selectedProduct.setCertificateId(certificateId);
            selectedProduct.setType(type);
          }
        } catch (Exception e) {
          // TODO
          // ignore? fail dialog and asking to chose?
          selectedProduct = null;
        }

        if (selectedProduct == null) {
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
            request.setSessionValue(req.getSessionValue());
            request.setCertificateFilter(new CertificateFilter(certificateToken.getDigest(DigestAlgorithm.SHA1)));
            Execution<GetCertificateResponse> getCertificate = api.getCertificate(request);
            if(!getCertificate.isSuccess()) {
              // manual selection error
              return this.handleErrorOperationResult(getCertificate.getOperationResult());
            }
            products = RegisteredProducts.getMap().get(certificateId);
          }

          if(products == null || products.isEmpty()) {
            throw new IllegalStateException("Unable to find a manually stored keystore/device (product).");
          } else if (products.size() > 1) {
            // private key is found on multiple devices - selection dialog
            while (true) {
              final Operation<AbstractProduct> operation = this.getOperationFactory()
                  .getOperation(UIOperation.class, "/fxml/product-collision.fxml", api, getOperationFactory(), products);
              final OperationResult<AbstractProduct> selectProductOperationResult = operation.perform();
              if (selectProductOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
                selectedProduct = selectProductOperationResult.getResult();
                break;
              } else if (!selectProductOperationResult.getStatus().equals(CoreOperationStatus.BACK)) {
                return this.handleErrorOperationResult(selectProductOperationResult);
              }
            }
          } else {
            selectedProduct = products.get(0);
          }
        }

        while (true) {
          // find usable adapters (keystore, windows, generic card adapter)
          final OperationResult<List<Match>> getMatchingCardAdaptersOperationResult = this.getOperationFactory()
              .getOperation(GetMatchingProductAdaptersOperation.class, Collections.singletonList(selectedProduct), api).perform();
          if (getMatchingCardAdaptersOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
            List<Match> matchingProductAdapters = getMatchingCardAdaptersOperationResult.getResult();
            // configure the product
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
                      .getOperation(AutoSelectPrivateKeyOperation.class, token, api, product, productAdapter,
                          selectedProduct.getKeyAlias(), req.getCertificate().getCertificate()).perform();
                  if (selectPrivateKeyOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
                    final DSSPrivateKeyEntry key = selectPrivateKeyOperationResult.getResult();

                    final GetTokenResponse resp = new GetTokenResponse();
                    resp.setTokenId(tokenId);

                    final CertificateToken certificate = key.getCertificate();
                    resp.setCertificate(certificate);
                    resp.setKeyId(certificate.getDSSIdAsString());
                    resp.setEncryptionAlgorithm(certificate.getEncryptionAlgorithm());

                    final CertificateToken[] certificateChain = key.getCertificateChain();
                    if (certificateChain != null) {
                      resp.setCertificateChain(certificateChain);
                    }

                    return new Execution<>(resp);
                  } else if (selectPrivateKeyOperationResult.getStatus().equals(CoreOperationStatus.BACK)) {
                    continue;
                  } else if (selectPrivateKeyOperationResult.getStatus().equals(CoreOperationStatus.NO_KEY)) {
                    final Operation<Boolean> notFoundOperation = this.getOperationFactory().getOperation(UIOperation.class,
                        "/fxml/certificate-not-found.fxml", api, certificateToken);
                    final OperationResult<Boolean> notFoundOperationResult = notFoundOperation.perform();
                    if(notFoundOperationResult.getStatus().equals(BasicOperationStatus.USER_CANCEL) ||
                        !notFoundOperationResult.getResult()) {
                      // user will fix profile
                      return this.handleErrorOperationResult(selectPrivateKeyOperationResult);
                    } else {
                      // repeat the process with certificate forgotten to make user find it again
                      productAdapter.removeProduct(selectedProduct);
                      req.setCertificateLocation(null); // product is not available, let's ignore the given location
                      return process(api, req);
                    }
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
      } else {
        return this.handleErrorOperationResult(sessionValidityResult); // INVALID SESSION
      }
    } catch (final Exception e) {
      logger.error("Flow error", e);
      SessionManager.getManager().destroy();
      throw this.handleException(e);
    }
  }
}

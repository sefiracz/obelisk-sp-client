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
import cz.sefira.obelisk.api.flow.OperationResult;
import cz.sefira.obelisk.flow.operation.*;
import cz.sefira.obelisk.generic.SessionManager;
import cz.sefira.obelisk.view.core.UIDisplay;
import eu.europa.esig.dss.SignatureValue;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.flow.operation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

class SignatureFlow extends AbstractCoreFlow<SignatureRequest, SignatureResponse> {

	private static final Logger logger = LoggerFactory.getLogger(SignatureFlow.class.getName());

	public SignatureFlow(UIDisplay display, NexuAPI api) {
		super(display, api);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Execution<SignatureResponse> process(NexuAPI api, SignatureRequest req) throws Exception {
    SignatureTokenConnection token = null;
    try {
      // check session validity
      final OperationResult<Boolean> sessionValidityResult = this.getOperationFactory()
              .getOperation(CheckSessionValidityOperation.class, req.getSessionValue()).perform();
      if(sessionValidityResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {

        // check mandatory values
        if ((req.getToBeSigned() == null) || (req.getToBeSigned().getBytes() == null)) {
          throw new NexuException("ToBeSigned is null");
        }
        if ((req.getDigestAlgorithm() == null)) {
          throw new NexuException("Digest algorithm expected");
        }

        final OperationResult<Map<TokenOperationResultKey, Object>> getTokenOperationResult =
            getOperationFactory().getOperation(GetTokenOperation.class, api, req.getTokenId()).perform();
        if (getTokenOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
          final Map<TokenOperationResultKey, Object> map = getTokenOperationResult.getResult();
          final TokenId tokenId = (TokenId) map.get(TokenOperationResultKey.TOKEN_ID);

          final OperationResult<SignatureTokenConnection> getTokenConnectionOperationResult =
              getOperationFactory().getOperation(GetTokenConnectionOperation.class, api, tokenId).perform();
          if (getTokenConnectionOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
            token = getTokenConnectionOperationResult.getResult();
            logger.info("Token " + token);

            final OperationResult<DSSPrivateKeyEntry> selectPrivateKeyOperationResult =
                getOperationFactory().getOperation(
                    TokenPrivateKeyOperation.class, token, api, req.getKeyId()).perform();
            if (selectPrivateKeyOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
              final DSSPrivateKeyEntry key = selectPrivateKeyOperationResult.getResult();

              logger.info("Key " + key + " " + key.getCertificate().getCertificate().getSubjectDN() + " from " + key.getCertificate().getCertificate().getIssuerDN());
              final OperationResult<SignatureValue> signOperationResult = getOperationFactory().getOperation(
                  SignOperation.class, token, api, req.getToBeSigned(), req.getDigestAlgorithm(), key).perform();
              if(signOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
                final SignatureValue value = signOperationResult.getResult();
                logger.info("Signature performed " + value);
                return new Execution<SignatureResponse>(new SignatureResponse(value, key.getCertificate(), key.getCertificateChain()));
              } else {
                return handleErrorOperationResult(signOperationResult);
              }
            } else {
              // key error
              return handleErrorOperationResult(selectPrivateKeyOperationResult);
            }
          } else {
            // token connection error
            return handleErrorOperationResult(getTokenConnectionOperationResult);
          }
        } else {
          // token error
          return handleErrorOperationResult(getTokenOperationResult);
        }
      } else {
        // invalid session
        return this.handleErrorOperationResult(sessionValidityResult);
      }
		} catch (Exception e) {
			logger.error("Flow error", e);
      SessionManager.getManager().destroy();
			throw handleException(e);
		}
	}
}
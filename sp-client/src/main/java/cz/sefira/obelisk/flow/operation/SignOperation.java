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
package cz.sefira.obelisk.flow.operation;

import cz.sefira.obelisk.CancelledOperationException;
import cz.sefira.obelisk.api.ws.model.SignParameters;
import cz.sefira.obelisk.token.windows.WindowsSignatureTokenAdapter;
import cz.sefira.obelisk.util.DSSUtils;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.flow.BasicOperationStatus;
import cz.sefira.obelisk.api.flow.Operation;
import cz.sefira.obelisk.api.flow.OperationResult;
import cz.sefira.obelisk.flow.exceptions.AbstractTokenRuntimeException;
import cz.sefira.obelisk.view.BusyIndicator;
import cz.sefira.obelisk.dss.*;
import cz.sefira.obelisk.dss.token.DSSPrivateKeyEntry;
import cz.sefira.obelisk.dss.token.SignatureTokenConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SignatureException;

/**
 * This {@link Operation} allows to perform a signature.
 * 
 * Expected parameters:
 * <ol>
 * <li>{@link SignatureTokenConnection}</li>
 * <li>{@link PlatformAPI}</li>
 * <li>{@link SignParameters}</li>
 * <li>{@link DSSPrivateKeyEntry}</li>
 * </ol>
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public class SignOperation extends AbstractCompositeOperation<SignatureValue> {

	private static final Logger logger = LoggerFactory.getLogger(SignOperation.class.getName());

	 private SignatureTokenConnection token;
	 private PlatformAPI api;
	 private SignParameters signParams;
	 private DSSPrivateKeyEntry key;

	public SignOperation() {
		super();
	}

	@Override
	public void setParams(Object... params) {
		try {
			this.token = (SignatureTokenConnection) params[0];
			this.api = (PlatformAPI) params[1];
			this.signParams = (SignParameters) params[2];
			this.key = (DSSPrivateKeyEntry) params[3];
		} catch(final ArrayIndexOutOfBoundsException | ClassCastException e) {
			throw new IllegalArgumentException("Expected parameters: SignatureTokenConnection, PlatformAPI, SignParams, DSSPrivateKeyEntry");
		}
	}

	@Override
  @SuppressWarnings("unchecked")
	public OperationResult<SignatureValue> perform() {
		byte[] toBeSigned = signParams.getToBeSigned();
		DigestAlgorithm digestAlgorithm = signParams.getDigestAlgorithm();
		boolean rsaPss = signParams.isUseRsaPss();
		if (rsaPss && !EncryptionAlgorithm.RSA.equals(key.getEncryptionAlgorithm())) {
			rsaPss = false; // force RSA-PSS value to false for non-RSA keys
		}
		MaskGenerationFunction maskGenerationFunction = rsaPss ? MaskGenerationFunction.MGF1 : null;
		return sign(toBeSigned, digestAlgorithm, maskGenerationFunction, key);
	}

	private OperationResult<SignatureValue> sign(byte[] toBeSigned, DigestAlgorithm digestAlgorithm,
																							 MaskGenerationFunction maskGenerationFunction, DSSPrivateKeyEntry key) {
		// to prevent covering windows minidriver PIN input being covered by busy indicator
		boolean alwaysOnTop = !(token instanceof WindowsSignatureTokenAdapter);
		try (BusyIndicator busyIndicator = new BusyIndicator(true, alwaysOnTop)) {
			return new OperationResult<>(token.sign(toBeSigned, digestAlgorithm, maskGenerationFunction, key));
		} catch (AbstractTokenRuntimeException e) {
			this.operationFactory.getMessageDialog(api, e.getDialogMessage(), true);
			return new OperationResult<>(CoreOperationStatus.CANNOT_SELECT_KEY);
		} catch (final CancelledOperationException e) {
			return new OperationResult<>(BasicOperationStatus.USER_CANCEL);
		} catch (Exception e) {
			// handling of user cancellation and other MSCAPI errors (MSCAPI throws generic exceptions with
			// localized messages, and only since JDK17 those messages have error codes that can identify the exception)
			if(e.getCause() instanceof SignatureException) {
				String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
				/*
					Signing action was cancelled in some way
					SCARD_W_CANCELLED_BY_USER - 0x8010006E - error 2148532334 - the action was cancelled by the user.
					NTE_PERM - 0x80090010 - error 2148073488 - Access denied.
					SCARD_E_CANCELLED - 0x80100002 - error 2148532226 - The action was canceled by an SCardCancel request.
					SCARD_E_SYSTEM_CANCELLED - 0x80100012 - error 2148532242 - The action was canceled by the system, presumably to log off or shut down.
				*/
				if (message.contains("error 2148532226") || message.contains("error 2148532242") ||
						message.contains("error 2148532334") || message.contains("error 2148073488")) {
					logger.info("Cancel: "+e.getCause().getMessage());
					return new OperationResult<>(BasicOperationStatus.USER_CANCEL);
				}

				/*
					Bug/Error usually happens when MSCAPI key container name contains illegal (non-ASCII) characters (e.g. diacritics)
					and RSA-PSS signature algorithm is used
					NTE_BUFFER_TOO_SMALL - 0x80090028 - error 2148073512 - The buffer supplied to a function was too small.
				*/
				if (message.contains("error 2148073512") && maskGenerationFunction != null) {
					logger.error("NTE_BUFFER_TOO_SMALL - 0x80090028 - error 2148073512 - The buffer supplied to a function was too small.");
					logger.info("Retry signature with downgraded algorithm: "+SignatureAlgorithm.getAlgorithm(key.getEncryptionAlgorithm(), digestAlgorithm));
					return sign(toBeSigned, digestAlgorithm, null, key); // fallback without RSA-PSS (MGF1)
				}
			}
			if (!DSSUtils.checkWrongPasswordInput(e, operationFactory, api))
				throw e;
			return new OperationResult<>(CoreOperationStatus.CANNOT_SELECT_KEY);
		}
	}
}

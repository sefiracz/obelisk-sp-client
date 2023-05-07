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
		// to prevent covering windows minidriver PIN input being covered by busy indicator
		boolean alwaysOnTop = !(token instanceof WindowsSignatureTokenAdapter);
		try (BusyIndicator busyIndicator = new BusyIndicator(true, alwaysOnTop)) {
			if (!EncryptionAlgorithm.RSA.equals(key.getEncryptionAlgorithm())) {
				rsaPss = false; // force RSA-PSS value to false for non-RSA keys
			}
      return new OperationResult<>(token.sign(toBeSigned, digestAlgorithm, rsaPss ? MaskGenerationFunction.MGF1 : null, key));
    } catch (AbstractTokenRuntimeException e) {
      this.operationFactory.getMessageDialog(api, e.getDialogMessage(), true);
      return new OperationResult<>(CoreOperationStatus.CANNOT_SELECT_KEY);
    } catch (final CancelledOperationException e) {
      return new OperationResult<>(BasicOperationStatus.USER_CANCEL);
    } catch (Exception e) {
    	// workaround for uncertain user cancellation (MSCAPI throws ambiguous exception with localized messages)
			if(e.getCause() instanceof SignatureException) {
				String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
				/* best guess - check for all czech variants of word "zrušit" and "cancel" for generic/english */
				if (message.contains("zru") || message.contains("cancel")) {
					return new OperationResult<>(BasicOperationStatus.USER_CANCEL);
				}
			}
      if (!DSSUtils.checkWrongPasswordInput(e, operationFactory, api))
        throw e;
      return new OperationResult<>(CoreOperationStatus.CANNOT_SELECT_KEY);
    }
	}
}

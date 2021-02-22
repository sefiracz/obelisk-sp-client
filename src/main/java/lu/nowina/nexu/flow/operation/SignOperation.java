/**
 * © Nowina Solutions, 2015-2015
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
package lu.nowina.nexu.flow.operation;

import eu.europa.esig.dss.DigestAlgorithm;
import eu.europa.esig.dss.SignatureValue;
import eu.europa.esig.dss.ToBeSigned;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import lu.nowina.nexu.CancelledOperationException;
import lu.nowina.nexu.Utils;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.flow.BasicOperationStatus;
import lu.nowina.nexu.api.flow.Operation;
import lu.nowina.nexu.api.flow.OperationResult;
import lu.nowina.nexu.keystore.KeystoreNotFoundException;
import lu.nowina.nexu.keystore.UnsupportedKeystoreTypeException;
import lu.nowina.nexu.pkcs11.PKCS11RuntimeException;
import lu.nowina.nexu.view.core.UIOperation;

/**
 * This {@link Operation} allows to perform a signature.
 * 
 * Expected parameters:
 * <ol>
 * <li>{@link SignatureTokenConnection}</li>
 * <li>{@link ToBeSigned}</li>
 * <li>{@link DigestAlgorithm}</li>
 * <li>{@link DSSPrivateKeyEntry}</li>
 * </ol>
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public class SignOperation extends AbstractCompositeOperation<SignatureValue> {

	 private SignatureTokenConnection token;
	 private NexuAPI api;
	 private ToBeSigned toBeSigned;
	 private DigestAlgorithm digestAlgorithm;
	 private DSSPrivateKeyEntry key;

	public SignOperation() {
		super();
	}

	@Override
	public void setParams(Object... params) {
		try {
			this.token = (SignatureTokenConnection) params[0];
			this.api = (NexuAPI) params[1];
			this.toBeSigned = (ToBeSigned) params[2];
			this.digestAlgorithm = (DigestAlgorithm) params[3];
			this.key = (DSSPrivateKeyEntry) params[4];
		} catch(final ArrayIndexOutOfBoundsException | ClassCastException e) {
			throw new IllegalArgumentException("Expected parameters: SignatureTokenConnection, NexuApi, ToBeSigned, DigestAlgorithm, DSSPrivateKeyEntry");
		}
	}

	@Override
	public OperationResult<SignatureValue> perform() {
		try {
			try {
				return new OperationResult<SignatureValue>(token.sign(toBeSigned, digestAlgorithm, key));
			} catch (KeystoreNotFoundException e) {
				this.operationFactory.getOperation(UIOperation.class, "/fxml/message.fxml", new Object[] {
						"key.selection.keystore.not.found", api.getAppConfig().getApplicationName(), 370, 150, e.getMessage()
				}).perform();
				return new OperationResult<>(CoreOperationStatus.CANNOT_SELECT_KEY);
			} catch(PKCS11RuntimeException e) {
				this.operationFactory.getOperation(UIOperation.class, "/fxml/message.fxml", new Object[] {
						"key.selection.pkcs11.not.found", api.getAppConfig().getApplicationName(), 370, 150
				}).perform();
				return new OperationResult<>(CoreOperationStatus.CANNOT_SELECT_KEY);
			} catch (UnsupportedKeystoreTypeException e) {
				this.operationFactory.getOperation(UIOperation.class, "/fxml/message.fxml", new Object[] {
						"key.selection.keystore.unsupported.type", api.getAppConfig().getApplicationName(), 370, 150,
						e.getFilePath()
				}).perform();
				return new OperationResult<>(CoreOperationStatus.CANNOT_SELECT_KEY);
			} catch (Exception e) {
				if(Utils.checkWrongPasswordInput(e, operationFactory, api))
					throw e;
				return new OperationResult<>(CoreOperationStatus.CANNOT_SELECT_KEY);
			}
		} catch(final CancelledOperationException e) {
			return new OperationResult<SignatureValue>(BasicOperationStatus.USER_CANCEL);
		}
	}
}

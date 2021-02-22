package lu.nowina.nexu.flow.operation;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.flow.operation.TokenPrivateKeyOperation
 *
 * Created: 16.02.2021
 * Author: hlavnicka
 */

import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import lu.nowina.nexu.Utils;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.flow.OperationResult;
import lu.nowina.nexu.keystore.KeystoreNotFoundException;
import lu.nowina.nexu.keystore.UnsupportedKeystoreTypeException;
import lu.nowina.nexu.pkcs11.PKCS11RuntimeException;
import lu.nowina.nexu.view.core.UIOperation;

import java.util.List;

/**
 * Get private key from token using keyId
 */
public class TokenPrivateKeyOperation extends AbstractCompositeOperation<DSSPrivateKeyEntry> {

  private SignatureTokenConnection token;
  private NexuAPI api;
  private String keyId;

  public TokenPrivateKeyOperation() {
    super();
  }

  @Override
  public void setParams(final Object... params) {
    try {
      this.token = (SignatureTokenConnection) params[0];
      this.api = (NexuAPI) params[1];
      this.keyId = (String) params[2];
    } catch(final ClassCastException | ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Expected parameters: SignatureTokenConnection, NexuAPI, String (keyId)");
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public OperationResult<DSSPrivateKeyEntry> perform() {
    final List<DSSPrivateKeyEntry> keys;
    try {
      if (this.token != null) {
        keys = this.token.getKeys();
      }
      else {
        return new OperationResult<>(CoreOperationStatus.NO_TOKEN);
      }
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

    DSSPrivateKeyEntry key = null;
    if (this.keyId != null) {
      for (final DSSPrivateKeyEntry k : keys) {
        if (k.getCertificate().getDSSIdAsString().equals(this.keyId)) {
          key = k;
          break;
        }
      }
    }
    if (key == null) {
      return new OperationResult<>(CoreOperationStatus.CANNOT_SELECT_KEY);
    }
    return new OperationResult<>(key);
  }

}

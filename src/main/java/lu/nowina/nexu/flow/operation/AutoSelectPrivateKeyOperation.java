package lu.nowina.nexu.flow.operation;

/*
 * Copyright 2020 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.flow.operation.SelectCertificateOperation
 *
 * Created: 17.12.2020
 * Author: hlavnicka
 */

import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import lu.nowina.nexu.CancelledOperationException;
import lu.nowina.nexu.Utils;
import lu.nowina.nexu.api.*;
import lu.nowina.nexu.api.flow.BasicOperationStatus;
import lu.nowina.nexu.api.flow.OperationResult;
import lu.nowina.nexu.keystore.KeystoreNotFoundException;
import lu.nowina.nexu.pkcs11.PKCS11RuntimeException;
import lu.nowina.nexu.view.core.UIOperation;

import java.io.FileNotFoundException;

/**
 * description
 */
public class AutoSelectPrivateKeyOperation extends AbstractCompositeOperation<DSSPrivateKeyEntry> {


  private SignatureTokenConnection token;
  private NexuAPI api;
  private Product product;
  private ProductAdapter productAdapter;
  private String keyAlias;

  public AutoSelectPrivateKeyOperation() {
    super();
  }

  @Override
  public void setParams(Object... params) {
    try {
      this.token = (SignatureTokenConnection) params[0];
      this.api = (NexuAPI) params[1];
      if(params.length > 2) {
        this.product = (Product) params[2];
      }
      if(params.length > 3) {
        this.productAdapter = (ProductAdapter) params[3];
      }
      if(params.length > 4) {
        this.keyAlias = (String) params[4];
      }
    } catch(final ClassCastException | ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Expected parameters: SignatureTokenConnection, NexuAPI, Product (optional), " +
          "ProductAdapter (optional), KeyAlias (optional)");
    }
  }

  @Override
  public OperationResult<DSSPrivateKeyEntry> perform() {
    DSSPrivateKeyEntry key;
    try {
      if((this.productAdapter != null) && (this.product != null) && (this.keyAlias != null)) {
        key = this.productAdapter.getKey(this.token, this.keyAlias);
      } else {
        return new OperationResult<>(CoreOperationStatus.CANNOT_SELECT_KEY);
      }
    } catch(final CancelledOperationException e) {
      return new OperationResult<>(BasicOperationStatus.USER_CANCEL);
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
    } catch (Exception e) {
      if(Utils.checkWrongPasswordInput(e, operationFactory, api))
        throw e;
      return new OperationResult<>(CoreOperationStatus.CANNOT_SELECT_KEY);
    }
    if(key == null) {
       return new OperationResult<>(CoreOperationStatus.NO_KEY);
    }
    return new OperationResult<>(key);
  }
}

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

package cz.sefira.obelisk.flow.operation;

/*
 * Copyright 2020 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.flow.operation.SelectCertificateOperation
 *
 * Created: 17.12.2020
 * Author: hlavnicka
 */

import cz.sefira.obelisk.CancelledOperationException;
import cz.sefira.obelisk.Utils;
import cz.sefira.obelisk.api.NexuAPI;
import cz.sefira.obelisk.api.Product;
import cz.sefira.obelisk.api.ProductAdapter;
import cz.sefira.obelisk.api.flow.BasicOperationStatus;
import cz.sefira.obelisk.api.flow.OperationResult;
import cz.sefira.obelisk.view.BusyIndicator;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.flow.exceptions.AbstractTokenRuntimeException;

import java.security.cert.X509Certificate;

/**
 * description
 */
public class AutoSelectPrivateKeyOperation extends AbstractCompositeOperation<DSSPrivateKeyEntry> {


  private SignatureTokenConnection token;
  private NexuAPI api;
  private Product product;
  private ProductAdapter productAdapter;
  private String keyAlias;
  private X509Certificate certificate;

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
      if(params.length > 5) {
        this.certificate = (X509Certificate) params[5];
      }
    } catch(final ClassCastException | ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Expected parameters: SignatureTokenConnection, NexuAPI, Product (optional), " +
          "ProductAdapter (optional), KeyAlias (optional)");
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public OperationResult<DSSPrivateKeyEntry> perform() {
    DSSPrivateKeyEntry key;
    try (BusyIndicator busyIndicator = new BusyIndicator()){
      if((this.productAdapter != null) && (this.product != null) && (this.keyAlias != null) &&
          (this.certificate != null)) {
        key = this.productAdapter.getKey(this.token, this.keyAlias, this.certificate);
      } else {
        return new OperationResult<>(CoreOperationStatus.CANNOT_SELECT_KEY);
      }
    } catch(final CancelledOperationException e) {
      return new OperationResult<>(BasicOperationStatus.USER_CANCEL);
    } catch (AbstractTokenRuntimeException e) {
      this.operationFactory.getMessageDialog(api, e.getDialogMessage(), true);
      return new OperationResult<>(CoreOperationStatus.CANNOT_SELECT_KEY);
    } catch (Exception e) {
      if(!Utils.checkWrongPasswordInput(e, operationFactory, api))
        throw e;
      return new OperationResult<>(CoreOperationStatus.CANNOT_SELECT_KEY);
    }
    if(key == null) {
       return new OperationResult<>(CoreOperationStatus.NO_KEY);
    }
    return new OperationResult<>(key);
  }
}

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
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.flow.operation.TokenPrivateKeyOperation
 *
 * Created: 16.02.2021
 * Author: hlavnicka
 */

import cz.sefira.obelisk.CancelledOperationException;
import cz.sefira.obelisk.api.flow.BasicOperationStatus;
import cz.sefira.obelisk.util.DSSUtils;
import cz.sefira.obelisk.api.flow.OperationResult;
import cz.sefira.obelisk.flow.exceptions.AbstractTokenRuntimeException;
import cz.sefira.obelisk.view.BusyIndicator;
import cz.sefira.obelisk.dss.token.DSSPrivateKeyEntry;
import cz.sefira.obelisk.dss.token.SignatureTokenConnection;
import cz.sefira.obelisk.api.PlatformAPI;

import java.util.List;

/**
 * Get private key from token using keyId
 */
public class TokenPrivateKeyOperation extends AbstractCompositeOperation<DSSPrivateKeyEntry> {

  private SignatureTokenConnection token;
  private PlatformAPI api;
  private String keyId;

  public TokenPrivateKeyOperation() {
    super();
  }

  @Override
  public void setParams(final Object... params) {
    try {
      this.token = (SignatureTokenConnection) params[0];
      this.api = (PlatformAPI) params[1];
      this.keyId = (String) params[2];
    } catch(final ClassCastException | ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Expected parameters: SignatureTokenConnection, PlatformAPI, String (keyId)");
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public OperationResult<DSSPrivateKeyEntry> perform() {
    final List<DSSPrivateKeyEntry> keys;
    try (BusyIndicator busyIndicator = new BusyIndicator()){
      if (this.token != null) {
        keys = this.token.getKeys();
      }
      else {
        return new OperationResult<>(CoreOperationStatus.NO_TOKEN);
      }
    } catch (CancelledOperationException e) {
      return new OperationResult<>(BasicOperationStatus.USER_CANCEL);
    } catch (AbstractTokenRuntimeException e) {
      this.operationFactory.getMessageDialog(api, e.getDialogMessage(), true);
      return new OperationResult<>(CoreOperationStatus.CANNOT_SELECT_KEY);
    } catch (Exception e) {
      if(!DSSUtils.checkWrongPasswordInput(e, operationFactory, api))
        throw e;
      return new OperationResult<>(CoreOperationStatus.CANNOT_SELECT_KEY);
    }

    DSSPrivateKeyEntry key = null;
    if (this.keyId != null) {
      for (final DSSPrivateKeyEntry k : keys) {
        if (k.getCertificateToken().getDSSIdAsString().equals(this.keyId)) {
          key = k;
          break;
        }
      }
    }
    if (key == null) {
      return new OperationResult<>(CoreOperationStatus.NO_KEY);
    }
    return new OperationResult<>(key);
  }

}

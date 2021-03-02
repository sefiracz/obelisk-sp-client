/**
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Licensed under EUPL Version 1.1 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package lu.nowina.nexu.pkcs11;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.pkcs11.Pkcs11SignatureTokenAdapter
 *
 * Created: 03.02.2021
 * Author: hlavnicka
 */

import eu.europa.esig.dss.token.PasswordInputCallback;
import eu.europa.esig.dss.token.Pkcs11SignatureToken;

/**
 * Abstract PKCS11 token adapter
 */
public abstract class AbstractPkcs11SignatureTokenAdapter extends Pkcs11SignatureToken {

  public AbstractPkcs11SignatureTokenAdapter(String pkcs11Path) {
    super(pkcs11Path);
  }

  public AbstractPkcs11SignatureTokenAdapter(String pkcs11Path, PasswordInputCallback callback, int slotId) {
    super(pkcs11Path, callback, slotId);
  }

  public abstract String getPkcs11Library();
}

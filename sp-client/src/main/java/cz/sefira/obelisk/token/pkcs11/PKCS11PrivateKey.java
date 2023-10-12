/**
 * © SEFIRA spol. s r.o., 2020-2023
 * <p>
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 * <p>
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 * <p>
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package cz.sefira.obelisk.token.pkcs11;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.pkcs11.Pkcs11PrivateKey
 *
 * Created: 07.11.2021
 * Author: hlavnicka
 */

/**
 * PKCS11 private key attributes handler
 */
public class PKCS11PrivateKey {

  final private long signatureKeyHandle;
  private boolean alwaysAuthenticate;

  public PKCS11PrivateKey(long signatureKeyHandle, boolean alwaysAuthenticate) {
    this.signatureKeyHandle = signatureKeyHandle;
    this.alwaysAuthenticate = alwaysAuthenticate;
  }

  public long getSignatureKeyHandle() {
    return signatureKeyHandle;
  }

  public boolean isAlwaysAuthenticate() {
    return alwaysAuthenticate;
  }

  public void setAlwaysAuthenticate(boolean alwaysAuthenticate) {
    this.alwaysAuthenticate = alwaysAuthenticate;
  }

  @Override
  public String toString() {
    return "PKCS11PrivateKey{" +
        "signatureKeyHandle=" + signatureKeyHandle +
        ", alwaysAuthenticate=" + alwaysAuthenticate +
        '}';
  }
}

package lu.nowina.nexu.pkcs11;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.pkcs11.Pkcs11PrivateKey
 *
 * Created: 07.11.2021
 * Author: hlavnicka
 */

/**
 * PKCS11 private key attributes handler
 */
public class PKCS11PrivateKey {

  final private long signatureKeyHandle;
  final private boolean alwaysAuthenticate;

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

  @Override
  public String toString() {
    return "PKCS11PrivateKey{" +
        "signatureKeyHandle=" + signatureKeyHandle +
        ", alwaysAuthenticate=" + alwaysAuthenticate +
        '}';
  }
}

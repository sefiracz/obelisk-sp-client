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

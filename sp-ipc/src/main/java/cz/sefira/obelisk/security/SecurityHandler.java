package cz.sefira.obelisk.security;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.security.SecurityUtils
 *
 * Created: 01.02.2023
 * Author: hlavnicka
 */

import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;

/**
 * description
 */
public class SecurityHandler {

  private final Certificate certificate;

  public SecurityHandler() throws GeneralSecurityException, IOException {
    char[] password = "sefira".toCharArray(); // TODO - handle password
    KeyStore ks = KeyStore.getInstance("PKCS12");
    ks.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("sp-client-uri.pfx"), password);
    String alias = ks.aliases().nextElement();
    certificate = ks.getCertificate(alias);
  }

  public boolean verifySignature(byte[] signatureValue, byte[] signedData) throws GeneralSecurityException {
    Signature s = Signature.getInstance("SHA256withECDSA");
    s.initVerify(certificate.getPublicKey());
    s.update(signedData);
    return s.verify(signatureValue);
  }



}

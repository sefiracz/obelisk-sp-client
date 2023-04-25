package cz.sefira.obelisk.dss;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.dss.KeyUsageBit
 *
 * Created: 31.01.2023
 * Author: hlavnicka
 */

/**
 * description
 */
public enum KeyUsageBit {

 digitalSignature(0),

 nonRepudiation(1),

 keyEncipherment(2),

 dataEncipherment(3),

 keyAgreement(4),

 keyCertSign(5),

 crlSign(6),

 encipherOnly(7),

 decipherOnly(8);

 private final int index;

 /**
  * The default constructor for KeyUsageBit.
  */
 private KeyUsageBit(int index) {
  this.index = index;
 }

 public int getIndex() {
  return index;
 }

}
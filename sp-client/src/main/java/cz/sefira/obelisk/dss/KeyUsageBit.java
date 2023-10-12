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
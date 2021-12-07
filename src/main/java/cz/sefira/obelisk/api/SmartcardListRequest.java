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
package cz.sefira.obelisk.api;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.SmartcardListRequest
 *
 * Created: 08.02.2021
 * Author: hlavnicka
 */

import java.util.List;

public class SmartcardListRequest extends NexuRequest {

  private List<SmartcardInfo> smartcardInfos;
  private byte[] digest;

  public List<SmartcardInfo> getSmartcardInfos() {
    return smartcardInfos;
  }

  public void setSmartcardInfos(List<SmartcardInfo> smartcardInfos) {
    this.smartcardInfos = smartcardInfos;
  }

  public byte[] getDigest() {
    return digest;
  }

  public void setDigest(byte[] digest) {
    this.digest = digest;
  }
}
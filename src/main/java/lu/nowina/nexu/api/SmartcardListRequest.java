package lu.nowina.nexu.api;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.api.SmartcardListRequest
 *
 * Created: 08.02.2021
 * Author: hlavnicka
 */

import java.util.List;

public class SmartcardListRequest extends NexuRequest {

  private List<SmartcardInfo> smartcardInfos;

  public List<SmartcardInfo> getSmartcardInfos() {
    return smartcardInfos;
  }

  public void setSmartcardInfos(List<SmartcardInfo> smartcardInfos) {
    this.smartcardInfos = smartcardInfos;
  }

}

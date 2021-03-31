/**
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package lu.nowina.nexu.generic;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.generic.ConnectionInfoDatabase
 *
 * Created: 04.02.2021
 * Author: hlavnicka
 */

import lu.nowina.nexu.DatabaseEventHandler;

import lu.nowina.nexu.EntityDatabase;
import lu.nowina.nexu.api.SmartcardInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@XmlRootElement(name = "database")
@XmlAccessorType(XmlAccessType.FIELD)
public class SmartcardInfoDatabase implements EntityDatabase {

  private static final Logger log = LoggerFactory.getLogger(SmartcardInfoDatabase.class.getName());

  @XmlElement(name = "smartcardInfo")
  private List<SmartcardInfo> infos;

  @XmlTransient
  private DatabaseEventHandler onAddRemoveAction;

  @Override
  public void initialize() {
    log.info("Smartcard connections information loaded");
  }

  public final void setSmartcardInfos(List<SmartcardInfo> infos) {
    this.infos = infos;
    onAddRemove();
  }

  public List<SmartcardInfo> getInfos() {
    return Collections.unmodifiableList(getStoredInfo());
  }

  public Map<String, SmartcardInfo> getSmartcardInfosMap() {
    Map<String, SmartcardInfo> infosMap = new ConcurrentHashMap<>();
    for(SmartcardInfo smartcardInfo : getStoredInfo()) {
      infosMap.put(smartcardInfo.getAtr(), smartcardInfo);
    }
    return infosMap;
  }

  private void onAddRemove() {
    if(onAddRemoveAction != null) {
      onAddRemoveAction.execute(this);
    } else {
      log.warn("No DatabaseEventHandler define, the database cannot be stored");
    }
  }

  private List<SmartcardInfo> getStoredInfo() {
    if (infos == null) {
      this.infos = new ArrayList<>();
    }
    return infos;
  }

  @Override
  public void setOnAddRemoveAction(DatabaseEventHandler eventHandler) {
    this.onAddRemoveAction = eventHandler;
  }
}

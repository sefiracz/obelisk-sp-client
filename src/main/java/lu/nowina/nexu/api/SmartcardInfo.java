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
package lu.nowina.nexu.api;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.api.SmartcardInfo
 *
 * Created: 08.02.2021
 * Author: hlavnicka
 */

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Smartcard informations
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class SmartcardInfo {

  private String atr;

  private String description;

  private String modelName;

  private String downloadUrl;

  @XmlElement(name = "drivers")
  private List<String> drivers;

  public SmartcardInfo() {
  }

  public SmartcardInfo(String atr, String pkcs11Path) {
    this.atr = atr;
    this.drivers = new ArrayList<>();
    if(pkcs11Path != null)
      this.drivers.add(pkcs11Path);
  }

  public String getAtr() {
    return atr;
  }

  public void setAtr(String atr) {
    this.atr = atr;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  public String getDownloadUrl() {
    return downloadUrl;
  }

  public void setDownloadUrl(String downloadUrl) {
    this.downloadUrl = downloadUrl;
  }

  public List<String> getDrivers() {
    return drivers;
  }

  public void setDrivers(List<String> drivers) {
    this.drivers = drivers;
  }

}

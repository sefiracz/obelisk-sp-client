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

  public String getAtr() {
    return atr;
  }

  public void setAtr(String atr) {
    this.atr = atr;
  }

  public SmartcardInfo(String description) {
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

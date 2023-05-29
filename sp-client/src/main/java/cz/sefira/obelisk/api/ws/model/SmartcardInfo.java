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
package cz.sefira.obelisk.api.ws.model;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.SmartcardInfo
 *
 * Created: 08.02.2021
 * Author: hlavnicka
 */

import cz.sefira.obelisk.util.annotation.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SmartcardInfo {

  private String atr;

  private String description;

  private String modelName;

  private String downloadUrl;

  private List<String> drivers;

  private SmartcardInfo() {}

  public SmartcardInfo(@NotNull String atr, @NotNull String pkcs11Path) {
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

  public boolean compare(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SmartcardInfo that = (SmartcardInfo) o;

    if (getAtr() != null ? !getAtr().equals(that.getAtr()) : that.getAtr() != null) return false;
    if (getDescription() != null ? !getDescription().equals(that.getDescription()) : that.getDescription() != null)
      return false;
    if (getModelName() != null ? !getModelName().equals(that.getModelName()) : that.getModelName() != null)
      return false;
    if (getDownloadUrl() != null ? !getDownloadUrl().equals(that.getDownloadUrl()) : that.getDownloadUrl() != null)
      return false;
    return getDrivers() != null ? getDrivers().equals(that.getDrivers()) : that.getDrivers() == null;
  }

    @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SmartcardInfo that = (SmartcardInfo) o;

    return getAtr() != null ? getAtr().equals(that.getAtr()) : that.getAtr() == null;
  }

  @Override
  public int hashCode() {
    return getAtr() != null ? getAtr().hashCode() : 0;
  }
}

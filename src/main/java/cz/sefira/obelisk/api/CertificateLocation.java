/**
 * © SEFIRA spol. s r.o., 2020-2022
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
package cz.sefira.obelisk.api;

/*
 * Copyright 2022 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.CertificateLocation
 *
 * Created: 06.06.2022
 * Author: hlavnicka
 */

/**
 * Assumed certificate device/storage location
 */
public class CertificateLocation {

  private KeystoreType type;
  private String alias;
  private String param;
  private TokenInfo tokenInfo;

  public KeystoreType getType() {
    return type;
  }

  public void setType(KeystoreType type) {
    this.type = type;
  }

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }

  public String getParam() {
    return param;
  }

  public void setParam(String param) {
    this.param = param;
  }

  public TokenInfo getTokenInfo() {
    return tokenInfo;
  }

  public void setTokenInfo(TokenInfo tokenInfo) {
    this.tokenInfo = tokenInfo;
  }

  public class TokenInfo {

    private String atr;
    private String tokenLabel;
    private String tokenSerial;
    private String tokenManufacturer;

    public String getAtr() {
      return atr;
    }

    public void setAtr(String atr) {
      this.atr = atr;
    }

    public String getTokenLabel() {
      return tokenLabel;
    }

    public void setTokenLabel(String tokenLabel) {
      this.tokenLabel = tokenLabel;
    }

    public String getTokenSerial() {
      return tokenSerial;
    }

    public void setTokenSerial(String tokenSerial) {
      this.tokenSerial = tokenSerial;
    }

    public String getTokenManufacturer() {
      return tokenManufacturer;
    }

    public void setTokenManufacturer(String tokenManufacturer) {
      this.tokenManufacturer = tokenManufacturer;
    }
  }
}

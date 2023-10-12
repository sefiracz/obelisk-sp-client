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
package cz.sefira.obelisk.api.ws.auth;

import com.google.gson.annotations.SerializedName;

public class JwtToken {

  @SerializedName("exp")
  private Long expiration;
  @SerializedName("iat")
  private Long issuedAt;
  private String tokenData;
  private String redirectUri;
  @SerializedName("iss")
  private String issuer;
  @SerializedName("azp")
  private String authorizedParty;
  @SerializedName("nbf")
  private Long notBefore;
  private String nonce;

  public Long getExpiration() {
    return expiration;
  }

  public void setExpiration(Long expiration) {
    this.expiration = expiration;
  }

  public Long getIssuedAt() {
    return issuedAt;
  }

  public void setIssuedAt(Long issuedAt) {
    this.issuedAt = issuedAt;
  }

  public String getTokenData() {
    return tokenData;
  }

  public void setTokenData(String tokenData) {
    this.tokenData = tokenData;
  }

  public String getRedirectUri() {
    return redirectUri;
  }

  public void setRedirectUri(String redirectUri) {
    this.redirectUri = redirectUri;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public String getAuthorizedParty() {
    return authorizedParty;
  }

  public void setAuthorizedParty(String authorizedParty) {
    this.authorizedParty = authorizedParty;
  }

  public Long getNotBefore() {
    return notBefore;
  }

  public void setNotBefore(Long notBefore) {
    this.notBefore = notBefore;
  }

  public String getNonce() {
    return nonce;
  }

  public void setNonce(String nonce) {
    this.nonce = nonce;
  }
}

package cz.sefira.obelisk.api.ws.auth;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.ws.auth.BearerToken
 *
 * Created: 09.03.2023
 * Author: hlavnicka
 */

import com.google.gson.annotations.SerializedName;

/**
 * BearerToken POJO
 */
public class BearerToken {

  @SerializedName("access_token")
  private String accessToken;

  @SerializedName("expires_in")
  private long expiresIn;

  @SerializedName("refresh_expires_in")
  private long refreshExpiresIn;

  @SerializedName("refresh_token")
  private String refreshToken;

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public long getExpiresIn() {
    return expiresIn;
  }

  public void setExpiresIn(long expiresIn) {
    this.expiresIn = expiresIn;
  }

  public long getRefreshExpiresIn() {
    return refreshExpiresIn;
  }

  public void setRefreshExpiresIn(long refreshExpiresIn) {
    this.refreshExpiresIn = refreshExpiresIn;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

}

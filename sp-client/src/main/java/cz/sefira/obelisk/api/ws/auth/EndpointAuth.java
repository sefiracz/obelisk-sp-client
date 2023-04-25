package cz.sefira.obelisk.api.ws.auth;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.ws.auth.EndpointAuth
 *
 * Created: 30.03.2023
 * Author: hlavnicka
 */

/**
 * Authentication value for given URL endpoint
 */
public class EndpointAuth {

  /**
   * URL endpoint for which this authentication is designated
   */
  private final String url;

  /**
   * Full Authentication header value
   */
  private final String authentication;

  public EndpointAuth(String url, String authentication) {
    this.url = url;
    this.authentication = authentication;
  }

  public String getUrl() {
    return url;
  }

  public String getAuthentication() {
    return authentication;
  }

}

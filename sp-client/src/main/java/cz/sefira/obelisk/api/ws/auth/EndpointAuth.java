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

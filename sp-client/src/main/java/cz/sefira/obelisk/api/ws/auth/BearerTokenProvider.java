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
 * cz.sefira.obelisk.api.ws.auth.BearerTokenHandler
 *
 * Created: 09.03.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.ws.ssl.HttpResponse;
import cz.sefira.obelisk.api.ws.ssl.HttpsClient;
import cz.sefira.obelisk.api.ws.ssl.SSLCommunicationException;
import cz.sefira.obelisk.json.GsonHelper;
import cz.sefira.obelisk.util.HttpUtils;
import cz.sefira.obelisk.util.JwtTokenUtils;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Bearer token provider
 */
public class BearerTokenProvider implements AuthenticationProvider {

  private static final Logger logger = LoggerFactory.getLogger(BearerTokenProvider.class.getName());

  private static final String CLIENT_ID = "obelisk-sp-client";
  private static final String AUTH_TYPE = "Bearer ";

  private final String magicLink;
  private final PlatformAPI api;

  private HttpsClient client;
  private String authServerUrl = null;
  private transient String redirectUri = null;
  private transient String code = null;
  private transient String sessionState = null;
  private transient BearerToken currentToken = null;

  public BearerTokenProvider(String magicLink, PlatformAPI api)
      throws AuthenticationProviderException, SSLCommunicationException {
    this.magicLink = magicLink;
    this.api = api;
    this.currentToken = initToken();
  }

  public String getRedirectUri() {
    return redirectUri;
  }

  public String getEndpointAuthentication() throws AuthenticationProviderException, SSLCommunicationException {
    if (currentToken == null) {
      currentToken = initToken();
      return AUTH_TYPE + currentToken.getAccessToken();
    }
    else if (!JwtTokenUtils.isExpired(currentToken.getAccessToken())) {
      return AUTH_TYPE + currentToken.getAccessToken();
    }
    else if (currentToken.getRefreshToken() != null && JwtTokenUtils.isExpired(currentToken.getAccessToken())
        && !JwtTokenUtils.isExpired(currentToken.getRefreshToken())) {
      currentToken = refreshToken();
      return AUTH_TYPE + currentToken.getAccessToken();
    }
    else {
      throw new AuthExpirationException("Authentication credentials expired and cannot be refreshed");
    }
  }

  private BearerToken initToken() throws AuthenticationProviderException, SSLCommunicationException {
    client = new HttpsClient(api);
    parseAuthServerURL();
    // MAGIC LINK
    logger.info("Calling magic link");
    actionToken();
    // GET BEARER TOKEN
    List<NameValuePair> params = new ArrayList<>();
    params.add(new BasicNameValuePair("client_id", CLIENT_ID));
    params.add(new BasicNameValuePair("redirect_uri", redirectUri));
    params.add(new BasicNameValuePair("grant_type", "authorization_code"));
    params.add(new BasicNameValuePair("code", code));
    params.add(new BasicNameValuePair("session_state", sessionState));
    logger.info("Init bearer token");
    return currentToken = token(params);
  }

  private BearerToken refreshToken() throws AuthenticationProviderException, SSLCommunicationException {
    try {
      // REFRESH BEARER TOKEN
      List<NameValuePair> params = new ArrayList<>();
      params.add(new BasicNameValuePair("client_id", CLIENT_ID));
      params.add(new BasicNameValuePair("grant_type", "refresh_token"));
      params.add(new BasicNameValuePair("refresh_token", currentToken.getRefreshToken()));
      params.add(new BasicNameValuePair("code", code));
      params.add(new BasicNameValuePair("session_state", sessionState));
      logger.info("Refreshing bearer token");
      return currentToken = token(params);
    } catch (SSLCommunicationException e) {
      throw e;
    } catch (Exception e) {
      throw new AuthenticationProviderException(e);
    }
  }

  private void actionToken() throws AuthenticationProviderException, SSLCommunicationException {
    try {
      URIBuilder uriBuilder = new URIBuilder(magicLink);
      HttpUriRequestBase request = new HttpUriRequestBase("GET", uriBuilder.build());
      HttpClientBuilder clientBuilder = HttpClientBuilder.create().disableRedirectHandling();
      HttpResponse response = client.execute(request, clientBuilder);
      int responseCode = response.getCode();
      if (responseCode == HttpStatus.SC_MOVED_TEMPORARILY) {
        String location = HttpUtils.getLocationURI(response);
        if (location != null) {
          URIBuilder builder = new URIBuilder(location);
          List<NameValuePair> queryParams = builder.getQueryParams();
          for (NameValuePair pair : queryParams) {
            if ("code".equals(pair.getName())) {
              code = pair.getValue();
              builder.removeParameter("code");
            }
            if ("session_state".equals(pair.getName())) {
              sessionState = pair.getValue();
              builder.removeParameter("session_state");
            }
          }
          redirectUri = builder.build().toString();
        }
      }
      if (redirectUri == null) {
        throw new IllegalStateException("Magic-link did not provide redirect URI.");
      }
    } catch (SSLCommunicationException e) {
      throw e;
    } catch (Exception e) {
      throw new AuthenticationProviderException(e);
    }
  }

  private BearerToken token(List<NameValuePair> params)
      throws AuthenticationProviderException, SSLCommunicationException {
    try {
      URIBuilder uriBuilder = new URIBuilder(authServerUrl);
      uriBuilder.appendPath(AppConfig.get().getTokenEndpoint()); // /protocol/openid-connect/token
      HttpUriRequestBase request = new HttpUriRequestBase("POST", uriBuilder.build());
      request.setEntity(new UrlEncodedFormEntity(params));
      HttpClientBuilder clientBuilder = HttpClientBuilder.create().disableRedirectHandling();
      HttpResponse response = client.execute(request, clientBuilder);
      return GsonHelper.fromJson(new String(response.getContent(), StandardCharsets.UTF_8), BearerToken.class);
    } catch (SSLCommunicationException e) {
      throw e;
    } catch (Exception e) {
      throw new AuthenticationProviderException(e);
    }
  }

  private void parseAuthServerURL() {
    int opIdx = magicLink.indexOf(AppConfig.get().getActionTokenEndpoint()); // /login-actions/action-token
    if (opIdx != -1) {
      authServerUrl = magicLink.substring(0, opIdx);
    } else {
      throw new IllegalStateException("Magic-link not in expected format.");
    }
  }

}

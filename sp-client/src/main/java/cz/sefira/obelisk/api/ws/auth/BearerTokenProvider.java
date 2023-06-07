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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
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
          throws GeneralSecurityException, URISyntaxException, IOException {
    this.magicLink = magicLink;
    this.api = api;
    this.currentToken = initToken();
  }

  public String getRedirectUri() {
    return redirectUri;
  }

  public String getEndpointAuthentication() throws GeneralSecurityException, URISyntaxException, IOException {
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

  private BearerToken initToken() throws GeneralSecurityException, URISyntaxException, IOException {
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

  private BearerToken refreshToken() throws GeneralSecurityException, URISyntaxException, IOException {
    // REFRESH BEARER TOKEN
    List<NameValuePair> params = new ArrayList<>();
    params.add(new BasicNameValuePair("client_id", CLIENT_ID));
    params.add(new BasicNameValuePair("grant_type", "refresh_token"));
    params.add(new BasicNameValuePair("refresh_token", currentToken.getRefreshToken()));
    params.add(new BasicNameValuePair("code", code));
    params.add(new BasicNameValuePair("session_state", sessionState));
    logger.info("Refreshing bearer token");
    return currentToken = token(params);
  }

  private void actionToken() throws URISyntaxException, GeneralSecurityException, IOException {
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
  }

  private BearerToken token(List<NameValuePair> params) throws URISyntaxException, GeneralSecurityException, IOException {
    URIBuilder uriBuilder = new URIBuilder(authServerUrl);
    uriBuilder.appendPath(AppConfig.get().getTokenEndpoint()); // /protocol/openid-connect/token
    HttpUriRequestBase request = new HttpUriRequestBase("POST", uriBuilder.build());
    request.setEntity(new UrlEncodedFormEntity(params));
    HttpClientBuilder clientBuilder = HttpClientBuilder.create().disableRedirectHandling();
    HttpResponse response = client.execute(request, clientBuilder);
    return GsonHelper.fromJson(new String(response.getContent(), StandardCharsets.UTF_8), BearerToken.class);
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

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
package cz.sefira.obelisk.api.ws;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.ws.WebserviceClient
 *
 * Created: 07.02.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.model.Platform;
import cz.sefira.obelisk.api.ws.auth.AuthenticationProvider;
import cz.sefira.obelisk.api.ws.auth.AuthenticationProviderException;
import cz.sefira.obelisk.api.ws.ssl.HttpResponse;
import cz.sefira.obelisk.api.ws.ssl.HttpsClient;
import cz.sefira.obelisk.json.GsonHelper;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.*;
import java.util.concurrent.TimeUnit;

/**
 * description
 */
public class SpApiClient {

  private static final Logger logger = LoggerFactory.getLogger(SpApiClient.class.getName());

  private final HttpsClient client;

  public SpApiClient(PlatformAPI api) {
    this.client = new HttpsClient(api);
  }

  public HttpResponse call(String method, String url, AuthenticationProvider authProvider, Object payload, boolean sync)
          throws AuthenticationProviderException, URISyntaxException, GeneralSecurityException, IOException {
    // URI builder
    URIBuilder uriBuilder = new URIBuilder(url);
    uriBuilder.addParameter(new BasicNameValuePair("version", AppConfig.get().getApplicationVersion()));
    uriBuilder.addParameter(new BasicNameValuePair("platform", Platform.get()));
    uriBuilder.addParameter(new BasicNameValuePair("devices", String.valueOf(sync)));
    // execute request
    URI requestUri = uriBuilder.build();
    logger.info(method+" "+requestUri);
    HttpUriRequestBase request = new HttpUriRequestBase(method, requestUri);
    request.addHeader(HttpHeaders.AUTHORIZATION, authProvider.getEndpointAuthentication());
    HttpClientBuilder clientBuilder = HttpClientBuilder.create().disableRedirectHandling();
    clientBuilder.setDefaultRequestConfig(RequestConfig.custom()
        .setConnectionRequestTimeout(5, TimeUnit.SECONDS)
        .build());
    if (payload != null) {
      HttpEntity entity = new StringEntity(GsonHelper.toJson(payload), ContentType.APPLICATION_JSON);
      request.setEntity(entity);
    }
    return client.execute(request, clientBuilder);
  }

}

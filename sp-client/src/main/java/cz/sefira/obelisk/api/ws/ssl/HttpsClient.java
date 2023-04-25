package cz.sefira.obelisk.api.ws.ssl;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.ws.RestClient
 *
 * Created: 01.02.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.ws.auth.CommunicationExpirationException;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

import static org.apache.hc.core5.http.HttpStatus.*;

/**
 * SSL enabled http(s) client
 */
public class HttpsClient {

  private final PlatformAPI api;

  public HttpsClient(PlatformAPI api) {
    this.api = api;
  }

  public HttpResponse execute(ClassicHttpRequest request, HttpClientBuilder clientBuilder)
      throws GeneralSecurityException, IOException, URISyntaxException {
    if (api.getSslCertificateProvider() != null) {
      ConnectionConfig.Builder config = ConnectionConfig.custom()
          .setConnectTimeout(5, TimeUnit.SECONDS)
          .setSocketTimeout(30, TimeUnit.SECONDS);
      BasicHttpClientConnectionManager connectionManager =
          new BasicHttpClientConnectionManager(api.getSslCertificateProvider().getSocketFactory());
      connectionManager.setConnectionConfig(config.build());
      clientBuilder.setConnectionManager(connectionManager);
    }
    try (CloseableHttpClient httpClient = clientBuilder.build()) {
      HttpClientContext context = HttpClientContext.create();
      return httpClient.execute(request, context, response -> {
        // process response
        int responseCode = response.getCode();
        if (responseCode == SC_OK || responseCode == SC_ACCEPTED || responseCode == SC_NO_CONTENT ||
            responseCode == SC_MOVED_TEMPORARILY || responseCode == SC_SEE_OTHER) {
          final HttpEntity entity = response.getEntity();
          byte[] content = null;
          if (entity != null) {
            content = EntityUtils.toByteArray(entity);
          }
          return new HttpResponse(responseCode, response.getReasonPhrase(), response.getHeaders(), content);
        }
        else {
          throw new HttpResponseException(responseCode, response.getReasonPhrase());
        }
      });
    } catch (SSLException e) {
      throw new SSLCommunicationException(e, request.getUri().getHost(), api.getSslCertificateProvider().getCertificateChain());
    } catch (SocketTimeoutException e) {
      throw new CommunicationExpirationException(e.getMessage(), e);
    }
  }

}

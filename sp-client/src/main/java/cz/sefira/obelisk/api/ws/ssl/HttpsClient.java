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
import cz.sefira.obelisk.api.ws.HttpResponseException;
import cz.sefira.obelisk.api.ws.auth.CommunicationExpirationException;
import cz.sefira.obelisk.util.DSSUtils;
import cz.sefira.obelisk.util.X509Utils;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.apache.hc.core5.http.HttpStatus.*;

/**
 * SSL enabled http(s) client
 */
public class HttpsClient {

  private static final Logger logger = LoggerFactory.getLogger(HttpsClient.class.getName());

  private final PlatformAPI api;

  public HttpsClient(PlatformAPI api) {
    this.api = api;
  }

  public HttpResponse execute(ClassicHttpRequest request, HttpClientBuilder clientBuilder)
      throws GeneralSecurityException, IOException, URISyntaxException {
    return execute(request, clientBuilder, true);
  }

  private HttpResponse execute(ClassicHttpRequest request, HttpClientBuilder clientBuilder, boolean allowAIA)
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
        final HttpEntity entity = response.getEntity();
        byte[] content = null;
        if (entity != null) {
          content = EntityUtils.toByteArray(entity);
        }
        if (responseCode == SC_OK || responseCode == SC_ACCEPTED || responseCode == SC_NO_CONTENT ||
            responseCode == SC_MOVED_TEMPORARILY || responseCode == SC_SEE_OTHER) {
          return new HttpResponse(responseCode, response.getReasonPhrase(), response.getHeaders(), content);
        }
        else {
          throw new HttpResponseException(responseCode, response.getReasonPhrase(), response.getHeaders(), content);
        }
      });
    } catch (SSLException e) {
      List<X509Certificate> sslChain = api.getSslCertificateProvider().getCertificateChain();
      // if AIA is allowed
      if (allowAIA && processSSLException(e, sslChain)) {
        // we found trusted chain
        // put all but end-certificate to SSL cache and add to runtime trust
        List<X509Certificate> subChain = new ArrayList<>(sslChain.size()-1);
        for (int i=1; i<sslChain.size(); i++) {
          subChain.add(sslChain.get(i));
        }
        // add to cache and trusted store
        api.getSslCertificateProvider().addTrustedChain(subChain, true);
        // try again with new completed trust chain
        return execute(request, clientBuilder, false);
      } else {
        throw new SSLCommunicationException(e, request.getUri().getHost(), sslChain);
      }
    } catch (SocketTimeoutException e) {
      throw new CommunicationExpirationException(e.getMessage(), e);
    }
  }

  private boolean processSSLException(SSLException e, List<X509Certificate> sslChain) {
    String exceptionMsg = e.getMessage();
    exceptionMsg = exceptionMsg != null ? exceptionMsg.toLowerCase() : "";
    if (exceptionMsg.contains("unable to find valid certification path to requested target") &&
        sslChain != null && !sslChain.isEmpty()) {
      api.getSslCertificateProvider().refreshRoot();
      return completeCertificateChain(sslChain.get(sslChain.size()-1), sslChain);
    }
    return false;
  }


  public boolean completeCertificateChain(X509Certificate subject, List<X509Certificate> certificates) {
    if (X509Utils.isSelfSigned(subject)) {
      return false; // chain ends with untrusted self-sign, nothing to do here
    }
    List<String> urls = DSSUtils.getAccessLocations(subject);
    if (urls == null)
      return false; // no AIA URLs
    for (String url : urls) {
      if (!url.toLowerCase().startsWith("http")) {
        continue; // not HTTP URL
      }
      try {
        logger.info("Accessing AIA URL: "+url);
        ClassicHttpRequest request = new HttpUriRequestBase("GET", new URIBuilder(url).build());
        HttpResponse response = execute(request, HttpClientBuilder.create(), false);
        if (response == null || response.getContent() == null)
          continue; // no certificate downloaded
        X509Certificate issuer = X509Utils.getCertificateFromBytes(response.getContent());
        certificates.add(issuer);
        // did we find certificate issued by trust-anchor?
        List<X509Certificate> anchors = api.getSslCertificateProvider().getBySubject(issuer.getIssuerX500Principal());
        if (anchors != null) {
          boolean trustedChain = X509Utils.validateCertificateChain(certificates); // validate certificate chain
          for (X509Certificate anchor : anchors) {
            // find trusted-anchor that signs the chain
            if (trustedChain && X509Utils.validateCertificateIssuer(issuer, anchor)) {
              logger.info("Found trusted certificate chain");
              return true;
            }
          }
        }
        // is there a point to continue?
        if (!X509Utils.isSelfSigned(issuer)) {
          return completeCertificateChain(issuer, certificates); // still not at root or anchor
        } else {
          return false; // self-signed and not amongst trust-anchors
        }
      } catch (HttpResponseException e) {
        logger.error("Unable to download AIA certificate: "+e.getStatusCode()+" "+e.getReasonPhrase());
      } catch (Exception e) {
        logger.error("Unable to download AIA certificate: "+e.getMessage(), e);
      }
    }
    return false;
  }

}

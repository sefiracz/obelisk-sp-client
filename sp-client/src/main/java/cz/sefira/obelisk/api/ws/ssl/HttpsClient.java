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
import cz.sefira.obelisk.api.notification.LongActivityNotifier;
import cz.sefira.obelisk.util.DSSUtils;
import cz.sefira.obelisk.util.X509Utils;
import cz.sefira.obelisk.view.BusyIndicator;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.RequestFailedException;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Timeout;
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
import java.util.Timer;
import java.util.TimerTask;

import static org.apache.hc.core5.http.HttpStatus.*;

/**
 * SSL enabled http(s) client
 */
public class HttpsClient {

  private static final Logger logger = LoggerFactory.getLogger(HttpsClient.class.getName());

  private static final Timeout LONG_ACTIVITY = Timeout.ofSeconds(3);
  private static final Timeout CONNECT_TIMEOUT = Timeout.ofSeconds(10);
  private static final Timeout SOCKET_TIMEOUT = Timeout.ofSeconds(30);
  private static final Timeout HARD_TIMEOUT = Timeout.ofMinutes(2);

  private final PlatformAPI api;

  public HttpsClient(PlatformAPI api) {
    this.api = api;
  }

  /**
   * Execute HTTP request
   * @param request HTTP request
   * @param clientBuilder Request parameters
   * @return Http response
   * @throws GeneralSecurityException
   * @throws IOException
   */
  public HttpResponse execute(HttpUriRequestBase request, HttpClientBuilder clientBuilder)
      throws GeneralSecurityException, IOException, URISyntaxException {
    return execute(request, clientBuilder, true, true);
  }

  /**
   * Execute HTTP request
   * @param request HTTP request
   * @param clientBuilder Request parameters
   * @param allowAIA Allow using AIA to try and complete SSL certificate chain till trusted anchors
   * @param reloadSSL Reload SSL certificate stores to check if new trusted anchors didn't appear
   * @return Http response
   */
  private HttpResponse execute(HttpUriRequestBase request, HttpClientBuilder clientBuilder, boolean allowAIA,
                               boolean reloadSSL) throws GeneralSecurityException, IOException, URISyntaxException {

    BasicHttpClientConnectionManager connectionManager;
    if (api.getSslCertificateProvider() != null) {
      connectionManager = new BasicHttpClientConnectionManager(api.getSslCertificateProvider().getSocketFactory());
    } else {
      connectionManager = new BasicHttpClientConnectionManager();
    }
    connectionManager.setSocketConfig(SocketConfig.custom()
        .setSoTimeout(SOCKET_TIMEOUT)
        .build());
    connectionManager.setConnectionConfig(ConnectionConfig.custom()
        .setConnectTimeout(CONNECT_TIMEOUT)
        .setSocketTimeout(SOCKET_TIMEOUT)
        .build());
    clientBuilder.setConnectionManager(connectionManager);
    setHardTimeout(request);
    try (BusyIndicator busyIndicator = new BusyIndicator(true, false);
         LongActivityNotifier notifier = new LongActivityNotifier(api, "notification.long.activity.server", LONG_ACTIVITY.toMilliseconds());
         CloseableHttpClient httpClient = clientBuilder.build()) {
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
        } else {
          throw new HttpResponseException(responseCode, response.getReasonPhrase(), response.getHeaders(), content);
        }
      });
    } catch (SSLException e) {
      SSLCertificateProvider provider = api.getSslCertificateProvider();
      List<X509Certificate> sslChain = provider.getCertificateChain();
      if (reloadSSL && sslTrustIssue(e)) {
        logger.info("Reload system SSL certificates");
        X509Utils.loadSSLCertificates(provider.getTrustStore(), provider); // refresh trusted SSL certificates
        return execute(request, clientBuilder, true, false);
      }
      // if AIA is allowed
      if (allowAIA && processSSLException(e, sslChain)) {
        // we found trusted chain
        // put all but end-certificate to SSL cache and add to runtime trust
        List<X509Certificate> subChain = new ArrayList<>(sslChain.size() - 1);
        for (int i = 1; i < sslChain.size(); i++) {
          subChain.add(sslChain.get(i));
        }
        // add to cache and trusted store
        provider.addTrustedChain(subChain, true);
        // try again with new completed trust chain
        return execute(request, clientBuilder, false, false);
      } else {
        throw new SSLCommunicationException(e, request.getUri().getHost(), sslChain);
      }
    } catch (SocketTimeoutException | RequestFailedException e) {
      throw new CommunicationExpirationException("Connection expired: "+e.getMessage(), e);
    }
  }

  private void setHardTimeout(HttpUriRequestBase request) {
    TimerTask task = new TimerTask() {
      @Override
      public void run() {
        request.abort();
      }
    };
    new Timer(true).schedule(task, HARD_TIMEOUT.toMilliseconds());
  }

  private boolean processSSLException(SSLException e, List<X509Certificate> sslChain) {
    if (sslTrustIssue(e) && sslChain != null && !sslChain.isEmpty()) {
      return completeCertificateChain(sslChain.get(sslChain.size()-1), sslChain);
    }
    return false;
  }

  private boolean completeCertificateChain(X509Certificate subject, List<X509Certificate> certificates) {
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
        HttpUriRequestBase request = new HttpUriRequestBase("GET", new URIBuilder(url).build());
        HttpResponse response = execute(request, HttpClientBuilder.create(), false, false);
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

  private boolean sslTrustIssue(SSLException e) {
    String exceptionMsg = e.getMessage();
    exceptionMsg = exceptionMsg != null ? exceptionMsg.toLowerCase() : "";
    return  exceptionMsg.contains("unable to find valid certification path to requested target");
  }

}

package cz.sefira.obelisk.util;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.util.HttpUtils
 *
 * Created: 03.04.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.dispatcher.Dispatcher;
import cz.sefira.obelisk.api.ws.HttpResponseException;
import cz.sefira.obelisk.api.ws.model.Problem;
import cz.sefira.obelisk.api.ws.ssl.HttpResponse;
import cz.sefira.obelisk.json.GsonHelper;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.apache.hc.core5.http.ContentType.APPLICATION_JSON;

/**
 * HTTP helper utility methods
 */
public class HttpUtils {

  private static final Logger logger = LoggerFactory.getLogger(Dispatcher.class.getName());

  public static String getLocationURI(HttpResponse response) {
    for (Header h : response.getHeaders()) {
      if (HttpHeaders.LOCATION.equals(h.getName())) {
        return h.getValue();
      }
    }
    return null;
  }

  public static boolean isExpectedContentType(Header[] headers, ContentType contentType) {
    if (headers != null) {
      for (Header h : headers) {
        if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(h.getName())) {
          return contentType.isSameMimeType(ContentType.create(h.getValue()));
        }
      }
    }
    return false;
  }

  public static Problem processProblem(HttpResponseException response) {
    try {
      if (response.getHeaders() != null && response.getContent() != null) {
        if (HttpUtils.isExpectedContentType(response.getHeaders(), APPLICATION_JSON)) {
          return GsonHelper.fromJson(new String(response.getContent(), StandardCharsets.UTF_8), Problem.class);
        }
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
    return null;
  }

}

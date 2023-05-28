package cz.sefira.obelisk.api.ws;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.ws.HttpResponseException
 *
 * Created: 26.05.2023
 * Author: hlavnicka
 */

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.util.TextUtils;

/**
 * Signals a non 2xx HTTP response.
 */
public class HttpResponseException extends ClientProtocolException {

  private final int statusCode;
  private final String reasonPhrase;
  private byte[] content;
  private Header[] headers;

  public HttpResponseException(final int statusCode, final String reasonPhrase) {
    super(String.format("status code: %d" +
        (TextUtils.isBlank(reasonPhrase) ? "" : ", reason phrase: %s"), statusCode, reasonPhrase));
    this.statusCode = statusCode;
    this.reasonPhrase = reasonPhrase;
  }

  public HttpResponseException(final int statusCode, final String reasonPhrase,
                               final Header[] headers, final byte[] content) {
    this(statusCode, reasonPhrase);
    this.headers = headers;
    this.content = content;
  }

  public int getStatusCode() {
    return this.statusCode;
  }

  public String getReasonPhrase() {
    return this.reasonPhrase;
  }

  public byte[] getContent() {
    return content;
  }

  public Header[] getHeaders() {
    return headers;
  }
}

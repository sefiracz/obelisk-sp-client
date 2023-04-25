package cz.sefira.obelisk.api.ws.ssl;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.ws.HttpResponse
 *
 * Created: 13.03.2023
 * Author: hlavnicka
 */

import org.apache.hc.core5.http.Header;

public class HttpResponse {

  private final int code;
  private final String reasonPhrase;
  private final byte[] content;
  private final Header[] headers;

  public HttpResponse(int httpCode, String reasonPhrase, Header[] headers, byte[] content) {
    this.code = httpCode;
    this.reasonPhrase = reasonPhrase;
    this.headers = headers;
    this.content = content;
  }

  public int getCode() {
    return code;
  }

  public String getReasonPhrase() {
    return reasonPhrase;
  }

  public byte[] getContent() {
    return content;
  }

  public Header[] getHeaders() {
    return headers;
  }
}

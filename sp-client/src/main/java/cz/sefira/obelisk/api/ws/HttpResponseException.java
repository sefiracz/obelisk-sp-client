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

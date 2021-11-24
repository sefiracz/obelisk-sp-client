/**
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package cz.sefira.obelisk.api;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.SessionValue
 *
 * Created: 28.02.2021
 * Author: hlavnicka
 */

import java.util.Objects;

/**
 * Session value holder
 */
public class SessionValue {

  private final String sessionId;
  private final String sessionSignature;

  public SessionValue(String sessionId, String sessionSignature) {
    this.sessionId = sessionId;
    this.sessionSignature = sessionSignature;
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getSessionSignature() {
    return sessionSignature;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SessionValue that = (SessionValue) o;
    return Objects.equals(sessionId, that.sessionId) && Objects.equals(sessionSignature, that.sessionSignature);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sessionId, sessionSignature);
  }
}

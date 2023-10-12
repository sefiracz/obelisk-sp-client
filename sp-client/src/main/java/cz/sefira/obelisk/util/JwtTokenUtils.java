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
package cz.sefira.obelisk.util;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.util.TokenUtils
 *
 * Created: 08.03.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.ws.auth.JwtToken;
import cz.sefira.obelisk.json.GsonHelper;
import org.apache.commons.codec.binary.Base64;

/**
 * JWT token utils
 */
public class JwtTokenUtils {

  /**
   * Parse payload from well-formed JWT consisting of three concatenated Base64url-encoded strings, separated by dots (.):
   * @param fullToken Base64url-encoded strings, separated by dots (.)
   * @return Parsed JWS payload object
   */
  public static JwtToken parsePayload(String fullToken) {
    String[] parts = fullToken.split("\\.");
    if (parts.length != 3) {
      throw new IllegalStateException("Unexpected number of parts: " + parts.length);
    }
    return GsonHelper.fromJson(new String(Base64.decodeBase64(parts[1])), JwtToken.class);
  }

  /**
   * Determines if the JWT token is expired or not
   * @param fullToken Base64url-encoded strings, separated by dots (.)
   * @return True if the token is expired, false otherwise
   */
  public static boolean isExpired(String fullToken) {
    JwtToken token = parsePayload(fullToken);
    long expiration = token.getExpiration();
    long issuedAt = token.getIssuedAt();
    long threshold = (expiration - issuedAt) / 10; // 10% of time is reserved for refresh period
    return expiration < -1L || expiration > 0L && (System.currentTimeMillis() / 1000L) + threshold >= expiration;
  }
}

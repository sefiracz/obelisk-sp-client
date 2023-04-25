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
package cz.sefira.obelisk.json;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.json.DigestAlgorithmAdapter
 *
 * Created: 04.03.2021
 * Author: hlavnicka
 */

import com.google.gson.*;
import cz.sefira.obelisk.dss.DigestAlgorithm;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DigestAlgorithmIdentifierFinder;

import java.lang.reflect.Type;

public class DigestAlgorithmAdapter implements JsonSerializer<DigestAlgorithm>, JsonDeserializer<DigestAlgorithm> {

  @Override
  public DigestAlgorithm deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    DigestAlgorithmIdentifierFinder hashAlgorithmFinder = new DefaultDigestAlgorithmIdentifierFinder();
    AlgorithmIdentifier digestAlgorithmIdentifier = hashAlgorithmFinder.find(json.getAsString());
    return DigestAlgorithm.forOID(digestAlgorithmIdentifier.getAlgorithm().getId());
  }

  @Override
  public JsonElement serialize(DigestAlgorithm src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(src.getJavaName());
  }

}

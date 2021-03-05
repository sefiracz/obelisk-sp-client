package lu.nowina.nexu.json;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.json.DigestAlgorithmAdapter
 *
 * Created: 04.03.2021
 * Author: hlavnicka
 */

import com.google.gson.*;
import eu.europa.esig.dss.DigestAlgorithm;
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

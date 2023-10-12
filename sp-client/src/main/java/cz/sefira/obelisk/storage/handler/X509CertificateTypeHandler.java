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
package cz.sefira.obelisk.storage.handler;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.storage.handler.X509CertificateHandler
 *
 * Created: 28.05.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.util.X509Utils;
import one.microstream.persistence.binary.internal.AbstractBinaryHandlerCustomValue;
import one.microstream.persistence.binary.types.Binary;
import one.microstream.persistence.types.PersistenceLoadHandler;
import one.microstream.persistence.types.PersistenceStoreHandler;

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * X509Certificate type handler
 */
public class X509CertificateTypeHandler extends AbstractBinaryHandlerCustomValue<X509Certificate, byte[]> {

  public X509CertificateTypeHandler() {
    super(X509Certificate.class, CustomFields(bytes("encodedBytes")));
  }

  private static byte[] instanceState(final X509Certificate instance) {
    try {
      return instance.getEncoded();
    } catch (CertificateEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  private static byte[] binaryState(final Binary data) {
    return data.build_bytes();
  }

  @Override
  public void store(Binary data, X509Certificate instance, long objectId, PersistenceStoreHandler<Binary> handler) {
    data.store_bytes(this.typeId(), objectId, instanceState(instance));
  }

  @Override
  public X509Certificate create(Binary data, PersistenceLoadHandler handler) {
    try {
      return X509Utils.getCertificateFromBytes(binaryState(data));
    } catch (CertificateException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public byte[] getValidationStateFromInstance(X509Certificate instance) {
    return instanceState(instance);
  }

  @Override
  public byte[] getValidationStateFromBinary(Binary data) {
    return binaryState(data);
  }

  @Override
  public boolean hasVaryingPersistedLengthInstances() {
    return true;
  }

}
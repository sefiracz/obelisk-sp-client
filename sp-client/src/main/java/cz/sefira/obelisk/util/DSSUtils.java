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
package cz.sefira.obelisk.util;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.Utils
 *
 * Created: 08.01.2021
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.flow.OperationFactory;
import cz.sefira.obelisk.dss.DSSException;
import cz.sefira.obelisk.dss.DigestAlgorithm;
import cz.sefira.obelisk.generic.SessionManager;
import cz.sefira.obelisk.view.DialogMessage;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class DSSUtils {

  private static final Logger logger = LoggerFactory.getLogger(DSSUtils.class.getName());

  /**
   * Checks if the exception is pertaining to wrong user password input
   * @param e Throw exception this method analyzes
   * @param operationFactory
   * @param api
   * @return True if it is wrong password, false if not and this exception is to be re-thrown
   */
  public static boolean checkWrongPasswordInput(Exception e, OperationFactory operationFactory, PlatformAPI api) {
    String exception = TextUtils.printException(e);
    String msg;
    if(exception.contains("CKR_PIN_INCORRECT") || exception.contains("CKR_PIN_LEN_RANGE")
        || exception.contains("PIN is incorrect")) {
      msg = "key.selection.error.pin.incorrect";
    } else if (exception.contains("keystore password was incorrect")) {
      msg = "key.selection.error.password.incorrect";
    } else if(e.getCause() instanceof UnrecoverableKeyException) {
      msg = "key.selection.error.password.incorrect"; // TODO - different key x keystore password, other issues???
    } else if (exception.contains("CKR_PIN_LOCKED")) {
      msg = "key.selection.error.pin.locked";
    } else if (exception.contains("Integrity check failed: java.security.UnrecoverableKeyException")) {
      msg = "key.selection.keystore.integrity.failed";
    } else {
      return false; // unknown exception - re-throw
    }
    SessionManager.getManager().destroy();
    operationFactory.getMessageDialog(api, new DialogMessage(msg, DialogMessage.Level.WARNING,
            410, 155), true);
    return true;
  }

  public static byte[] digest(final DigestAlgorithm digestAlgorithm, final byte[] data) throws DSSException {
    final MessageDigest messageDigest = getMessageDigest(digestAlgorithm);
    return messageDigest.digest(data);
  }

  public static MessageDigest getMessageDigest(final DigestAlgorithm digestAlgorithm) {
    try {
      final String digestAlgorithmOid = digestAlgorithm.getOid();
      return MessageDigest.getInstance(digestAlgorithmOid, BouncyCastleProvider.PROVIDER_NAME);
    } catch (GeneralSecurityException e) {
      throw new DSSException("Digest algorithm '" + digestAlgorithm.getName() + "' error: " + e.getMessage(), e);
    }
  }

  public static List<String> getAccessLocations(final X509Certificate certificate) {
    final byte[] authInfoAccessExtensionValue = certificate.getExtensionValue(Extension.authorityInfoAccess.getId());
    if (null == authInfoAccessExtensionValue) {
      return null;
    }
    // Parse the extension
    ASN1Sequence asn1Sequence;
    try (ASN1InputStream aiaInput = new ASN1InputStream(authInfoAccessExtensionValue)) {
      final DEROctetString s = (DEROctetString) aiaInput.readObject();
      final byte[] content = s.getOctets();
      try (ASN1InputStream contentInput = new ASN1InputStream(content)) {
        asn1Sequence = (ASN1Sequence) contentInput.readObject();
      }
    }
    catch (IOException e) {
      logger.error(e.getMessage(), e);
      return null;
    }
    // process the URLs
    AuthorityInformationAccess authorityInformationAccess = AuthorityInformationAccess.getInstance(asn1Sequence);
    AccessDescription[] accessDescriptions = authorityInformationAccess.getAccessDescriptions();
    List<String> locationsUrls = new ArrayList<>();
    for (AccessDescription accessDescription : accessDescriptions) {
      if (X509ObjectIdentifiers.id_ad_caIssuers.equals(accessDescription.getAccessMethod())) {
        GeneralName gn = accessDescription.getAccessLocation();
        if (GeneralName.uniformResourceIdentifier == gn.getTagNo()) {
          DERIA5String str = (DERIA5String) ((DERTaggedObject) gn.toASN1Primitive()).getObject();
          locationsUrls.add(str.getString());
        }
      }
    }
    return locationsUrls;
  }

}

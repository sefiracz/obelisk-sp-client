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
package cz.sefira.obelisk;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.Utils
 *
 * Created: 08.01.2021
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.EnvironmentInfo;
import cz.sefira.obelisk.api.OS;
import cz.sefira.obelisk.generic.SessionManager;
import cz.sefira.obelisk.view.DialogMessage;
import cz.sefira.obelisk.api.NexuAPI;
import cz.sefira.obelisk.api.flow.OperationFactory;
import cz.sefira.obelisk.view.x509.CertificateInfoDialog;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class Utils {

  private static final Logger logger = LoggerFactory.getLogger(Utils.class.getName());

  private static final FastDateFormat XS_DATE_TIME_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  public static String formatXsDateTime(Date date) {
    StringBuilder sb = new StringBuilder(XS_DATE_TIME_FORMAT.format(date));
    return sb.insert(sb.length() - 2, ":").toString();
  }

  public static String encodeHexString(byte[] byteArray) {
    StringBuilder hexStringBuffer = new StringBuilder();
    for (byte num : byteArray) {
      char[] hexDigits = new char[2];
      hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
      hexDigits[1] = Character.forDigit((num & 0xF), 16);
      hexStringBuffer.append(new String(hexDigits));
    }
    return hexStringBuffer.toString().toUpperCase();
  }

  public static String printException(Throwable t) {
    if (t == null) return null;
    StringWriter sw = new StringWriter();
    t.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }

  public static X509Certificate getCertificateFromBase64(String base64certificate) throws CertificateException {
    byte[] cert = Base64.decodeBase64(base64certificate);
    CertificateFactory factory = CertificateFactory.getInstance("X509");
    return  (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(cert));
  }

  /**
   * Checks if the exception is pertaining to wrong user password input
   * @param e Throw exception this method analyzes
   * @param operationFactory
   * @param api
   * @return True if it is wrong password, false if not and this exception is to be re-thrown
   */
  public static boolean checkWrongPasswordInput(Exception e, OperationFactory operationFactory, NexuAPI api) {
    String exception = Utils.printException(e);
    String msg;
    if(exception.contains("CKR_PIN_INCORRECT") || exception.contains("CKR_PIN_LEN_RANGE")) {
      msg = "key.selection.error.pin.incorrect";
    } else if (exception.contains("keystore password was incorrect")) {
      msg = "key.selection.error.password.incorrect";
    } else if(e.getCause() instanceof UnrecoverableKeyException) {
      msg = "key.selection.error.password.incorrect"; // TODO - different key x keystore password, other issues???
    }  else if (exception.contains("CKR_PIN_LOCKED")) {
      msg = "key.selection.error.pin.locked";
    } else {
      return false; // unknown exception - re-throw
    }
    SessionManager.getManager().destroy();
    operationFactory.getMessageDialog(api, new DialogMessage(msg, DialogMessage.Level.WARNING,
            400, 150), true);
    return true;
  }

  public static void openPEMCertificate(String pemCertificate) {
    try {
      Certificate certificate = CertificateFactory.getInstance("X509").generateCertificate(
          new ByteArrayInputStream(pemCertificate.getBytes(StandardCharsets.UTF_8)));
      SwingUtilities.invokeLater(() -> new CertificateInfoDialog(certificate));
    }
    catch (CertificateException e) {
      logger.error(e.getMessage(), e);
    }
  }

  public static String wrapPEMCertificate(String certificate) {
    byte[] encoded = Base64.decodeBase64(certificate);
    certificate = StringUtils.newStringUtf8(Base64.encodeBase64Chunked(encoded));
    String begin = "-----BEGIN CERTIFICATE-----";
    String end = "-----END CERTIFICATE-----";
    if(!certificate.startsWith(begin)) {
      certificate = begin + "\n" + certificate;
    }
    if(!certificate.endsWith(end)) {
      certificate = certificate + end;
    }
    return certificate;
  }

  public static String createKeyUsageString(final X509Certificate certificate, final ResourceBundle resources) {
    final boolean[] keyUsages = certificate.getKeyUsage();
    if (keyUsages == null) {
      return "";
    }
    final List<String> keyUsageList = new ArrayList<>();
    if (keyUsages[0]) {
      keyUsageList.add(resources.getString("keyUsage.digitalSignature"));
    }
    if (keyUsages[1]) {
      keyUsageList.add(resources.getString("keyUsage.nonRepudiation"));
    }
    if (keyUsages[2]) {
      keyUsageList.add(resources.getString("keyUsage.keyEncipherment"));
    }
    if (keyUsages[3]) {
      keyUsageList.add(resources.getString("keyUsage.dataEncipherment"));
    }
    if (keyUsages[4]) {
      keyUsageList.add(resources.getString("keyUsage.keyAgreement"));
    }
    if (keyUsages[5]) {
      keyUsageList.add(resources.getString("keyUsage.keyCertSign"));
    }
    if (keyUsages[6]) {
      keyUsageList.add(resources.getString("keyUsage.crlSign"));
    }
    if (keyUsages[7]) {
      keyUsageList.add(resources.getString("keyUsage.encipherOnly"));
    }
    if (keyUsages[8]) {
      keyUsageList.add(resources.getString("keyUsage.decipherOnly"));
    }
    // comma separated list
    return String.join(", ", keyUsageList);
  }

}

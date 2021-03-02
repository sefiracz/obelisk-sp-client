/**
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Licensed under EUPL Version 1.1 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package lu.nowina.nexu;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.Utils
 *
 * Created: 08.01.2021
 * Author: hlavnicka
 */

import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.flow.OperationFactory;
import lu.nowina.nexu.generic.SessionManager;
import lu.nowina.nexu.view.DialogMessage;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.awt.*;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Date;

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

  public static boolean checkWrongPasswordInput(Exception e, OperationFactory operationFactory, NexuAPI api) {
    String exception = Utils.printException(e);
    String msg;
    if(exception.contains("CKR_PIN_INCORRECT") || exception.contains("CKR_PIN_LEN_RANGE")) {
      msg = "key.selection.error.pin.incorrect";
    } else if (exception.contains("keystore password was incorrect")) {
      msg = "key.selection.error.password.incorrect";
    } else if(e.getCause() instanceof UnrecoverableKeyException) {
      msg = "key.selection.error.password.incorrect"; // TODO - muze byt jine heslo ke klici nez keystore/jiny technicky problem/???
    }  else if (exception.contains("CKR_PIN_LOCKED")) {
      msg = "key.selection.error.pin.locked";
    } else {
      return true; // unknown exception - re-throw
    }
    SessionManager.getManager().destroy();
    operationFactory.getMessageDialog(api, new DialogMessage(msg, DialogMessage.Level.WARNING,
            400, 150), true);
    return false;
  }

  public static char[] getDecodedValue(String base64) {
    if (base64 != null)
      return new String(Base64.decodeBase64(base64)).toCharArray();
    else
      return null;
  }

  public static byte[] verifySignature(byte[] signature, Certificate certificate) {
    try {
      Cipher rsa = Cipher.getInstance("RSA");
      rsa.init(Cipher.DECRYPT_MODE, certificate);
      rsa.update(signature);
      return rsa.doFinal();
    } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException |
            IllegalBlockSizeException | BadPaddingException e) {
      logger.error(e.getMessage(), e);
      return null;
    }
  }

  public static void openCertificate(String certificate) {
    if (Desktop.isDesktopSupported()) {
      try {
        final File tmpFile = File.createTempFile("certificate", ".crt");
        tmpFile.deleteOnExit();
        final FileWriter writer = new FileWriter(tmpFile);
        writer.write(certificate);
        writer.close();
        new Thread(() -> {
          try {
            Desktop.getDesktop().open(tmpFile);
          } catch (final IOException e) {
            logger.error(e.getMessage(), e);
          }
        }).start();
      } catch (final Exception e) {
        logger.error(e.getMessage(), e);
      }
    }
  }


}

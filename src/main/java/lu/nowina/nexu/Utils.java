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

import lu.nowina.nexu.api.*;
import lu.nowina.nexu.api.flow.OperationFactory;
import lu.nowina.nexu.generic.PasswordManager;
import lu.nowina.nexu.generic.SCInfo;
import lu.nowina.nexu.view.core.UIOperation;
import org.apache.commons.lang.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.*;
import java.security.UnrecoverableKeyException;
import java.util.Date;
import java.util.List;

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
      msg = "key.selection.error.password.incorrect"; // TODO - muze byt spatne heslo ke klici/technicky problem/???
    }  else if (exception.contains("CKR_PIN_LOCKED")) {
      msg = "key.selection.error.pin.locked";
    } else {
      return true; // unknown exception - re-throw
    }
    PasswordManager.getInstance().destroy();
    operationFactory.getOperation(UIOperation.class, "/fxml/message.fxml", new Object[] {
        msg, api.getAppConfig().getApplicationName(), 375, 120
    }).perform();
    return false;
  }

  /**
   * Check if adapter is for PKCS11 and the library is present on expected location
   *
   * @param matchingProductAdapters List of matching product adapters
   * @return True if library is present or if product is not PKCS11
   */
  // TODO ??? stale aktualni a potreba? // mozna lepe jit skrz PKCS11Manager
  public static boolean isPkcs11LibraryPresent(List<Match> matchingProductAdapters) {
    if(!matchingProductAdapters.isEmpty()) {
      Product p = matchingProductAdapters.get(0).getProduct();
      if(p instanceof SCInfo && ((SCInfo) p).getType().equals(KeystoreType.PKCS11) &&
          ((SCInfo) p).getInfos().get(0).getSelectedApi().equals(ScAPI.PKCS_11)) {
        String pkcs11Path = ((SCInfo)p).getInfos().get(0).getApiParam();
        return new File(pkcs11Path).exists() && new File(pkcs11Path).canRead();
      }
    }
    return true; // product isn't PKCS11 so it's ok
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

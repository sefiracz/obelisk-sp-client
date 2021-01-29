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

import eu.europa.esig.dss.DSSException;
import eu.europa.esig.dss.token.PasswordInputCallback;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import iaik.pkcs.pkcs11.TokenException;
import lu.nowina.nexu.api.*;
import lu.nowina.nexu.api.flow.OperationFactory;
import lu.nowina.nexu.generic.IaikPkcs11SignatureTokenAdapter;
import lu.nowina.nexu.generic.SunPkcs11SignatureTokenAdapter;
import lu.nowina.nexu.generic.ProductPasswordManager;
import lu.nowina.nexu.generic.SCInfo;
import lu.nowina.nexu.view.core.UIOperation;
import org.apache.commons.lang.time.FastDateFormat;
import sun.security.pkcs11.wrapper.PKCS11RuntimeException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {

  private static final FastDateFormat XS_DATE_TIME_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  public static final Map<String, IaikPkcs11SignatureTokenAdapter> PKCS11_ADAPTERS = new HashMap<>();

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

  public static boolean checkPasswordInput(Exception e, OperationFactory operationFactory, NexuAPI api) {
    String exception = Utils.printException(e);
    String msg;
    if(exception.contains("CKR_PIN_INCORRECT")) {
      msg = "key.selection.error.pin.incorrect";
    } else if (exception.contains("keystore password was incorrect")) {
      msg = "key.selection.error.password.incorrect";
    } else if (exception.contains("CKR_PIN_LOCKED")) {
      msg = "key.selection.error.pin.locked";
    } else {
      return false; // unknown exception - re-throw
    }
    ProductPasswordManager.getInstance().destroy();
    operationFactory.getOperation(UIOperation.class, "/fxml/message.fxml", new Object[] {
        msg, api.getAppConfig().getApplicationName(), 375, 120
    }).perform();
    return true;
  }

  /**
   * Check if adapter is for PKCS11 and the library is present on expected location
   *
   * @param matchingProductAdapters List of matching product adapters
   * @return True if library is present or if product is not PKCS11
   */
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

  /**
   * Checks if preconfigured smartcard has a same slot index as it has been detected
   * @param api NexuAPI
   * @param card Preconfigured smartcard
   */
  public static void checkSlotIndex(NexuAPI api, DetectedCard card) {
    boolean deviceFound = false;
    List<DetectedCard> detectedCards = api.detectCards();
    for(DetectedCard c : detectedCards) {
      if(c.getAtr().equals(card.getAtr())) {
        card.setTerminalIndex(c.getTerminalIndex());
        deviceFound = true;
        break;
      }
    }
    if(detectedCards.isEmpty() || !deviceFound) {
      throw new PKCS11RuntimeException("No card detected");
    }
  }

  /**
   * Creates or returns previously stored Pkcs11SignatureTokenAdapter for given product
   *
   * @param card DetectedCard product
   * @param absolutePkcs11Path Absolute path to PKCS11 native lib
   * @param callback PasswordInput callback
   * @return Returns freshly created or previously stored Pkcs11SignatureTokenAdapter
   */
  public static SignatureTokenConnection getStoredPkcs11TokenAdapter(DetectedCard card, String absolutePkcs11Path,
                                                                     PasswordInputCallback callback) {
    try {
      IaikPkcs11SignatureTokenAdapter adapter = PKCS11_ADAPTERS.get(card.getAtr());
      if(adapter == null || adapter.isModuleFinalized()) {
        adapter = new IaikPkcs11SignatureTokenAdapter(new File(absolutePkcs11Path), callback, card);
        PKCS11_ADAPTERS.put(card.getAtr(), adapter); // TODO - replace ATR with TokenInfo ???
      }
      return adapter;
    } catch (IOException | TokenException e) {
      throw new DSSException(e);
    }
  }
}

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

import cz.sefira.obelisk.api.AbstractProduct;
import cz.sefira.obelisk.token.keystore.ConfiguredKeystore;
import cz.sefira.obelisk.token.pkcs11.DetectedCard;
import cz.sefira.obelisk.util.annotation.NotNull;
import org.apache.commons.lang.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Text utility methods
 */
public class TextUtils {

  private static final Logger logger = LoggerFactory.getLogger(TextUtils.class.getName());

  private static final FastDateFormat XS_DATE_TIME_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  public static String formatXsDateTime(Date date) {
    StringBuilder sb = new StringBuilder(XS_DATE_TIME_FORMAT.format(date));
    return sb.insert(sb.length() - 2, ":").toString();
  }

  public static String localizedDatetime(Date date, boolean includeTime) {
    if (date == null)
      return "";
    String timeFormat = ResourceUtils.getBundle().getString("date.format.pattern");
    timeFormat = (includeTime ? timeFormat+" HH:mm:ss" : timeFormat);
    SimpleDateFormat sdf = new SimpleDateFormat(timeFormat);
    return sdf.format(date);
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

  public static boolean isCausedBy(@NotNull Throwable t, Class<? extends Throwable> cl) {
    while (t != null) {
      if (t.getClass() == cl) {
        return true;
      }
      Throwable cause = t.getCause();
      if (cause != t) {
        t = cause;
      } else {
        break;
      }
    }
    return false;
  }

  public static String getProductLabel(AbstractProduct value) {
    if(value instanceof ConfiguredKeystore) {
      String path = ((ConfiguredKeystore) value).getUrl();
      try {
        path = Paths.get(new URI(path)).toFile().getAbsolutePath();
      }
      catch (URISyntaxException e) {
        logger.error(e.getMessage(), e);
      }
      return path;
    } else if(value instanceof DetectedCard) {
      return value.getSimpleLabel();
    } else {
      return value.getLabel();
    }
  }

}

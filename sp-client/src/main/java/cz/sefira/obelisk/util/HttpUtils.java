package cz.sefira.obelisk.util;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.util.HttpUtils
 *
 * Created: 03.04.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.ws.ssl.HttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;

/**
 * description
 */
public class HttpUtils {

    public static String getLocationURI(HttpResponse response) {
        for (Header h : response.getHeaders()) {
            if (HttpHeaders.LOCATION.equals(h.getName())) {
                return h.getValue();
            }
        }
        return null;
    }

}

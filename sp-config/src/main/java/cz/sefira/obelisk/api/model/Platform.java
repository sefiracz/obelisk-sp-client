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
package cz.sefira.obelisk.api.model;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.model.Platform
 *
 * Created: 21.04.2023
 * Author: hlavnicka
 */


/**
 * Returns software platform string
 */
public class Platform {

  public static String get() {
    OS os = OS.getOS();
    switch (os) {
      case LINUX:
        return "linux64";
      case MACOSX:
        return "macos64";
      case WINDOWS:
      default:
        return "win64";
    }
  }

}

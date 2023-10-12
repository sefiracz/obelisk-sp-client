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
package cz.sefira.obelisk.dss.x509;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.dss.x509.ExtendedKeyUsage
 *
 * Created: 16.03.2023
 * Author: hlavnicka
 */

/**
 * description
 */
public class ExtendedKeyUsage {

  public static final String SERVER_AUTH = "1.3.6.1.5.5.7.3.1";
  public static final String CLIENT_AUTH = "1.3.6.1.5.5.7.3.2";
  public static final String CODE_SIGN = "1.3.6.1.5.5.7.3.3";
  public static final String EMAIL = "1.3.6.1.5.5.7.3.4";
  public static final String TIMESTAMP = "1.3.6.1.5.5.7.3.8";
  public static final String OCSP_SIGN = "1.3.6.1.5.5.7.3.9";
  public static final String DOC_SIGN = "1.3.6.1.4.1.311.10.3.12";

}

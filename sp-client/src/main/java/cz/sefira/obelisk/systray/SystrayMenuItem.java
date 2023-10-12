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
package cz.sefira.obelisk.systray;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.systray.SystrayMenuItem
 *
 * Created: 24.05.2023
 * Author: hlavnicka
 */

/**
 * description
 */
public class SystrayMenuItem {

  private final String label;
  private final String name;
  private final Runnable runnable;

  public SystrayMenuItem(String label, String name, Runnable runnable) {
    this.label = label;
    this.name = name;
    this.runnable = runnable;
  }

  public String getLabel() {
    return label;
  }

  public String getName() {
    return name;
  }

  public Runnable getOperation() {
    return runnable;
  }
}

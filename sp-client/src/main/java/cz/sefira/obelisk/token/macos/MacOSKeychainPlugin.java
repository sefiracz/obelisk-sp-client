/**
 * © SEFIRA spol. s r.o., 2020-2021
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
package cz.sefira.obelisk.token.macos;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.macos.MacOSKeychainPlugin
 *
 * Created: 06.12.2021
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.model.OS;
import cz.sefira.obelisk.api.plugin.InitErrorMessage;
import cz.sefira.obelisk.api.plugin.SignaturePlugin;

import java.util.Collections;
import java.util.List;

/**
 * Plugin that registers MacOS KeyChain implementation.
 */
public class MacOSKeychainPlugin implements SignaturePlugin {

  public MacOSKeychainPlugin() {
    super();
  }

  @Override
  public List<InitErrorMessage> init(String pluginId, PlatformAPI api) {
    if (OS.MACOSX.equals(api.getEnvironmentInfo().getOs())) {
      api.registerProductAdapter(new KeychainProductAdapter(api));
    }
    return Collections.emptyList();
  }
}

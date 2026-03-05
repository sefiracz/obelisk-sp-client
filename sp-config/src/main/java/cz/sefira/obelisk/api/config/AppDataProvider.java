/**
 * © SEFIRA spol. s r.o., 2020-2025
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
package cz.sefira.obelisk.api.config;

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.AppData;
import cz.sefira.obelisk.api.model.OS;

public class AppDataProvider {

  private static volatile AppData appData;

  public static AppData get() {
    if (appData == null) {
      synchronized (AppDataProvider.class) {
        if (appData == null) {
          String applicationName = AppConfig.get().getApplicationName();
          String companyName = AppConfig.get().getCompanyName();
          String appId = "cz.sefira.obelisk.SigningPortal";
          if (OS.isWindows())
            appData = new WindowsAppData(applicationName, companyName);
          else if (OS.isLinux())
            appData = new LinuxAppData(applicationName, companyName);
          else if (OS.isMacOS())
            appData = new MacOSAppData(applicationName, companyName, appId);
          else
            throw new IllegalStateException("Unexpected operating system: '" + System.getProperty("os.name") + "'");
        }
      }
    }
    return appData;
  }

}

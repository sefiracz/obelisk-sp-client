package cz.sefira.obelisk.api.plugin;

import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.model.OS;
import cz.sefira.obelisk.api.plugin.encrypt.WinDPAPI;
import cz.sefira.obelisk.api.plugin.encrypt.DataProtection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DataProtectionPlugin implements AppPlugin {

  private static final Logger logger = LoggerFactory.getLogger(DataProtectionPlugin.class.getName());

  private DataProtection dataProtection;

  @Override
  public List<InitErrorMessage> init(String pluginId, PlatformAPI api) {
    try {
      switch (OS.getOS()) {
        case WINDOWS -> dataProtection = new WinDPAPI();
        case LINUX, MACOSX -> {
          String notAvailable = "DataProtection plugin not implemented for OS: " + OS.getOS().name();
          if (isOptional()) {
            throw new OptionalPluginException(notAvailable);
          }
          throw new UnsupportedOperationException(notAvailable);
        }
        default -> {
          String notAvailable = "Unrecognizable OS, no known implementation available for plugin " + DataProtectionPlugin.class.getSimpleName();
          if (isOptional()) {
            throw new OptionalPluginException(notAvailable);
          }
          throw new UnsupportedOperationException(notAvailable);
        }
      }
      return List.of();
    } catch (Exception e) {
      if (isOptional() && e instanceof OptionalPluginException) {
        throw e;
      }
      logger.error(e.getMessage(), e);
      return List.of(new InitErrorMessage(this.getClass().getSimpleName(), "error.application.init", e));
    }
  }

  public byte[] encryptData(byte[] data) throws Exception {
    return dataProtection.encryptData(data);
  }

  public byte[] decryptData(byte[] data) throws Exception {
    return dataProtection.decryptData(data);
  }

  @Override
  public boolean isOptional() {
    return true;
  }
}

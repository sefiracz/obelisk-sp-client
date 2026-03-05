package cz.sefira.obelisk.api.plugin.encrypt;

import cz.sefira.obelisk.api.model.OS;
import cz.sefira.obelisk.api.plugin.OptionalPluginException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.windpapi4j.WinDPAPI.CryptProtectFlag.CRYPTPROTECT_AUDIT;
import static com.github.windpapi4j.WinDPAPI.CryptProtectFlag.CRYPTPROTECT_NO_RECOVERY;

/**
 * Data encryption plugin implemented by using Windows DPAPI (DataProtection API)
 */
public class WinDPAPI implements DataProtection {

  private static final Logger logger = LoggerFactory.getLogger(WinDPAPI.class.getName());

  private final com.github.windpapi4j.WinDPAPI winDPAPI;

  public WinDPAPI() {
    try {
      if(!com.github.windpapi4j.WinDPAPI.isPlatformSupported()) {
        throw new OptionalPluginException("DPAPI: unsupported platform "+ OS.getOS().name());
      }
      winDPAPI = com.github.windpapi4j.WinDPAPI.newInstance(CRYPTPROTECT_NO_RECOVERY, CRYPTPROTECT_AUDIT);
    } catch (Throwable e) {
      if (e instanceof NoClassDefFoundError) {
        throw new OptionalPluginException("WinDPAPI not available");
      } else if (e instanceof OptionalPluginException optional) {
        throw optional;
      }
      logger.error(e.getMessage(), e);
      throw new RuntimeException("DPAPI initialization failed: "+e.getMessage(), e);
    }
  }

  @Override
  public byte[] encryptData(byte[] data) throws Exception {
    return winDPAPI.protectData(data);
  }

  @Override
  public byte[] decryptData(byte[] data) throws Exception {
    return winDPAPI.unprotectData(data);
  }

}

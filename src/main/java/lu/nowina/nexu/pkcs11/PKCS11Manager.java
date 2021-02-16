package lu.nowina.nexu.pkcs11;

import iaik.pkcs.pkcs11.TokenException;
import lu.nowina.nexu.api.*;
import lu.nowina.nexu.generic.SCDatabase;
import lu.nowina.nexu.generic.SCInfo;
import lu.nowina.nexu.generic.SmartcardInfoDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PKCS11Manager {

  private static final Logger log = LoggerFactory.getLogger(PKCS11Module.class.getName());

  private final NexuAPI api;

  /**
   * Smartcards information database
   */
  private final SmartcardInfoDatabase scInfoDatabase;
  private byte[] databaseDigest = new byte[0];

  /**
   * Known smartcard models
   */
  private Map<String /* ATR */, SmartcardInfo> supported;
  private Map<String /* ATR */, SmartcardInfo> registered;

  /**
   * Currently initialized PKCS11 modules
   */
  private Map<String /* PKCS11 library path */, PKCS11Module> modules;

  public PKCS11Manager(NexuAPI api, SmartcardInfoDatabase scInfoDatabase) {
    this.api = api;
    this.scInfoDatabase = scInfoDatabase;
    this.supported = scInfoDatabase != null ? scInfoDatabase.getSmartcardInfosMap() : new ConcurrentHashMap<>();
    this.modules = new ConcurrentHashMap<>();
    this.registered = new ConcurrentHashMap<>();
    registerSavedCards();
  }

  /**
   * Register smartcard informations
   * @param infos Collection of smartcard informations
   * @param infosDigest Digest of smartcard informations data
   */
  public void registerSmartcardInfos(List<SmartcardInfo> infos, byte[] infosDigest) {
    // check digest and update only if new informations
    if(infos != null && infosDigest != null && !Arrays.equals(databaseDigest, infosDigest)) {
      this.databaseDigest = infosDigest;
      this.scInfoDatabase.setSmartcardInfos(infos);
      this.supported = this.scInfoDatabase.getSmartcardInfosMap();
    }
  }

  /**
   * Register PKCS11 smartcard informations
   * @param info Smartcard informations
   */
  public void registerCard(SCInfo info) {
    if(info.getType().equals(KeystoreType.PKCS11)) {
      String pkcs11Path = info.getInfos().get(0).getApiParam();
      String atr = info.getAtr();
      SmartcardInfo smartcardInfo = registered.get(info.getAtr());
      if(smartcardInfo == null) {
        registered.putIfAbsent(atr, new SmartcardInfo(atr, pkcs11Path)); // register new known smartcard
        log.info("Registering '"+pkcs11Path+"' for ATR: "+info.getAtr());
      }
    }
  }

  /**
   * Add user stored cards to supported known models (in case they are not already known)
   */
  public void registerSavedCards() {
    SCDatabase scDatabase = api.loadDatabase(SCDatabase.class, "database-smartcard.xml");
    List<AbstractProduct> savedCards = scDatabase.getProducts();
    for(AbstractProduct savedCard : savedCards) {
      registerCard((SCInfo) savedCard);
    }
  }

  /**
   * Unregister smartcard informations
   * @param info Smartcard informations
   */
  public void unregisterCard(SCInfo info) {
    registered.remove(info.getAtr());
  }

  /**
   * Returns initialized PKCS11 module
   * @param atr ATR of card
   * @param pkcs11Path (Optional) PKCS11 library path (optional, but takes priority if present)
   * @return Initialized PKCS11 module
   * @throws IOException
   * @throws TokenException
   */
  public PKCS11Module getModule(String atr, String pkcs11Path) throws IOException, TokenException {
    // check if PKCS11 module is already initialized
    if(pkcs11Path != null && modules.get(pkcs11Path) != null) {
      return modules.get(pkcs11Path);
    }
    PKCS11Module module = null;
    // use given PKCS11 library path to initialize new PKCS11 module
    if(pkcs11Path != null && new File(pkcs11Path).exists() && new File(pkcs11Path).canRead()) {
      module = new PKCS11Module(pkcs11Path);
      modules.put(pkcs11Path, module);
      return module;
    }
    // check known PKCS11 models using given ATR
    SmartcardInfo info = registered.get(atr);
    info = info == null ? supported.get(atr) : info;
    if(info == null)
      return null;
    // check if PKCS11 library driver is present
    for(String pkcs11Driver : info.getDrivers()) {
      if(pkcs11Driver != null && new File(pkcs11Driver).exists() && new File(pkcs11Driver).canRead()) {
        // check if PKCS11 module is already initialized
        module = modules.get(pkcs11Driver);
        if (module == null) {
          // use given PKCS11 library path to initialize new PKCS11 module
          module = new PKCS11Module(pkcs11Driver);
          modules.put(pkcs11Driver, module);
        }
        break;
      }
    }
    return module;
  }

  public String getAvailablePkcs11Library(String atr) {
    SmartcardInfo info = getAvailableSmartcardInfo(atr);
    return getAvailableDriver(info);
  }

  /**
   * Get model name for given ATR
   * @param atr Detected card ATR
   * @return Card model name or ATR if unknown card
   */
  public String getName(String atr) {
    SmartcardInfo info = supported.get(atr);
    if(info != null && info.getModelName() != null && !info.getModelName().isEmpty()) {
      return info.getModelName();
    }
    return atr;
  }

  /**
   * Finalize all initialized PKCS11 modules
   */
  public void finalizeAllModules() {
    for(PKCS11Module module : modules.values()) {
      try {
        if(!module.isModuleFinalized()) {
          module.moduleFinalize();
        }
      } catch (Throwable t) {
        log.error("Error finalizing module: "+t.getMessage(), t);
      }
    }
    modules = new ConcurrentHashMap<>();
  }

  private SmartcardInfo getAvailableSmartcardInfo(String atr) {
    SmartcardInfo info = registered.get(atr);
    return info == null ? supported.get(atr) : info;
  }

  private String getAvailableDriver(SmartcardInfo info) {
    if(info != null && info.getDrivers() != null) {
      for(String driver : info.getDrivers()) {
        if(driver != null && new File(driver).exists() && new File(driver).canRead()) {
          return driver;
        } else {
          log.warn("PKCS11 driver '"+driver+"' is not present.");
        }
      }
    }
    return null;
  }

}

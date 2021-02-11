package lu.nowina.nexu.pkcs11;

import iaik.pkcs.pkcs11.TokenException;
import lu.nowina.nexu.api.DetectedCard;
import lu.nowina.nexu.api.SmartcardInfo;
import lu.nowina.nexu.generic.SmartcardInfoDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PKCS11Manager {

  private static final Logger log = LoggerFactory.getLogger(PKCS11Module.class.getName());

  private final SmartcardInfoDatabase database;
  private byte[] databaseDigest = new byte[0];

  /**
   * Known smartcard models
   */
  private Map<String /* ATR */, SmartcardInfo> models;

  /**
   * Currently initialized PKCS11 modules
   */
  private Map<String /* PKCS11 library path */, PKCS11Module> modules;

  public PKCS11Manager(SmartcardInfoDatabase database) {
    this.database = database;
    this.models = database.getSmartcardInfosMap();
    this.modules = new HashMap<>();
  }

  public void loadInfo(List<SmartcardInfo> infos, byte[] infosDigest) {
    // check digest and update only if new informations
    if(infos != null && infosDigest != null && !Arrays.equals(databaseDigest, infosDigest)) {
      this.databaseDigest = infosDigest;
      this.database.setSmartcardInfos(infos);
      // TODO - synchronizovat zapis a cteni mapy models?
      this.models = this.database.getSmartcardInfosMap();
    }
  }

  public TokenHandler getPkcs11TokenHandler(DetectedCard card, String pkcs11Path) throws IOException, TokenException {
    // TODO - ma pkcs11Path prioritu pred ConnectionInfo ?
    // TODO - pkcs11Path vicemene nikdy nebude null, jelikoz bud to na zacatku zadal uzivatel nebo ho uz zname z connectionInfo
    SmartcardInfo info = models.get(card.getAtr());
    if(info == null || info.getDrivers() == null || info.getDrivers().isEmpty()) {
      if(pkcs11Path != null) {
        PKCS11Module pkcs11Module = modules.get(pkcs11Path);
        if(pkcs11Module == null) {
          pkcs11Module = new PKCS11Module(pkcs11Path);
          modules.put(pkcs11Path, pkcs11Module);
        }
        return new TokenHandler(pkcs11Module, card.getTerminalLabel());
      }
    } else {
      PKCS11Module pkcs11Module = modules.get(info.getDrivers().get(0));
      if(pkcs11Module == null) {
        String pkcs11ModulePath = info.getDrivers().get(0);
        pkcs11Module = new PKCS11Module(pkcs11ModulePath);
        modules.put(pkcs11Path, pkcs11Module);
      }
      return new TokenHandler(pkcs11Module, card.getTerminalLabel());
    }
    return null;
  }

  public void setTokenInfo(DetectedCard d) throws IOException, TokenException {
    String tokenLabel = d.getAtr();
    SmartcardInfo info = models.get(d.getAtr());
    if(info != null) {
      tokenLabel = info.getModelName();
      String pkcs11ModulePath = info.getDrivers().get(0);
      if(pkcs11ModulePath != null) {
        PKCS11Module pkcs11Module = modules.get(pkcs11ModulePath);
        if(pkcs11Module == null) {
          pkcs11Module = new PKCS11Module(pkcs11ModulePath);
          modules.put(pkcs11ModulePath, pkcs11Module);
        }
        TokenHandler tokenHandler = new TokenHandler(pkcs11Module, d.getTerminalLabel());
        tokenHandler.setTokenInfo(d);
        return;
      }
    }
    d.setTokenLabel(tokenLabel.trim());
  }

  public String getAvailablePkcs11Library(String atr) {
    SmartcardInfo info = models.get(atr);
    if(info != null && info.getDrivers() != null && !info.getDrivers().isEmpty()) {
      return info.getDrivers().get(0);
    }
    return null;
  }

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
    modules = new HashMap<>();
  }

}

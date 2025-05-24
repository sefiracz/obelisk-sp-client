package cz.sefira.obelisk.api.plugin.encrypt;

public interface DataProtection {

  byte[] encryptData(byte[] data) throws Exception;

  byte[] decryptData(byte[] data) throws Exception;

}

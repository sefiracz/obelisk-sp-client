package lu.nowina.nexu.keystore;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.keystore.UnknownKeystoreTypeException
 *
 * Created: 17.02.2021
 * Author: hlavnicka
 */

public class UnsupportedKeystoreTypeException extends RuntimeException {

  private final String filePath;

  public UnsupportedKeystoreTypeException(String message, String keystorePath) {
    super(message);
    this.filePath = keystorePath;
  }

  public String getFilePath() {
    return filePath;
  }
}

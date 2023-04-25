package cz.sefira.obelisk.api.plugin.version;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.plugin.version.XMLMigrator
 *
 * Created: 05.04.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AbstractProduct;
import cz.sefira.obelisk.api.model.KeystoreType;
import cz.sefira.obelisk.api.model.OS;
import cz.sefira.obelisk.api.model.ScAPI;
import cz.sefira.obelisk.generic.ConnectionInfo;
import cz.sefira.obelisk.token.keystore.ConfiguredKeystore;
import cz.sefira.obelisk.token.macos.MacOSKeychain;
import cz.sefira.obelisk.token.pkcs11.DetectedCard;
import cz.sefira.obelisk.token.windows.WindowsKeystore;
import org.apache.commons.io.FilenameUtils;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Legacy key management migration handler
 */
public class LegacyDatabaseHandler extends DefaultHandler {

  // abstract product, windows + macos keychain
  private String certId;
  private String cert;
  private String keyAlias;

  // keystore
  private String url;

  // card
  private String atr;
  private String tokenLabel;
  private String tokenSerial;
  private String tokenManufacturer;
  private String apiParam;

  private StringBuilder sb;

  private final List<AbstractProduct> products = new ArrayList<>();

  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) {
    if ("certificateId".equals(qName)) {
      sb = new StringBuilder();
    }
    if ("certificate".equals(qName)) {
      sb = new StringBuilder();
    }
    if ("keyAlias".equals(qName)) {
      sb = new StringBuilder();
    }
    if ("url".equals(qName)) {
      sb = new StringBuilder();
    }
    if ("atr".equals(qName)) {
      sb = new StringBuilder();
    }
    if ("tokenLabel".equals(qName)) {
      sb = new StringBuilder();
    }
    if ("tokenSerial".equals(qName)) {
      sb = new StringBuilder();
    }
    if ("tokenManufacturer".equals(qName)) {
      sb = new StringBuilder();
    }
    if ("apiParam".equals(qName)) {
      sb = new StringBuilder();
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) {
    if ("certificateId".equals(qName)) {
      certId = sb.toString();
      sb = null;
    }
    if ("certificate".equals(qName)) {
      cert = sb.toString();
      sb = null;
    }
    if ("keyAlias".equals(qName)) {
      keyAlias = sb.toString();
      sb = null;
    }
    if ("url".equals(qName)) {
      url = sb.toString();
      sb = null;
    }
    if ("atr".equals(qName)) {
      atr = sb.toString();
      sb = null;
    }
    if ("tokenLabel".equals(qName)) {
      tokenLabel = sb.toString();
      sb = null;
    }
    if ("tokenSerial".equals(qName)) {
      tokenSerial = sb.toString();
      sb = null;
    }
    if ("tokenManufacturer".equals(qName)) {
      tokenManufacturer = sb.toString();
      sb = null;
    }
    if ("apiParam".equals(qName)) {
      apiParam = sb.toString();
      sb = null;
    }

    if ("winkeystore".equals(qName)) {
      WindowsKeystore winStore = new WindowsKeystore();
      winStore.setCertificate(cert);
      winStore.setKeyAlias(keyAlias);
      winStore.setCertificateId(certId);
      if (validateValues()) {
        products.add(winStore);
      }
    }
    if ("keystore".equals(qName)) {
      ConfiguredKeystore keystore = new ConfiguredKeystore();
      keystore.setCertificate(cert);
      keystore.setKeyAlias(keyAlias);
      keystore.setCertificateId(certId);
      if (validateValues() && url != null) {
        keystore.setUrl(url);
        KeystoreType type;
        String ext = FilenameUtils.getExtension(url.toLowerCase());
        switch (ext) {
          case "jks":
            type = KeystoreType.JKS;
            break;
          case "jceks":
            type = KeystoreType.JCEKS;
            break;
          case "p12":
          case "pfx":
          default:
            type = KeystoreType.PKCS12;
            break;
        }
        keystore.setType(type);
        products.add(keystore);
      }
    }
    if ("smartcard".equals(qName)) {
      DetectedCard card = new DetectedCard();
      card.setCertificate(cert);
      card.setKeyAlias(keyAlias);
      card.setCertificateId(certId);
      card.setAtr(atr);
      card.setTokenLabel(tokenLabel);
      card.setTokenSerial(tokenSerial);
      card.setTokenManufacturer(tokenManufacturer);
      ConnectionInfo connectionInfo = new ConnectionInfo();
      connectionInfo.setOs(OS.forOSName(System.getProperty("os.name")));
      connectionInfo.setSelectedApi(ScAPI.PKCS_11);
      connectionInfo.setApiParam(apiParam);
      card.setInfos(List.of(connectionInfo));
      if (validateValues() && atr != null &&
          tokenLabel != null  && tokenSerial != null  && tokenManufacturer != null && apiParam != null) {
        products.add(card);
      }
    }
    if ("keychain".equals(qName)) {
      MacOSKeychain keychain = new MacOSKeychain();
      keychain.setCertificate(cert);
      keychain.setKeyAlias(keyAlias);
      keychain.setCertificateId(certId);
      if (validateValues()) {
        products.add(keychain);
      }
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) {
    if (sb != null) {
      sb.append(ch, start, length);
    }
  }

  private boolean validateValues() {
    return  cert != null && keyAlias != null && certId != null;
  }

  public List<AbstractProduct> getProducts() {
    return products;
  }


}

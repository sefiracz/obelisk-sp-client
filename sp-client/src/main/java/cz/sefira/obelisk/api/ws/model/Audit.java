package cz.sefira.obelisk.api.ws.model;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.ws.model.Audit
 *
 * Created: 31.03.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AbstractProduct;
import cz.sefira.obelisk.generic.ConnectionInfo;
import cz.sefira.obelisk.token.keystore.ConfiguredKeystore;
import cz.sefira.obelisk.token.pkcs11.DetectedCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * description
 */
public class Audit {

  private static final Logger logger = LoggerFactory.getLogger(Audit.class.getName());

  private String ip;
  private String username;
  private String appDate;
  private UsedToken usedToken;

  public Audit(AbstractProduct usedProduct, Date initDate) {
    if (usedProduct != null) {
      String type = usedProduct.getType().getLabel();
      if (usedProduct instanceof DetectedCard) {
        DetectedCard card = (DetectedCard) usedProduct;
        ConnectionInfo infos = card.getConnectionInfo();
        String driver = infos != null ? infos.getApiParam() : "";
        String detail = card.getAtr()+" "+driver;
        this.usedToken = new UsedToken(type, detail);
      } else if (usedProduct instanceof ConfiguredKeystore) {
        this.usedToken = new UsedToken(type, ((ConfiguredKeystore) usedProduct).getUrl());
      } else {
        this.usedToken = new UsedToken(type, null);
      }
    }
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
    this.appDate = sdf.format(initDate);
    this.username = System.getProperty("user.name");
    try {
      this.ip = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      logger.error(e.getMessage(), e);
    }
  }

  public String getIp() {
    return ip;
  }

  public String getUsername() {
    return username;
  }

  public String getAppDate() {
    return appDate;
  }

  public Object getUsedToken() {
    return usedToken;
  }

  public static class UsedToken {

    private String type;
    private String detail;

    public UsedToken(String type, String detail) {
      this.type = type;
      this.detail = detail;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getDetail() {
      return detail;
    }

    public void setDetail(String detail) {
      this.detail = detail;
    }
  }
}

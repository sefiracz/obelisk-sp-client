package lu.nowina.nexu.object.model;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.object.model.AppLanguage
 *
 * Created: 14.01.2021
 * Author: hlavnicka
 */

import java.util.Locale;

public class AppLanguage {

  private String desc;
  private Locale locale;

  public AppLanguage(String desc, Locale locale) {
    this.desc = desc;
    this.locale = locale;
  }

  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  public Locale getLocale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  @Override
  public String toString() {
    return desc;
  }
}

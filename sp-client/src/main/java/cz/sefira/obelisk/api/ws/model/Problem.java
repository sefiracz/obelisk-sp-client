package cz.sefira.obelisk.api.ws.model;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.ws.model.Problem
 *
 * Created: 26.05.2023
 * Author: hlavnicka
 */

/**
 * SP-API problem
 */
public class Problem {

  private String type;
  private String title;
  private Integer status;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }
}

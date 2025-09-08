package cz.sefira.obelisk.api.plugin.webview.model;

public class CookieDefinition {

  private String domain;
  private String path;
  private Boolean secure;
  private Boolean httpOnly;

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Boolean getSecure() {
    return secure;
  }

  public void setSecure(Boolean secure) {
    this.secure = secure;
  }

  public Boolean getHttpOnly() {
    return httpOnly;
  }

  public void setHttpOnly(Boolean httpOnly) {
    this.httpOnly = httpOnly;
  }

}

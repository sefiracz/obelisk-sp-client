package cz.sefira.obelisk.api.ws;

public enum HttpMethod {

  GET("GET"),
  POST("POST");

  private final String method;

  HttpMethod(String method) {
    this.method = method;
  }

  public String getMethod() {
    return method;
  }

}

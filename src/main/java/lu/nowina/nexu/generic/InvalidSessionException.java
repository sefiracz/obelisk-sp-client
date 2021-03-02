package lu.nowina.nexu.generic;

public class InvalidSessionException extends Exception {

  public InvalidSessionException(String message) {
    super(message);
  }

  public InvalidSessionException(String message, Throwable cause) {
    super(message, cause);
  }

}

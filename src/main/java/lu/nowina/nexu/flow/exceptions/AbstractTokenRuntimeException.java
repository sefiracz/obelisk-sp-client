package lu.nowina.nexu.flow.exceptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AbstractTokenRuntimeException extends RuntimeException {

  private final String messageCode;
  private final List<String> messageParams = new ArrayList<>();

  public AbstractTokenRuntimeException(String message, String messageCode, String... params) {
    super(message);
    this.messageCode = messageCode;
    this.messageParams.addAll(Arrays.asList(params));
  }

  public AbstractTokenRuntimeException(String message, Throwable cause, String messageCode, String... params) {
    super(message, cause);
    this.messageCode = messageCode;
    this.messageParams.addAll(Arrays.asList(params));
  }

  public AbstractTokenRuntimeException(Throwable cause, String messageCode, String... params) {
    super(cause);
    this.messageCode = messageCode;
    this.messageParams.addAll(Arrays.asList(params));
  }

  public String getMessageCode() {
    return messageCode;
  }

  public String[] getMessageParams() {
    return messageParams.toArray(new String[0]);
  }
}

/**
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Licensed under EUPL Version 1.1 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package lu.nowina.nexu.flow.exceptions;

import lu.nowina.nexu.view.DialogMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AbstractTokenRuntimeException extends RuntimeException {

  private final DialogMessage.Level level;
  private final String messageCode;
  private final List<String> messageParams = new ArrayList<>();

  public AbstractTokenRuntimeException(String message, String messageCode, DialogMessage.Level level, String... params) {
    super(message);
    this.messageCode = messageCode;
    this.level = level;
    this.messageParams.addAll(Arrays.asList(params));
  }

  public AbstractTokenRuntimeException(String message, Throwable cause, String messageCode, DialogMessage.Level level, String... params) {
    super(message, cause);
    this.messageCode = messageCode;
    this.level = level;
    this.messageParams.addAll(Arrays.asList(params));
  }

  public AbstractTokenRuntimeException(Throwable cause, String messageCode, DialogMessage.Level level, String... params) {
    super(cause);
    this.messageCode = messageCode;
    this.level = level;
    this.messageParams.addAll(Arrays.asList(params));
  }

  public String getMessageCode() {
    return messageCode;
  }

  public String[] getMessageParams() {
    return messageParams.toArray(new String[0]);
  }

  public DialogMessage.Level getLevel() {
    return level;
  }
}

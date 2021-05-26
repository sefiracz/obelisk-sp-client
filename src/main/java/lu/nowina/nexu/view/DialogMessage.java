package lu.nowina.nexu.view;

import javafx.scene.control.Button;
import javafx.stage.Stage;
import lu.nowina.nexu.view.core.AbstractUIOperationController;

import javax.annotation.Nullable;
import javax.swing.plaf.basic.BasicOptionPaneUI;
import java.util.ArrayList;
import java.util.List;

public class DialogMessage {

  public enum Level {
    INFORMATION("message.title.information"),
    WARNING("message.title.warning"),
    ERROR("message.title.error"),
    TIMER("message.title.information"),
    NO_LEVEL("");

    private final String titleCode;

    Level(String titleCode) {
      this.titleCode = titleCode;
    }

    public String getTitleCode() {
      return titleCode;
    }
  }

  private final Level level;
  private final List<MessageButton> buttons = new ArrayList<>();

  private String dialogId;
  private String messageProperty;
  private String[] messageParameters = new String[0];
  private double width = 400;
  private double height = 150;
  private boolean doNotShowCheckbox = false;
  private boolean okButton = true;
  private long timerLength = 30; // 30s
  private String message;

  public DialogMessage(Level level) {
    this.level = level;
  }

  public DialogMessage(String messageProperty, Level level) {
    this.messageProperty = messageProperty;
    this.level = level;
  }

  public DialogMessage(String messageProperty, Level level, String[] messageParameters) {
    this.messageProperty = messageProperty;
    this.level = level;
    this.messageParameters = messageParameters;
  }

  public DialogMessage(String messageProperty, Level level, double width, double height) {
    this.messageProperty = messageProperty;
    this.level = level;
    this.width = width;
    this.height = height;
  }

  public DialogMessage(String messageProperty, Level level, String[] messageParameters, double width, double height) {
    this.messageProperty = messageProperty;
    this.level = level;
    this.messageParameters = messageParameters;
    this.width = width;
    this.height = height;
  }

  public Level getLevel() {
    return level;
  }

  public String getMessageProperty() {
    return messageProperty;
  }

  public Object[] getMessageParameters() {
    return messageParameters;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public void setWidth(double width) {
    this.width = width;
  }

  public void setHeight(double height) {
    this.height = height;
  }

  public double getWidth() {
    return width;
  }

  public double getHeight() {
    return height;
  }

  public long getTimerLength() {
    return timerLength;
  }

  public void setTimerLength(long seconds) {
    this.timerLength = seconds;
  }

  public boolean isShowDoNotShowCheckbox() {
    return doNotShowCheckbox;
  }

  public void setShowDoNotShowCheckbox(boolean doNotShowCheckbox, String dialogId) {
    if(dialogId == null || dialogId.isEmpty())
      throw new IllegalArgumentException("DialogID must have proper value");
    this.doNotShowCheckbox = doNotShowCheckbox;
    this.dialogId = dialogId;
  }

  public boolean isShowOkButton() {
    return okButton;
  }

  public void setShowOkButton(boolean okButton) {
    this.okButton = okButton;
  }

  public void addButton(MessageButton button) {
    buttons.add(button);
  }

  public List<MessageButton> getButtons() {
    return buttons;
  }

  public String getDialogId() {
    return dialogId;
  }

  public static class MessageButton {

    private final Button button;
    private final ButtonAction buttonAction;

    public MessageButton(Button button, ButtonAction buttonAction) {
      this.button = button;
      this.buttonAction = buttonAction;
    }

    public Button getButton() {
      return button;
    }

    public ButtonAction getButtonAction() {
      return buttonAction;
    }
  }

  @FunctionalInterface
  public interface ButtonAction {

    void action(@Nullable Stage dialogStage, @Nullable AbstractUIOperationController<?> controller);

  }

}

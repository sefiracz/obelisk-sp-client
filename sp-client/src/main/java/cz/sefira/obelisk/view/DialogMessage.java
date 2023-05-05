package cz.sefira.obelisk.view;

import cz.sefira.obelisk.view.core.AbstractUIOperationController;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class DialogMessage {

  public enum Level {
    SUCCESS("message.title.success"),
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
  private boolean doNotShowSelected = false;
  private boolean okButton = true;
  private long timerLength = 15; // 15s
  private String message;
  private Stage owner;

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

  public void setShowDoNotShowCheckbox(boolean doNotShowCheckbox, boolean doNotShowSelected, String dialogId) {
    if(dialogId == null || dialogId.isEmpty())
      throw new IllegalArgumentException("DialogID must have proper value");
    this.doNotShowCheckbox = doNotShowCheckbox;
    this.doNotShowSelected = doNotShowSelected;
    this.dialogId = dialogId;
  }

  public boolean isDoNotShowSelected() {
    return doNotShowSelected;
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

  public Stage getOwner() {
    return owner;
  }

  public void setOwner(Stage owner) {
    this.owner = owner;
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

    void action(Stage dialogStage, AbstractUIOperationController<?> controller);

  }

}

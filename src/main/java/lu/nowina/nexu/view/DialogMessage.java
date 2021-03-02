package lu.nowina.nexu.view;

public class DialogMessage {

  public enum Level {
    INFORMATION("message.title.information"),
    WARNING("message.title.warning"),
    ERROR("message.title.error");

    private final String titleCode;

    Level(String titleCode) {
      this.titleCode = titleCode;
    }

    public String getTitleCode() {
      return titleCode;
    }
  }

  private final Level level;
  private final String messageProperty;

  private String[] messageParameters = new String[0];
  private double width = 400;
  private double height = 150;

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

  public double getWidth() {
    return width;
  }

  public double getHeight() {
    return height;
  }

}

package cz.sefira.obelisk.api.plugin;

/**
 * When optional plugin cannot be initialized
 */
public class OptionalPluginException extends RuntimeException {

  public OptionalPluginException(String optionalPluginNotInitialized) {
    super(optionalPluginNotInitialized);
  }
}

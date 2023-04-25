/**
 * © Nowina Solutions, 2015-2015
 * © SEFIRA spol. s r.o., 2020-2021
 * <p>
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 * <p>
 * http://ec.europa.eu/idabc/eupl5
 * <p>
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package cz.sefira.obelisk.api.plugin;

/**
 * POJO that holds information about an event that occurred during the initialization of a {@link AppPlugin}.
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public class InitErrorMessage {

  private final String pluginName;
  private final String messageProperty;

  private final Throwable exception;

  public InitErrorMessage(String pluginName, String messageProperty, Throwable exception) {
    this.pluginName = pluginName;
    this.messageProperty = messageProperty;
    this.exception = exception;
  }

  public String getPluginName() {
    return pluginName;
  }

  public String getMessageProperty() {
    return messageProperty;
  }

  public Throwable getException() {
    return exception;
  }
}

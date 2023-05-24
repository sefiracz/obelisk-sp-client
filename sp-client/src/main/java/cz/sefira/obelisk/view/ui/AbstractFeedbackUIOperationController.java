/**
 * © Nowina Solutions, 2015-2015
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package cz.sefira.obelisk.view.ui;

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.model.Feedback;
import cz.sefira.obelisk.view.core.AbstractUIOperationController;
import cz.sefira.obelisk.api.PlatformAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Convenient base class for {@link AbstractUIOperationController} whose result is a feedback that can be provided to support.
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public abstract class AbstractFeedbackUIOperationController extends AbstractUIOperationController<Void> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractFeedbackUIOperationController.class);

	private Exception exception;
	private PlatformAPI api;

	private String applicationName;
	private AppConfig appConfig;

	@Override
	public final void init(Object... params) {
		try {
			exception = (Exception) params[0];
      api = (PlatformAPI) params[1];
      applicationName = AppConfig.get().getApplicationName();
      appConfig = AppConfig.get();
		} catch(final ClassCastException | ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("Expected parameters: Feedback, PlatformAPI");
		}

		if((exception == null) || (api == null)) {
			throw new IllegalArgumentException("Expected parameters: Feedback, PlatformAPI");
		}

		if(params.length > 5) {
			doInit(Arrays.copyOfRange(params, 5, params.length));
		} else {
			doInit((Object) null);
		}
	}

	/**
	 * Allows subclasses to use additional parameters or perform some specific initialization.
	 *
	 * <p>This implementation does nothing.
	 *
	 * @param params Additional parameters of the concrete controller.
	 */
	protected void doInit(Object... params) {
		// Do nothing by contract
	}

	public Exception getException() {
		return exception;
	}

	protected String getApplicationName() {
		return applicationName;
	}

	public AppConfig getAppConfig() {
		return appConfig;
	}

  public PlatformAPI getApi() {
    return api;
  }
}

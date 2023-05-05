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
package cz.sefira.obelisk.view.core;

import cz.sefira.obelisk.api.flow.OperationStatus;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Convenient base class for {@link UIOperationController}.
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public abstract class AbstractUIOperationController<R> extends ControllerCore implements UIOperationController<R> {

  private static final Logger logger = LoggerFactory.getLogger(AbstractUIOperationController.class.getName());

	private UIOperation<R> uiOperation;
	private UIDisplay display;

	public AbstractUIOperationController() {
		super();
	}

	@Override
	public final void setUIOperation(final UIOperation<R> uiOperation) {
		this.uiOperation = uiOperation;
	}

	@Override
	public final void setDisplay(UIDisplay display) {
		this.display = display;
	}

  public final void signalEnd(R result) {
		uiOperation.signalEnd(result);
	}

	/**
	 * Provides the flow alternative actions (other than next or cancel).
	 * @param operationStatus
	 * Status the flow will check before dispatching to an action.
	 */
  public final void signalEndWithStatus(final OperationStatus operationStatus) {
		uiOperation.signalEnd(operationStatus);
	}

  public final void signalUserCancel() {
		uiOperation.signalUserCancel();
	}

	/**
	 * This implementation does nothing.
	 */
	public void init(Object... params) {
		// Do nothing by contract
	}

	protected final UIDisplay getDisplay() {
		return display;
	}

  public void asyncUpdate(UpdateCallback callback) {
    asyncUpdate(uiOperation.getUpdateExecutorService(), callback);
  }

}

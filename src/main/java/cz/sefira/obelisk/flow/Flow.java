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
package cz.sefira.obelisk.flow;

import cz.sefira.obelisk.api.Execution;
import cz.sefira.obelisk.api.Feedback;
import cz.sefira.obelisk.api.NexuAPI;
import cz.sefira.obelisk.api.flow.Operation;
import cz.sefira.obelisk.api.flow.OperationFactory;
import cz.sefira.obelisk.view.core.UIDisplay;
import cz.sefira.obelisk.view.core.UIOperation;

/**
 * A flow is a sequence of {@link Operation}.
 * 
 * @author David Naramski
 */
public abstract class Flow<I, O> {

	// flag to check if user is currently in flow process execution
	public static volatile boolean IN_EXEC = false;

	private UIDisplay display;

	private OperationFactory operationFactory;

	private NexuAPI api;

	public Flow(UIDisplay display, NexuAPI api) {
		if (display == null) {
			throw new IllegalArgumentException("display cannot be null");
		}
		this.display = display;
		this.api = api;
	}

	public final void setOperationFactory(final OperationFactory operationFactory) {
		this.operationFactory = operationFactory;
	}

	protected final OperationFactory getOperationFactory() {
		return operationFactory;
	}

	public final Execution<O> execute(NexuAPI api, I input) throws Exception {
		try {
			IN_EXEC = true;
			final Execution<O> out = process(api, input);
			return out;
		} finally {
			IN_EXEC = false;
			display.close(true);
		}
	}

	protected abstract Execution<O> process(NexuAPI api, I input) throws Exception;

	protected final UIDisplay getDisplay() {
		return display;
	}

	@SuppressWarnings("unchecked")
	protected Exception handleException(final Exception e) {
    final Feedback feedback = new Feedback(e);
    getOperationFactory().getOperation(UIOperation.class, "/fxml/provide-feedback.fxml",
            new Object[] { feedback, api }).perform();
		return e;
	}
}

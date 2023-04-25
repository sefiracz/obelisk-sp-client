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
package cz.sefira.obelisk.api;

import cz.sefira.obelisk.api.flow.OperationResult;
import cz.sefira.obelisk.api.flow.OperationStatus;
import cz.sefira.obelisk.api.ws.model.Audit;
import cz.sefira.obelisk.util.TextUtils;

/**
 * 
 * @author david.naramski
 *
 * @param <T>
 */
public class Execution<T> {

	private Long stepId;

	private final boolean success;

	private final T result;

	private final String error;

	private final String errorMessage;

	private final String exception;

	private Audit audit;

	private transient OperationResult<?> operationResult;

	private transient AbstractProduct usedProduct;

	public Execution(T result) {
		this(result, null);
	}

	public Execution(T result, AbstractProduct usedProduct) {
		this.result = result;
		this.success = true;
		this.error = null;
		this.errorMessage = null;
		this.exception = null;
		this.usedProduct = usedProduct;
	}

	public Execution(final OperationStatus errorOperationStatus) {
		this(null, errorOperationStatus);
	}

	public Execution(final OperationResult<?> operationResult, final OperationStatus errorOperationStatus) {
		this(operationResult, errorOperationStatus, null);
	}

	public Execution(final OperationStatus errorOperationStatus, final Exception error) {
		this(null, errorOperationStatus, error);
	}

	public Execution(final OperationResult<?> operationResult, final OperationStatus errorOperationStatus,
									 final Exception error) {
		this.success = false;
		this.error = errorOperationStatus.getCode();
		// errorMessage
		if (operationResult != null && operationResult.getMessage() != null) {
			this.errorMessage = operationResult.getMessage();
		}
		else {
			this.errorMessage = errorOperationStatus.getLabel();
		}
		// exception
		if (error != null) {
			this.exception = TextUtils.printException(error);
		}
		else if (operationResult != null && operationResult.getException() != null) {
			this.exception = TextUtils.printException(operationResult.getException());
		}
		else {
			this.exception = null;
		}
		this.result = null;
		this.operationResult = operationResult;
	}

	public T getResult() {
		return result;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getError() {
		return error;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public String getException() {
		return exception;
	}

	public AbstractProduct getUsedProduct() {
		return usedProduct;
	}

	public Audit getAudit() {
		return audit;
	}

	public void setAudit(Audit audit) {
		this.audit = audit;
	}

	public Long getStepId() {
		return stepId;
	}

	public void setStepId(Long stepId) {
		this.stepId = stepId;
	}

	public OperationResult<?> getOperationResult() {
		return operationResult;
	}
}

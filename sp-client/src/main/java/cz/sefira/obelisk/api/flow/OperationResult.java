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
package cz.sefira.obelisk.api.flow;

public class OperationResult<R> {

	private R result;

	private final OperationStatus status;

	private Exception exception;

	private String message;

	public OperationResult(R result) {
		this.status = BasicOperationStatus.SUCCESS;
		this.result = result;
	}

	public OperationResult(Exception e) {
		this.status = BasicOperationStatus.EXCEPTION;
		this.exception = e;
	}

	public OperationResult(OperationStatus status) {
		this.status = status;
	}

  public OperationResult(OperationStatus status, String message) {
    this.status = status;
    this.message = message;
  }

	public Exception getException() {
		return exception;
	}

	public R getResult() {
		return result;
	}

	public OperationStatus getStatus() {
		return status;
	}

  public String getMessage() {
    return message;
  }
}

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
package cz.sefira.obelisk.api.ws.model;

public class SignatureRequest extends BaseRequest {

  private SignParameters signParams;

  public SignatureRequest() {
    super();
  }

  public SignatureRequest(final SignParameters signParams) {
    super();
    this.signParams = signParams;
  }

  public SignParameters getSignParams() {
    return signParams;
  }

  public void setSignParams(SignParameters signParams) {
    this.signParams = signParams;
  }
}

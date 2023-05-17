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
package cz.sefira.obelisk.flow.operation;

import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.api.flow.BasicOperationStatus;
import cz.sefira.obelisk.api.flow.OperationResult;
import cz.sefira.obelisk.api.model.ScAPI;
import cz.sefira.obelisk.flow.exceptions.AbstractTokenRuntimeException;
import cz.sefira.obelisk.generic.ConnectionInfo;
import cz.sefira.obelisk.generic.GenericCardAdapter;
import cz.sefira.obelisk.model.Pkcs11Params;
import cz.sefira.obelisk.token.pkcs11.DetectedCard;
import cz.sefira.obelisk.view.core.UIOperation;
import cz.sefira.obelisk.dss.token.SignatureTokenConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This {@link CompositeOperation} allows to create a {@link SignatureTokenConnection}.
 *
 * <p>Expected parameters:
 * <ol>
 * <li>{@link PlatformAPI}</li>
 * <li>List of {@link Match}</li>
 * </ol>
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public class CreateTokenOperation extends AbstractCompositeOperation<Map<TokenOperationResultKey, Object>> {

    private static final Logger LOG = LoggerFactory.getLogger(CreateTokenOperation.class.getName());

    private PlatformAPI api;
    private List<Match> matchingProductAdapters;
    private AbstractProduct product;

    public CreateTokenOperation() {
        super();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setParams(final Object... params) {
        try {
            this.api = (PlatformAPI) params[0];
            this.matchingProductAdapters = (List<Match>) params[1];
            this.product = (AbstractProduct) params[2];
        } catch(final ArrayIndexOutOfBoundsException | ClassCastException e) {
            throw new IllegalArgumentException("Expected parameters: NexuAPI, List of Match");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public OperationResult<Map<TokenOperationResultKey, Object>> perform() {
      LOG.info(this.matchingProductAdapters.size() + " matching product adapters");
      try {
        if (!this.matchingProductAdapters.isEmpty() && availableConfiguration(matchingProductAdapters.get(0))) {
          return this.createTokenAuto(); // automatic token configuration
        } else {
          // unavailable/unsupported configuration - want to continue?
          OperationResult<Object> result;
          if(product instanceof DetectedCard && ((DetectedCard) product).isKnownToken() != null) {
            result = this.operationFactory.getOperation(UIOperation.class, "/fxml/unavailable-config.fxml",
                    ((DetectedCard) product).isKnownToken()).perform();
          } else {
            result = this.operationFactory.getOperation(UIOperation.class, "/fxml/unsupported-product.fxml",
                            new Object[]{AppConfig.get().getApplicationName()}).perform();
          }
          // advanced config?
          if (result.getStatus().equals(BasicOperationStatus.SUCCESS)) {
            return this.createTokenAdvanced(); // manual token configuration
          } else if (result.getStatus().equals(CoreOperationStatus.BACK)) {
            return new OperationResult<>(CoreOperationStatus.BACK); // go back
          } else {
            throw new IllegalStateException("Unexpected result status: "+result.getStatus().getCode());
          }
        }
      } catch (AbstractTokenRuntimeException e) {
        this.operationFactory.getMessageDialog(api, e.getDialogMessage(), true);
        return new OperationResult<>(CoreOperationStatus.NO_TOKEN);
      } catch (Exception e) {
        throw e;
      }
    }

  /**
   * Automatically creates token adapter
   * @return Operation returns Map with initialized token
   */
  private OperationResult<Map<TokenOperationResultKey, Object>> createTokenAuto() {
    final Match match = this.matchingProductAdapters.get(0);
    final Product supportedProduct = match.getProduct();
    final ProductAdapter adapter = match.getAdapter();

    final Map<TokenOperationResultKey, Object> map = new HashMap<>();
    map.put(TokenOperationResultKey.SELECTED_API, match.getScAPI());
    map.put(TokenOperationResultKey.SELECTED_API_PARAMS, match.getApiParameters());

    // create token
    return createToken(supportedProduct, adapter, map);
  }

  /**
   * Check that found match is actually viable to use for PKCS11 (that all drivers are still available)
   * @param match Matched product
   * @return True if usable, or false if it needs to be configured again
   */
  private boolean availableConfiguration(Match match) {
    if (match.getScAPI() != null && ScAPI.PKCS_11.equals(match.getScAPI())) {
      String driverPath = match.getApiParameters();
      return driverPath != null && !driverPath.isEmpty() &&
          new File(driverPath).exists() && new File(driverPath).canRead();
    }
    return true;
  }

  /**
   * Creates token adapter given user input/configuration
   * @return Operation returns Map with initialized token
   */
  @SuppressWarnings("unchecked")
  private OperationResult<Map<TokenOperationResultKey, Object>> createTokenAdvanced(){
    final Map<TokenOperationResultKey, Object> map = new HashMap<>();
    map.put(TokenOperationResultKey.ADVANCED_CREATION, true);
    map.put(TokenOperationResultKey.SELECTED_PRODUCT, product);

    // PKCS#11
    ScAPI scAPI = ScAPI.PKCS_11;
    map.put(TokenOperationResultKey.SELECTED_API, scAPI);
    final OperationResult<Object> driverSelection = operationFactory.getOperation(UIOperation.class,
        "/fxml/pkcs11-params.fxml").perform();
    if(driverSelection.getStatus().equals(BasicOperationStatus.USER_CANCEL)) {
      return new OperationResult<>(BasicOperationStatus.USER_CANCEL);
    }
    final Pkcs11Params pkcs11Params = (Pkcs11Params) driverSelection.getResult();
    final String absolutePath = pkcs11Params.getPkcs11Lib().getAbsolutePath();
    map.put(TokenOperationResultKey.SELECTED_API_PARAMS, absolutePath);

    // prepare smartcard information and generic card adapter
    final ConnectionInfo connectionInfo = new ConnectionInfo();
    connectionInfo.setApiParam((String) map.get(TokenOperationResultKey.SELECTED_API_PARAMS));
    connectionInfo.setSelectedApi(scAPI);
    connectionInfo.setOs(this.api.getEnvironmentInfo().getOs());
    DetectedCard card = (DetectedCard) product;
    card.setConnectionInfo(connectionInfo);
    final GenericCardAdapter adapter = new GenericCardAdapter(card, api);

    // create token
    return createToken(product, adapter, map);
  }

  /**
   * Creates token adapter
   *
   * @param product Product Product that describes token connection information
   * @param adapter Adapter that will be used to connect to token
   * @param map Map to fill with token parameters and returned
   * @return Operation returns Map with initialized token and parameters
   */
  private OperationResult<Map<TokenOperationResultKey, Object>> createToken(Product product, ProductAdapter adapter,
                                                                            Map<TokenOperationResultKey, Object> map) {
    final SignatureTokenConnection connect = adapter.connect(api, product, display.getPasswordInputCallback(product));
    if (connect == null) {
      LOG.error("No connect returned");
      return new OperationResult<>(CoreOperationStatus.NO_TOKEN);
    }

    map.put(TokenOperationResultKey.TOKEN, connect);
    map.put(TokenOperationResultKey.ADVANCED_CREATION, false);
    map.put(TokenOperationResultKey.SELECTED_PRODUCT, product);
    map.put(TokenOperationResultKey.SELECTED_PRODUCT_ADAPTER, adapter);

    return new OperationResult<>(map);
  }
}

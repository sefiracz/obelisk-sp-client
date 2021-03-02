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
package lu.nowina.nexu.flow.operation;

import eu.europa.esig.dss.token.SignatureTokenConnection;
import lu.nowina.nexu.api.*;
import lu.nowina.nexu.api.flow.BasicOperationStatus;
import lu.nowina.nexu.api.flow.OperationResult;
import lu.nowina.nexu.flow.exceptions.AbstractTokenRuntimeException;
import lu.nowina.nexu.generic.*;
import lu.nowina.nexu.model.Pkcs11Params;
import lu.nowina.nexu.view.DialogMessage;
import lu.nowina.nexu.view.core.UIOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This {@link CompositeOperation} allows to create a {@link TokenId}.
 *
 * <p>Expected parameters:
 * <ol>
 * <li>{@link NexuAPI}</li>
 * <li>List of {@link Match}</li>
 * </ol>
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public class CreateTokenOperation extends AbstractCompositeOperation<Map<TokenOperationResultKey, Object>> {

    private static final Logger LOG = LoggerFactory.getLogger(CreateTokenOperation.class.getName());

    private NexuAPI api;
    private List<Match> matchingProductAdapters;
    private AbstractProduct product;

    public CreateTokenOperation() {
        super();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setParams(final Object... params) {
        try {
            this.api = (NexuAPI) params[0];
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
        if (!this.matchingProductAdapters.isEmpty()) {
          return this.createTokenAuto(); // automatic token configuration
        } else {
          // unavailable/unsupported configuration - want to continue?
          OperationResult<Object> result;
          if(product instanceof DetectedCard && ((DetectedCard) product).isKnownToken() != null) {
            result = this.operationFactory.getOperation(UIOperation.class, "/fxml/unavailable-config.fxml",
                    new Object[]{this.api.getAppConfig().getApplicationName(),
                            ((DetectedCard) product).isKnownToken()}).perform();
          } else {
            result = this.operationFactory.getOperation(UIOperation.class, "/fxml/unsupported-product.fxml",
                            new Object[]{this.api.getAppConfig().getApplicationName()}).perform();
          }
          // advanced config?
          if (result.getStatus().equals(BasicOperationStatus.SUCCESS)) {
            return this.createTokenAdvanced(); // manual token configuration
          } else if (result.getStatus().equals(CoreOperationStatus.BACK)) {
            return new OperationResult<>(CoreOperationStatus.BACK); // go back
          } else {
            // TODO remove ???
            this.operationFactory.getMessageDialog(api, new DialogMessage("unsupported.product.message",
                    DialogMessage.Level.ERROR, 400, 150), true);
            return new OperationResult<>(CoreOperationStatus.UNSUPPORTED_PRODUCT);
          }
        }
      } catch (AbstractTokenRuntimeException e) {
        this.operationFactory.getMessageDialog(api, new DialogMessage(e.getMessageCode(), e.getLevel(),
                e.getMessageParams()), true);
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
   * Creates token adapter given user input/configuration
   * @return Operation returns Map with initialized token
   */
  @SuppressWarnings("unchecked")
  private OperationResult<Map<TokenOperationResultKey, Object>> createTokenAdvanced(){
    final Map<TokenOperationResultKey, Object> map = new HashMap<>();
    map.put(TokenOperationResultKey.ADVANCED_CREATION, true);
    map.put(TokenOperationResultKey.SELECTED_PRODUCT, product);

    // select token API - TODO - only when it makes sense and MSCAPI is usable
    ScAPI scAPI = ScAPI.PKCS_11;
    /*
    final OperationResult<Object> apiSelection = this.operationFactory.getOperation(UIOperation.class,
            "/fxml/api-selection.fxml", this.api.getAppConfig().getApplicationName()).perform();
    if(apiSelection.getStatus().equals(BasicOperationStatus.USER_CANCEL)) {
      return new OperationResult<>(BasicOperationStatus.USER_CANCEL);
    }
    scAPI = (ScAPI) apiSelection.getResult();
    */
    map.put(TokenOperationResultKey.SELECTED_API, scAPI);

    // select PKCS11 parameters
    if(scAPI.equals(ScAPI.PKCS_11)) {
      final OperationResult<Object> op2 = operationFactory.getOperation(UIOperation.class,
              "/fxml/pkcs11-params.fxml", this.api.getAppConfig().getApplicationName()).perform();
      if(op2.getStatus().equals(BasicOperationStatus.USER_CANCEL)) {
        return new OperationResult<>(BasicOperationStatus.USER_CANCEL);
      }
      final Pkcs11Params pkcs11Params = (Pkcs11Params) op2.getResult();
      final String absolutePath = pkcs11Params.getPkcs11Lib().getAbsolutePath();
      map.put(TokenOperationResultKey.SELECTED_API_PARAMS, absolutePath);
    }

    // prepare smartcard information and generic card adapter
    final ConnectionInfo connectionInfo = new ConnectionInfo();
    connectionInfo.setApiParam((String) map.get(TokenOperationResultKey.SELECTED_API_PARAMS));
    connectionInfo.setSelectedApi(scAPI);
    connectionInfo.setEnv(this.api.getEnvironmentInfo());
    final SCInfo info = new SCInfo((DetectedCard) product);
    info.getInfos().add(connectionInfo);

    final GenericCardAdapter adapter = new GenericCardAdapter(info, api);

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
    final SignatureTokenConnection connect;
    if(adapter.supportMessageDisplayCallback(product)) {
      connect = adapter.connect(api, product, display.getPasswordInputCallback(product), display.getMessageDisplayCallback());
    } else {
      connect = adapter.connect(api, product, display.getPasswordInputCallback(product));
    }
    if (connect == null) {
      LOG.error("No connect returned");
      return new OperationResult<>(CoreOperationStatus.NO_TOKEN);
    }
    final TokenId tokenId = api.registerTokenConnection(connect);
    if (tokenId == null) {
      LOG.error("Received null TokenId after registration");
      return new OperationResult<>(CoreOperationStatus.NO_TOKEN_ID);
    }

    map.put(TokenOperationResultKey.TOKEN_ID, tokenId);
    map.put(TokenOperationResultKey.ADVANCED_CREATION, false);
    map.put(TokenOperationResultKey.SELECTED_PRODUCT, product);
    map.put(TokenOperationResultKey.SELECTED_PRODUCT_ADAPTER, adapter);

    return new OperationResult<>(map);
  }
}

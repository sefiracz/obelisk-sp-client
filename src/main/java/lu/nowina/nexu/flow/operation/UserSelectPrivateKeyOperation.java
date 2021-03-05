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

import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import lu.nowina.nexu.CancelledOperationException;
import lu.nowina.nexu.Utils;
import lu.nowina.nexu.api.CertificateFilter;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.Product;
import lu.nowina.nexu.api.ProductAdapter;
import lu.nowina.nexu.api.flow.BasicOperationStatus;
import lu.nowina.nexu.api.flow.OperationResult;
import lu.nowina.nexu.flow.exceptions.*;
import lu.nowina.nexu.view.DialogMessage;
import lu.nowina.nexu.view.core.UIOperation;

import java.util.List;

/**
 * This {@link CompositeOperation} allows user to manually retrieve a private key from
 * a given {@link SignatureTokenConnection} and using <code>certificate filter</code> to remove
 * unwanted items from selection.
 *
 * Expected parameters:
 * <ol>
 * <li>{@link SignatureTokenConnection}</li>
 * <li>{@link NexuAPI}</li>
 * <li>{@link Product} (optional)</li>
 * <li>{@link ProductAdapter} (optional)</li>
 * <li>{@link CertificateFilter} (optional)</li>
 * </ol>
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
// TODO - make this into Controller? / move this into key-selection.fxml controller
public class UserSelectPrivateKeyOperation extends AbstractCompositeOperation<DSSPrivateKeyEntry> {

    private SignatureTokenConnection token;
    private NexuAPI api;
    private Product product;
    private ProductAdapter productAdapter;
    private CertificateFilter certificateFilter;

    public UserSelectPrivateKeyOperation() {
        super();
    }

    @Override
    public void setParams(final Object... params) {
        try {
            this.token = (SignatureTokenConnection) params[0];
            this.api = (NexuAPI) params[1];
            this.product = (Product) params[2];
            this.productAdapter = (ProductAdapter) params[3];
            this.certificateFilter = (CertificateFilter) params[4];
        } catch(final ClassCastException | ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Expected parameters: SignatureTokenConnection, NexuAPI, Product, ProductAdapter, CertificateFilter");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public OperationResult<DSSPrivateKeyEntry> perform() {
        final List<DSSPrivateKeyEntry> keys;

        try {
          // TODO - heavy load - move to key-selection and do it async ?
            keys = this.productAdapter.getKeys(this.token, this.certificateFilter);
        } catch (final CancelledOperationException e) {
          return new OperationResult<DSSPrivateKeyEntry>(BasicOperationStatus.USER_CANCEL);
        } catch (AbstractTokenRuntimeException e) {
          this.operationFactory.getMessageDialog(api, new DialogMessage(e.getMessageCode(), e.getLevel(),
                  e.getMessageParams()), true);
          return new OperationResult<>(CoreOperationStatus.BACK);
        } catch (Exception e) {
            if(Utils.checkWrongPasswordInput(e, operationFactory, api))
                throw e;
            return new OperationResult<DSSPrivateKeyEntry>(CoreOperationStatus.BACK);
        }

        DSSPrivateKeyEntry key = null;

        // Microsoft keystore
        keys.removeIf(k -> "CN=Token Signing Public Key".equals(k.getCertificate().getCertificate().getIssuerDN().getName()));

        // let user select private key
        final OperationResult<Object> op =
            this.operationFactory.getOperation(UIOperation.class, "/fxml/key-selection.fxml",
                new Object[]{keys, this.api.getAppConfig().getApplicationName()}).perform();
        if(op.getStatus().equals(CoreOperationStatus.BACK)) {
            return new OperationResult<DSSPrivateKeyEntry>(CoreOperationStatus.BACK);
        }
        if(op.getStatus().equals(BasicOperationStatus.USER_CANCEL)) {
            return new OperationResult<DSSPrivateKeyEntry>(BasicOperationStatus.USER_CANCEL);
        }
        // get selected key
        key = (DSSPrivateKeyEntry) op.getResult();
        if(key == null) {
            return new OperationResult<DSSPrivateKeyEntry>(CoreOperationStatus.NO_KEY_SELECTED);
        }
        return new OperationResult<DSSPrivateKeyEntry>(key);
    }


}

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

import cz.sefira.obelisk.CancelledOperationException;
import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.ws.model.CertificateFilter;
import cz.sefira.obelisk.api.Product;
import cz.sefira.obelisk.api.ProductAdapter;
import cz.sefira.obelisk.api.flow.BasicOperationStatus;
import cz.sefira.obelisk.flow.exceptions.AbstractTokenRuntimeException;
import cz.sefira.obelisk.view.BusyIndicator;
import cz.sefira.obelisk.dss.token.DSSPrivateKeyEntry;
import cz.sefira.obelisk.dss.token.SignatureTokenConnection;
import cz.sefira.obelisk.util.DSSUtils;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.flow.OperationResult;
import cz.sefira.obelisk.view.core.UIOperation;

import java.util.List;

/**
 * This {@link CompositeOperation} allows user to manually retrieve a private key from
 * a given {@link SignatureTokenConnection} and using <code>certificate filter</code> to remove
 * unwanted items from selection.
 *
 * Expected parameters:
 * <ol>
 * <li>{@link SignatureTokenConnection}</li>
 * <li>{@link PlatformAPI}</li>
 * <li>{@link ProductAdapter} (optional)</li>
 * <li>{@link CertificateFilter} (optional)</li>
 * </ol>
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public class UserSelectPrivateKeyOperation extends AbstractCompositeOperation<DSSPrivateKeyEntry> {

    private SignatureTokenConnection token;
    private PlatformAPI api;
    private ProductAdapter productAdapter;
    private CertificateFilter certificateFilter;
    private boolean userInteraction = true;

    public UserSelectPrivateKeyOperation() {
        super();
    }

    @Override
    public void setParams(final Object... params) {
        try {
            this.token = (SignatureTokenConnection) params[0];
            this.api = (PlatformAPI) params[1];
            this.productAdapter = (ProductAdapter) params[2];
            this.certificateFilter = (CertificateFilter) params[3];
            this.userInteraction = (boolean) params[4];
        } catch(final ClassCastException | ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Expected parameters: SignatureTokenConnection, PlatformAPI, ProductAdapter, CertificateFilter, UserInteraction");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public OperationResult<DSSPrivateKeyEntry> perform() {
        final List<DSSPrivateKeyEntry> keys;
        try (BusyIndicator busyIndicator = new BusyIndicator()){
            keys = this.productAdapter.getKeys(this.token, this.certificateFilter);
        } catch (final CancelledOperationException e) {
          return new OperationResult<>(BasicOperationStatus.USER_CANCEL);
        } catch (AbstractTokenRuntimeException e) {
          this.operationFactory.getMessageDialog(api, e.getDialogMessage(), true);
          return new OperationResult<>(CoreOperationStatus.BACK);
        } catch (Exception e) {
            if(!DSSUtils.checkWrongPasswordInput(e, operationFactory, api))
                throw e;
            return new OperationResult<>(CoreOperationStatus.BACK);
        }


        // Microsoft keystore
        keys.removeIf(k -> "CN=Token Signing Public Key".equals(k.getCertificateToken().getCertificate().getIssuerDN().getName()));

        DSSPrivateKeyEntry key = null;
        if (userInteraction) {
            // let user select private key
            final OperationResult<Object> op =
                this.operationFactory.getOperation(UIOperation.class, "/fxml/key-selection.fxml", keys, certificateFilter.getCertificateId() != null).perform();
            if(op.getStatus().equals(CoreOperationStatus.BACK)) {
                return new OperationResult<>(CoreOperationStatus.BACK);
            }
            if(op.getStatus().equals(BasicOperationStatus.USER_CANCEL)) {
                return new OperationResult<>(BasicOperationStatus.USER_CANCEL);
            }
            // get selected key
            key = (DSSPrivateKeyEntry) op.getResult();
        } else {
            //auto select best private key
            if (keys.size() == 1) {
                key = keys.get(0);
            } else if (keys.size() > 1) {
                for (DSSPrivateKeyEntry usable : keys) {
                    if (key == null) {
                        key = usable;
                    } else if (key.getCertificateToken().getNotBefore().before(usable.getCertificateToken().getNotBefore())) {
                        key = usable;
                    }
                }
            }
        }

        if(key == null) {
            return new OperationResult<>(CoreOperationStatus.NO_KEY_SELECTED);
        }
        return new OperationResult<>(key);
    }


}

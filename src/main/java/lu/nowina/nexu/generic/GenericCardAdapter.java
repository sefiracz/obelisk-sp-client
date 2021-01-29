/**
 * © Nowina Solutions, 2015-2015
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
package lu.nowina.nexu.generic;

import eu.europa.esig.dss.DigestAlgorithm;
import eu.europa.esig.dss.token.*;
import eu.europa.esig.dss.token.mocca.MOCCASignatureTokenConnection;
import lu.nowina.nexu.ProductDatabaseLoader;
import lu.nowina.nexu.Utils;
import lu.nowina.nexu.api.*;
import lu.nowina.nexu.flow.operation.TokenOperationResultKey;

import java.io.File;
import java.util.List;
import java.util.Map;

public class GenericCardAdapter extends AbstractCardProductAdapter {

    private final SCInfo info;
    private final NexuAPI api;

    public GenericCardAdapter(final SCInfo info, NexuAPI api, File nexuHome) {
        super(nexuHome);
        this.info = info;
        this.api = api;
    }

    @Override
    protected boolean accept(final DetectedCard card) {
        return this.info.getAtr().equals(card.getAtr());
    }

    @Override
    protected String getLabel(final NexuAPI api, final DetectedCard card, final PasswordInputCallback callback) {
        return card.getLabel();
    }

    @Override
    protected String getLabel(final NexuAPI api, final DetectedCard card, final PasswordInputCallback callback, final MessageDisplayCallback messageCallback) {
        throw new IllegalStateException("This product adapter does not support message display callback.");
    }

    @Override
    protected boolean supportMessageDisplayCallback(final DetectedCard card) {
        return false;
    }

    @Override
    protected SignatureTokenConnection connect(final NexuAPI api, final DetectedCard card, final PasswordInputCallback callback) {
        if(callback instanceof NexuPasswordInputCallback) {
            ((NexuPasswordInputCallback) callback).setProduct(card);
        }
        final ConnectionInfo cInfo = this.info.getConnectionInfo(api.getEnvironmentInfo());
        final ScAPI scApi = cInfo.getSelectedApi();
        switch (scApi) {
            case MSCAPI:
                // Cannot intercept cancel and timeout for MSCAPI (too generic error).
                return new MSCAPISignatureToken();
            case PKCS_11:
                final String absolutePath = cInfo.getApiParam();
                Utils.checkSlotIndex(api, card);
                return Utils.getStoredPkcs11TokenAdapter(card, absolutePath, callback);
            case MOCCA:
                return new MOCCASignatureTokenConnectionAdapter(new MOCCASignatureTokenConnection(callback), api, card);
            default:
                throw new RuntimeException("API not supported");
        }
    }

    @Override
    protected SignatureTokenConnection connect(final NexuAPI api, final DetectedCard card, final PasswordInputCallback callback,
            final MessageDisplayCallback messageCallback) {
        throw new IllegalStateException("This product adapter does not support message display callback.");
    }

    @Override
    protected boolean canReturnIdentityInfo(final DetectedCard card) {
        return false;
    }

    @Override
    public GetIdentityInfoResponse getIdentityInfo(final SignatureTokenConnection token) {
        throw new IllegalStateException("This card adapter cannot return identity information.");
    }

    @Override
    protected boolean supportCertificateFilter(final DetectedCard card) {
        return true;
    }

    @Override
    protected boolean canReturnSuportedDigestAlgorithms(final DetectedCard card) {
        return false;
    }

    @Override
    protected List<DigestAlgorithm> getSupportedDigestAlgorithms(final DetectedCard card) {
        throw new IllegalStateException("This card adapter cannot return list of supported digest algorithms.");
    }

    @Override
    protected DigestAlgorithm getPreferredDigestAlgorithm(final DetectedCard card) {
        throw new IllegalStateException("This card adapter cannot return list of supported digest algorithms.");
    }

    @Override
    public List<DSSPrivateKeyEntry> getKeys(final SignatureTokenConnection token, final CertificateFilter certificateFilter) {
        return new CertificateFilterHelper().filterKeys(token, certificateFilter);
    }

    @Override
    public DSSPrivateKeyEntry getKey(SignatureTokenConnection token, String keyAlias) {
        // TODO - MOCCAPrivateKeyEntry ?
        List<DSSPrivateKeyEntry> keys = token.getKeys();
        for(DSSPrivateKeyEntry key : keys) {
            if(key instanceof IAIKPrivateKeyEntry && ((IAIKPrivateKeyEntry) key).getKeyLabel().equalsIgnoreCase(keyAlias)) {
                return key;
            }
            if(key instanceof KSPrivateKeyEntry && ((KSPrivateKeyEntry) key).getAlias().equalsIgnoreCase(keyAlias)) {
                return key;
            }
        }
        return null;
    }

    public SCDatabase getDatabase() {
        return ProductDatabaseLoader.load(SCDatabase.class, new File(nexuHome, "database-smartcard.xml"));
    }

    private void saveKeystore(final DetectedCard keystore, Map<TokenOperationResultKey, Object> map) {
      String apiParam = (String) map.get(TokenOperationResultKey.SELECTED_API_PARAMS);
      ScAPI selectedApi = (ScAPI) map.get(TokenOperationResultKey.SELECTED_API);
      if(selectedApi.equals(ScAPI.MSCAPI)) {
        keystore.setType(KeystoreType.WINDOWS);
      }
      EnvironmentInfo env = api.getEnvironmentInfo();
      ConnectionInfo cInfo = new ConnectionInfo();
      cInfo.setSelectedApi(selectedApi);
      cInfo.setEnv(env);
      cInfo.setApiParam(apiParam);
      getDatabase().add(keystore, cInfo);
    }

    @Override
    public void saveKeystore(AbstractProduct keystore, Map<TokenOperationResultKey, Object> map) {
      saveKeystore((DetectedCard) keystore, map);
    }
}

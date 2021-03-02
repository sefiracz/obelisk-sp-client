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
package lu.nowina.nexu.generic;

import eu.europa.esig.dss.DigestAlgorithm;
import eu.europa.esig.dss.token.*;
import eu.europa.esig.dss.token.mocca.MOCCAPrivateKeyEntry;
import eu.europa.esig.dss.token.mocca.MOCCASignatureTokenConnection;
import lu.nowina.nexu.api.*;
import lu.nowina.nexu.flow.operation.TokenOperationResultKey;
import lu.nowina.nexu.pkcs11.IAIKPrivateKeyEntry;
import lu.nowina.nexu.pkcs11.IAIKPkcs11SignatureTokenAdapter;
import lu.nowina.nexu.flow.exceptions.PKCS11TokenException;

import javax.smartcardio.CardException;
import java.io.File;
import java.util.List;
import java.util.Map;

public class GenericCardAdapter extends AbstractCardProductAdapter {

    private final SCInfo info;
    private final NexuAPI api;

    public GenericCardAdapter(final SCInfo info, NexuAPI api) {
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
        final ConnectionInfo cInfo = this.info.getConnectionInfo(api.getEnvironmentInfo());
        final ScAPI scApi = cInfo.getSelectedApi();
        switch (scApi) {
            case MSCAPI:
                // Cannot intercept cancel and timeout for MSCAPI (too generic error).
                return new MSCAPISignatureToken(); // TODO ? filter the token content
            case PKCS_11:
              try {
                final String absolutePath = cInfo.getApiParam();
                // get present card
                DetectedCard detectedCard = api.getPresentCard(card);
                SignatureTokenConnection token = SessionManager.getManager().getInitializedTokenForProduct(detectedCard);
                if(token == null) {
                  token = new IAIKPkcs11SignatureTokenAdapter(api, new File(absolutePath), callback, detectedCard);
                }
                SessionManager.getManager().setToken(detectedCard, token);
                return token;
              } catch (CardException e) {
                throw new PKCS11TokenException("Token not present or unable to connect", e);
              }
            case MOCCA:
              // TODO - remove ?
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
        List<DSSPrivateKeyEntry> keys = token.getKeys();
        for(DSSPrivateKeyEntry key : keys) {
            if(key instanceof IAIKPrivateKeyEntry && ((IAIKPrivateKeyEntry) key).getKeyLabel().equalsIgnoreCase(keyAlias)) {
                return key;
            }
            if(key instanceof KSPrivateKeyEntry && ((KSPrivateKeyEntry) key).getAlias().equalsIgnoreCase(keyAlias)) {
                return key;
            }
            if(key instanceof MOCCAPrivateKeyEntry) {
              throw new UnsupportedOperationException("MOCCA not supported");
            }
        }
        return null;
    }

  @Override
  public SystrayMenuItem getExtensionSystrayMenuItem(NexuAPI api) {
    return null;
  }

  @Override
  public void saveProduct(AbstractProduct product, Map<TokenOperationResultKey, Object> map) {
    saveSmartcard((DetectedCard) product, map);
  }

  public SCDatabase getProductDatabase() {
    return api.loadDatabase(SCDatabase.class, "database-smartcard.xml");
  }

  private void saveSmartcard(final DetectedCard card, Map<TokenOperationResultKey, Object> map) {
    String apiParam = (String) map.get(TokenOperationResultKey.SELECTED_API_PARAMS);
    ScAPI selectedApi = (ScAPI) map.get(TokenOperationResultKey.SELECTED_API);
    EnvironmentInfo env = api.getEnvironmentInfo();
    ConnectionInfo cInfo = new ConnectionInfo();
    cInfo.setSelectedApi(selectedApi);
    cInfo.setEnv(env);
    cInfo.setApiParam(apiParam);
    getProductDatabase().add(api, card, cInfo);
  }

}

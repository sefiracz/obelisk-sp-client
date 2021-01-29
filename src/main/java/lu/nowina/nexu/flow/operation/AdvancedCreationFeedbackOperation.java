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
package lu.nowina.nexu.flow.operation;

import eu.europa.esig.dss.DigestAlgorithm;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.KSPrivateKeyEntry;
import lu.nowina.nexu.InternalAPI;
import lu.nowina.nexu.Utils;
import lu.nowina.nexu.api.*;
import lu.nowina.nexu.api.flow.OperationResult;

import java.util.Map;

/**
 * This {@link CompositeOperation} allows to provide some feedback in case of advanced creation.
 *
 * Expected parameters:
 * <ol>
 * <li>{@link NexuAPI}</li>
 * <li>{@link Map} whose keys are {@link TokenOperationResultKey} and values are {@link Object}.</li>
 * </ol>
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public class AdvancedCreationFeedbackOperation extends AbstractCompositeOperation<Void> {

    private NexuAPI api;
    private Map<TokenOperationResultKey, Object> map;
    private DSSPrivateKeyEntry key;

    public AdvancedCreationFeedbackOperation() {
        super();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setParams(final Object... params) {
        try {
            this.api = (NexuAPI) params[0];
            this.map = (Map<TokenOperationResultKey, Object>) params[1];
            this.key = (DSSPrivateKeyEntry) params[2];
        } catch(final ClassCastException | ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Expected parameters: NexuAPI, Map, Key");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public OperationResult<Void> perform() {
        if(this.api.getAppConfig().isEnablePopUps()) {
            final Feedback feedback = new Feedback();
            feedback.setFeedbackStatus(FeedbackStatus.SUCCESS);
            feedback.setApiParameter((String) this.map.get(TokenOperationResultKey.SELECTED_API_PARAMS));
            feedback.setSelectedAPI((ScAPI) this.map.get(TokenOperationResultKey.SELECTED_API));
            feedback.setSelectedCard((DetectedCard) this.map.get(TokenOperationResultKey.SELECTED_PRODUCT));

            if ((feedback.getSelectedCard() != null) && (feedback.getSelectedAPI() != null) &&
                    ((feedback.getSelectedAPI() == ScAPI.MOCCA) || (feedback.getSelectedAPI() == ScAPI.MSCAPI) ||
                            (feedback.getApiParameter() != null))) {

                // TODO - remove this code?
                byte[] id = key.getCertificate().getDigest(DigestAlgorithm.SHA256);
                String certificateId = Utils.encodeHexString(id);
                feedback.getSelectedCard().setCertificateId(certificateId);
                if (key instanceof KSPrivateKeyEntry) {
                    feedback.getSelectedCard().setKeyAlias(((KSPrivateKeyEntry) key).getAlias());
                }
                // store info
                ((InternalAPI) this.api).store(feedback.getSelectedCard(),
                    feedback.getSelectedAPI(), feedback.getApiParameter());

            }
        }
        return new OperationResult<Void>((Void) null);
    }

}

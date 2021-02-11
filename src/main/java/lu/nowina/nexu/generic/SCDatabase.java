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

import lu.nowina.nexu.DatabaseEventHandler;
import lu.nowina.nexu.ProductDatabase;
import lu.nowina.nexu.api.AbstractProduct;
import lu.nowina.nexu.api.DetectedCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@XmlRootElement(name = "database")
@XmlAccessorType(XmlAccessType.FIELD)
public class SCDatabase implements ProductDatabase {

	private static final Logger logger = LoggerFactory.getLogger(SCDatabase.class.getName());

	@XmlElement(name = "smartcard")
	private List<SCInfo> smartcards;

	@XmlTransient
	private DatabaseEventHandler onAddRemoveAction;

	/**
	 * Add a new ConnectionInfo to the database, associated with the DetectedCard and its ATR
	 *
	 * @param detectedCard
	 * @param cInfo
	 */
	public final void add(DetectedCard detectedCard, ConnectionInfo cInfo) {
		SCInfo info = getInfo(detectedCard);
		if (info == null) {
			info = new SCInfo();
			info.setAtr(detectedCard.getAtr());
			info.setCertificateId(detectedCard.getCertificateId());
			info.setCertificate(detectedCard.getCertificate());
			info.setType(detectedCard.getType());
			info.setKeyAlias(detectedCard.getKeyAlias());
			info.setTerminalIndex(detectedCard.getTerminalIndex());
			info.setTerminalLabel(detectedCard.getTerminalLabel());
			info.setTokenLabel(detectedCard.getTokenLabel());
			info.setTokenSerial(detectedCard.getTokenSerial());
			info.setTokenManufacturer(detectedCard.getTokenManufacturer());
		}
		if(!getSmartcards0().contains(info)) { // TODO overit ze je toto spravne
			getSmartcards0().add(info);
			info.getInfos().add(cInfo);
		}
		ProductsMap.getMap().put(detectedCard.getCertificateId(), info);
		onAddRemove();
	}

	public final void remove(final AbstractProduct keystore) {
		getSmartcards0().remove(keystore);
		ProductsMap.getMap().remove(keystore.getCertificateId(), keystore);
		onAddRemove();
	}

	private void onAddRemove() {
		if(onAddRemoveAction != null) {
			onAddRemoveAction.execute(this);
		} else {
			logger.warn("No DatabaseEventHandler define, the database cannot be stored");
		}
	}

	/**
	 * Get SCInfo matching the provided ATR
	 *
	 * @param atr
	 * @return
	 */
	public SCInfo getInfo(String atr, String certId, String keyAlias) {
		for (AbstractProduct ap : getKeystores()) {
			SCInfo scInfo = (SCInfo) ap;
			if (scInfo.getAtr().equals(atr) &&
					(certId == null || scInfo.getCertificateId().equals(certId)) &&
					(keyAlias == null || scInfo.getKeyAlias().equals(keyAlias))) {
				return scInfo;
			}
		}
		return null;
	}

	public SCInfo getInfo(DetectedCard card) {
		for (AbstractProduct ap : getKeystores()) {
			SCInfo scInfo = (SCInfo) ap;
			if (scInfo.getAtr().equals(card.getAtr()) &&
					(card.getCertificateId() == null || scInfo.getCertificateId().equals(card.getCertificateId())) &&
					(card.getKeyAlias() == null || scInfo.getKeyAlias().equals(card.getKeyAlias())) &&
					(card.getTokenLabel() == null || scInfo.getTokenLabel().equals(card.getTokenLabel()))	) {
				return scInfo;
			}
		}
		return null;
	}

	private List<SCInfo> getSmartcards0() {
		if (smartcards == null) {
			this.smartcards = new ArrayList<>();
		}
		return smartcards;
	}

	public List<AbstractProduct> getKeystores() {
		return Collections.unmodifiableList(getSmartcards0());
	}

	@Override
	public void setOnAddRemoveAction(DatabaseEventHandler eventHandler) {
		this.onAddRemoveAction = eventHandler;
	}

	/**
	 * Initialize runtime HashMap of CertificateId to configured Products
	 */
	public void initialize() {
		for (SCInfo keystore : getSmartcards0()) {
			ProductsMap.getMap().put(keystore.getCertificateId(), keystore);
		}
	}

}

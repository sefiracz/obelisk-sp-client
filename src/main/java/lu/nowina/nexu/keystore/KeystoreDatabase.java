/**
 * © Nowina Solutions, 2015-2016
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
package lu.nowina.nexu.keystore;

import lu.nowina.nexu.DatabaseEventHandler;
import lu.nowina.nexu.ProductDatabase;
import lu.nowina.nexu.api.AbstractProduct;
import lu.nowina.nexu.api.ConfiguredKeystore;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.generic.RegisteredProducts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@XmlRootElement(name = "database")
@XmlAccessorType(XmlAccessType.FIELD)
public class KeystoreDatabase implements ProductDatabase {

	private static final Logger LOGGER = LoggerFactory.getLogger(KeystoreDatabase.class.getName());

	@XmlElement(name = "keystore")
	private List<ConfiguredKeystore> keystores;

	@XmlTransient
	private DatabaseEventHandler onAddRemoveAction;

	/**
	 * Adds a new {@link ConfiguredKeystore} to the database.
	 * @param keystore The keystore to add.
	 */
	public final void add(final ConfiguredKeystore keystore) {
		if(!getKeystores().contains(keystore)) {
			getKeystores().add(keystore);
		}
		RegisteredProducts.getMap().put(keystore.getCertificateId(), keystore);
		onAddRemove();
	}

	/**
	 * Removes the given {@link ConfiguredKeystore} from the database.
	 * @param keystore The keystore to remove.
	 */
	public final void remove(NexuAPI api, final AbstractProduct keystore) {
		getKeystores().remove(keystore);
		RegisteredProducts.getMap().remove(keystore.getCertificateId(), keystore);
		onAddRemove();
	}

	private void onAddRemove() {
		if(onAddRemoveAction != null) {
			onAddRemoveAction.execute(this);
		} else {
			LOGGER.warn("No DatabaseEventHandler define, the database cannot be stored");
		}
	}

	private List<ConfiguredKeystore> getKeystores() {
		if (keystores == null) {
			this.keystores = new ArrayList<>();
		}
		return keystores;
	}

	public List<AbstractProduct> getProducts() {
		return Collections.unmodifiableList(getKeystores());
	}

	/**
	 * Initialize runtime HashMap of CertificateId to configured Products
	 */
	public void initialize() {
		for (ConfiguredKeystore keystore : getKeystores()) {
			RegisteredProducts.getMap().put(keystore.getCertificateId(), keystore);
		}
	}

	@Override
	public void setOnAddRemoveAction(DatabaseEventHandler eventHandler) {
		this.onAddRemoveAction = eventHandler;
	}

}

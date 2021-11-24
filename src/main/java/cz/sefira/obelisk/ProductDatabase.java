/**
 * © Nowina Solutions, 2015-2016
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
package cz.sefira.obelisk;

import cz.sefira.obelisk.api.AbstractProduct;
import cz.sefira.obelisk.api.NexuAPI;

import java.util.List;

/**
 * Generic interface for product databases.
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public interface ProductDatabase extends EntityDatabase {

  /**
   * Removes the given product from the database.
   * @param product The product to remove.
   */
	void remove(NexuAPI api, AbstractProduct product);

  /**
   * Returns list of currently stored products
   * @return List of currently stored products
   */
	List<AbstractProduct> getProducts();
}

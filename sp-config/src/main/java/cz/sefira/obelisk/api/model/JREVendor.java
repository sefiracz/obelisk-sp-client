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
package cz.sefira.obelisk.api.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum JREVendor {

	ORACLE, OPEN_JDK, NOT_RECOGNIZED;

	private final static Logger logger = LoggerFactory.getLogger(JREVendor.class);

	public static JREVendor forJREVendor(String jreVendor) {
		if (jreVendor.toLowerCase().contains("oracle")) {
			return ORACLE;
		}
		else if (jreVendor.toLowerCase().contains("openjdk")) {
			return OPEN_JDK;
		}
		else {
		  String jreName = System.getProperty("java.runtime.name");
		  if(jreName != null && jreName.toLowerCase().contains("openjdk")) {
        return OPEN_JDK;
      }
      logger.warn("JRE not recognized. Vendor = '" + jreVendor + "' ; Name = '" + jreName + "'");
      return NOT_RECOGNIZED;
		}
	}

}

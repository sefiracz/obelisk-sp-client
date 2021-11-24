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
package cz.sefira.obelisk.windows.keystore;

import cz.sefira.obelisk.api.NexuAPI;
import cz.sefira.obelisk.api.OS;
import cz.sefira.obelisk.api.plugin.InitializationMessage;
import cz.sefira.obelisk.api.plugin.SignaturePlugin;

import java.util.Collections;
import java.util.List;

/**
 * Plugin that registers Windows Keystore implementation to NexU.
 *
 * @author simon.ghisalberti
 *
 */
public class WindowsKeystorePlugin implements SignaturePlugin {

	public WindowsKeystorePlugin() {
		super();
	}

	@Override
	public List<InitializationMessage> init(String pluginId, NexuAPI api) {
		if (OS.WINDOWS.equals(api.getEnvironmentInfo().getOs())) {
			api.registerProductAdapter(new WindowsKeystoreProductAdapter(api));
		}
		return Collections.emptyList();
	}

}

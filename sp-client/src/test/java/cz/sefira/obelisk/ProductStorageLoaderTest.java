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
package cz.sefira.obelisk;

import cz.sefira.obelisk.token.macos.MacOSKeychain;
import cz.sefira.obelisk.token.windows.WindowsKeystore;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.List;

public class ProductStorageLoaderTest {

	@Test
	public void test1() throws Exception {

		ProductStorage storage = new ProductStorage(Paths.get("src/test/resources/storage"));

		WindowsKeystore keystore = new WindowsKeystore();
		keystore.setCertificate("CERTIFICATE1");
		keystore.setCertificateId("1");
		keystore.setKeyAlias("ALIAS1");

		MacOSKeychain keychain = new MacOSKeychain();
		keychain.setCertificate("CERTIFICATE1");
		keychain.setCertificateId("1");
		keychain.setKeyAlias("ALIAS1");

		storage.add(keychain);
		storage.add(keystore);

		List<WindowsKeystore> keystores = storage.getProducts(WindowsKeystore.class);
		System.out.println(keystores);
		Assert.assertEquals(1, keystores.size());

		List<MacOSKeychain> keychains = storage.getProducts(MacOSKeychain.class);
		System.out.println(keychains);
		Assert.assertEquals(1, keychains.size());

		storage.remove(keychain);
		storage.remove(keystore);

		keystores = storage.getProducts(WindowsKeystore.class);
		System.out.println(keystores);
		Assert.assertEquals(0, keystores.size());

		keychains = storage.getProducts(MacOSKeychain.class);
		System.out.println(keychains);
		Assert.assertEquals(0, keychains.size());
	}

}

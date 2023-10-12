/**
 * © SEFIRA spol. s r.o., 2020-2023
 * <p>
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 * <p>
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 * <p>
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package cz.sefira.obelisk.token.keystore;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import cz.sefira.obelisk.dss.DSSException;
import cz.sefira.obelisk.dss.token.AbstractSignatureTokenConnection;
import cz.sefira.obelisk.dss.token.DSSPrivateKeyEntry;

public abstract class AbstractKeyStoreTokenConnection extends AbstractSignatureTokenConnection {

	abstract KeyStore getKeyStore() throws DSSException;

	abstract PasswordProtection getKeyProtectionParameter();

	@Override
	public List<DSSPrivateKeyEntry> getKeys() throws DSSException {
		final List<DSSPrivateKeyEntry> list = new ArrayList<>();
		try {
			final KeyStore keyStore = getKeyStore();
			final Enumeration<String> aliases = keyStore.aliases();
			while (aliases.hasMoreElements()) {
				final String alias = aliases.nextElement();
				if (keyStore.isKeyEntry(alias)) {
					final PrivateKeyEntry entry = (PrivateKeyEntry) keyStore.getEntry(alias, getKeyProtectionParameter());
					list.add(new KSPrivateKeyEntry(alias, entry));
				} else {
					LOG.debug("No related/supported key found for alias '{}'", alias);
				}
			}
		} catch (GeneralSecurityException e) {
			throw new DSSException("Unable to retrieve keys from keystore", e);
		}
		return list;
	}

	/**
	 * This method allows to retrieve a DSSPrivateKeyEntry by alias
	 * 
	 * @param alias
	 *            the expected entry alias
	 * 
	 * @return the private key or null if the alias does not exist
	 */
	public DSSPrivateKeyEntry getKey(String alias) {
		return getKey(alias, getKeyProtectionParameter());
	}

	/**
	 * This method allows to retrieve a DSSPrivateKeyEntry by alias
	 * 
	 * @param alias
	 *            the expected entry alias
	 * @param passwordProtection
	 *            key password
	 * 
	 * @return the private key or null if the alias does not exist
	 */
	public DSSPrivateKeyEntry getKey(String alias, PasswordProtection passwordProtection) {
		try {
			final KeyStore keyStore = getKeyStore();
			if (keyStore.isKeyEntry(alias)) {
				final PrivateKeyEntry entry = (PrivateKeyEntry) keyStore.getEntry(alias, passwordProtection);
				return new KSPrivateKeyEntry(alias, entry);
			} else {
				LOG.debug("No related/supported key found for alias '{}'", alias);
			}
		} catch (GeneralSecurityException e) {
			throw new DSSException("Unable to retrieve key from keystore", e);
		}
		return null;
	}

}

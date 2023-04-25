package cz.sefira.obelisk.token.keystore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;

import javax.security.auth.DestroyFailedException;

import cz.sefira.obelisk.dss.DSSException;

public class KeyStoreSignatureTokenConnection extends AbstractKeyStoreTokenConnection {

	private final KeyStore keyStore;
	private final PasswordProtection password;

	/**
	 * Construct a KeyStoreSignatureTokenConnection object.
	 * Please note that the keystore password will also be used to retrieve the private key.
	 * For each keystore entry (identifiable by alias) the same private key password will be used.
	 *
	 * If you want to specify a separate private key password use the {@link #getKey(String, PasswordProtection)}
	 * method.
	 *
	 * @param ksFile
	 *            the inputstream which contains the keystore
	 * @param ksType
	 *            the keystore type
	 * @param password
	 *            the keystore password
	 */
	public KeyStoreSignatureTokenConnection(File ksFile, String ksType, PasswordProtection password) throws IOException {
		this(Files.newInputStream(ksFile.toPath()), ksType, password);
	}

	/**
	 * Construct a KeyStoreSignatureTokenConnection object.
	 * Please note that the keystore password will also be used to retrieve the private key.
	 * For each keystore entry (identifiable by alias) the same private key password will be used.
	 * 
	 * If you want to specify a separate private key password use the {@link #getKey(String, PasswordProtection)}
	 * method.
	 * 
	 * @param ksStream
	 *            the inputstream which contains the keystore
	 * @param ksType
	 *            the keystore type
	 * @param password
	 *            the keystore password
	 */
	public KeyStoreSignatureTokenConnection(InputStream ksStream, String ksType, PasswordProtection password) {
		try (InputStream is = ksStream) {
			this.keyStore = KeyStore.getInstance(ksType);
			this.password = password;
			this.keyStore.load(is, password.getPassword());
		} catch (Exception e) {
			throw new DSSException("Unable to instantiate KeyStoreSignatureTokenConnection", e);
		}
	}

	@Override
	KeyStore getKeyStore() {
		return keyStore;
	}

	@Override
	PasswordProtection getKeyProtectionParameter() {
		return password;
	}

	@Override
	public void close() {
		if (password != null) {
			try {
				password.destroy();
			} catch (DestroyFailedException e) {
				LOG.error("Unable to destroy password", e);
			}
		}
	}

}
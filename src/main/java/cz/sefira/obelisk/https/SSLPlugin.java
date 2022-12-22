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
package cz.sefira.obelisk.https;

import cz.sefira.obelisk.NexuException;
import cz.sefira.obelisk.UserPreferences;
import cz.sefira.obelisk.api.EnvironmentInfo;
import cz.sefira.obelisk.api.NexuAPI;
import cz.sefira.obelisk.api.plugin.InitializationMessage;
import cz.sefira.obelisk.api.plugin.NexuPlugin;
import cz.sefira.obelisk.view.DialogMessage;
import cz.sefira.obelisk.view.StandaloneDialog;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * SSL plugin that will perform all initialization tasks required to perform HTTPS.
 */
public class SSLPlugin implements NexuPlugin {

	private static final Logger LOGGER = LoggerFactory.getLogger(SSLPlugin.class.getName());

	public static final String ROOT_CERT = "ssl-root.crt";

	public SSLPlugin() {
		super();
	}

	@Override
	public List<InitializationMessage> init(String pluginId, NexuAPI api) {
		StandaloneDialog.showWelcomeMessage(api); // show welcome message

		final ResourceBundle resourceBundle = ResourceBundle.getBundle("bundles/https");
		final ResourceBundle baseResourceBundle = ResourceBundle.getBundle("bundles/nexu");
		LOGGER.info("Verify if keystore is ready");
		InputStream caCertStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(ROOT_CERT);
		final File nexuHome = api.getAppConfig().getNexuHome();
		final File caCert = new File(nexuHome, ROOT_CERT);
		try (FileOutputStream out = new FileOutputStream(caCert)) {
			if (caCertStream == null) {
				throw new NexuException("Missing CA certificate");
			}
			IOUtils.copy(caCertStream, out);
		}
		catch (IOException e) {
			return Arrays.asList(new InitializationMessage(
							InitializationMessage.MessageType.WARNING,
							resourceBundle.getString("warn.install.cert.title"),
							MessageFormat.format(resourceBundle.getString("warn.install.cert.header"), api.getAppConfig().getApplicationName(), "requirements"),
							baseResourceBundle.getString("contact.application.provider")
					)
			);
		}
		return installCaCert(api, caCert, resourceBundle, baseResourceBundle);
	}

	private List<InitializationMessage> installCaCert(final NexuAPI api, final File caCert, final ResourceBundle resourceBundle, final ResourceBundle baseResourceBundle) {
		final List<InitializationMessage> messages = new ArrayList<>();
		final EnvironmentInfo envInfo = EnvironmentInfo.buildFromSystemProperties(System.getProperties());
		switch(envInfo.getOs()) {
		case WINDOWS:
			messages.addAll(installCaCertInWindowsStore(api, caCert, resourceBundle, baseResourceBundle)); // fallback
			if (firefoxEnabled(api)) {
				messages.addAll(installCaCertInFirefoxForWindows(api, caCert, resourceBundle, baseResourceBundle));
			}
			break;
		case MACOSX:
			messages.addAll(installCaCertInMacUserKeychain(api, caCert, resourceBundle, baseResourceBundle)); // fallback
			if (firefoxEnabled(api)) {
				messages.addAll(installCaCertInFirefoxForMac(api, caCert, resourceBundle, baseResourceBundle));
			}
			break;
		case LINUX:
			messages.addAll(installCaCertInLinuxFFChromeStores(api, caCert, resourceBundle, baseResourceBundle));
			break;
		case NOT_RECOGNIZED:
			LOGGER.warn("Automatic installation of CA certficate is not yet supported for NOT_RECOGNIZED.");
			break;
		default:
			throw new IllegalArgumentException("Unhandled value: " + envInfo.getOs());
		}
		return messages;
	}

	/**
	 * Install the CA Cert in Firefox for Windows.
	 */
	private List<InitializationMessage> installCaCertInFirefoxForWindows(final NexuAPI api, final File caCert, final ResourceBundle resourceBundle, final ResourceBundle baseResourceBundle) {
		Path tempDirPath = null;
		try {
			// 1. Copy and unzip firefox_add-certs-win-1.2.zip
			tempDirPath = Files.createTempDirectory("NexU-Firefox-Add_certs");
			final File tempDirFile = tempDirPath.toFile();
			final File zipFile = new File(tempDirFile, "firefox_add-certs-win-1.2.zip");
			FileUtils.copyURLToFile(this.getClass().getResource("/firefox_add-certs-win-1.2.zip"), zipFile);
			new ZipFile(zipFile).extractAll(tempDirPath.toString());

			// 2. Install caCert into <unzipped_folder>/cacert
			final File unzippedFolder = new File(tempDirFile.getAbsolutePath() + File.separator +
					"firefox_add-certs-win-1.2");
			final File caCertDestDir = new File(unzippedFolder, "cacert");
			FileUtils.copyFile(caCert, new File(caCertDestDir, caCert.getName()));

			// 3. Run add-certs.cmd
			final ProcessBuilder pb = new ProcessBuilder(unzippedFolder + File.separator + "add-certs.cmd");
			pb.redirectErrorStream(true);
			LOGGER.info("Install root certificate into Mozilla Firefox");
			final Process p = pb.start();
			if(!p.waitFor(4, TimeUnit.SECONDS)) {
				throw new NexuException("Timeout occurred when trying to install CA certificate in Firefox");
			}
			if(p.exitValue() == -1) {
				LOGGER.info("Mozilla Firefox not installed.");
			} else if(p.exitValue() != 0) {
				final String output = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());
				throw new NexuException("Batch script returned " + p.exitValue() + " when trying to install CA certificate in Firefox. Output: " + output);
			}
			return Collections.emptyList();
		} catch(Exception e) {
      LOGGER.warn("Exception when trying to install certificate in Firefox", e);
      StandaloneDialog.showSslError(api, "install.ca.cert.firefox", "Firefox", caCert.getName());
			return Arrays.asList(new InitializationMessage(
					InitializationMessage.MessageType.WARNING,
					resourceBundle.getString("warn.install.cert.title"),
					MessageFormat.format(resourceBundle.getString("warn.install.cert.header"), api.getAppConfig().getApplicationName(), "Firefox"),
					baseResourceBundle.getString("contact.application.provider")
				)
			);
		} finally {
			if(tempDirPath != null) {
				try {
					FileUtils.deleteDirectory(tempDirPath.toFile());
				} catch (IOException e) {
					LOGGER.error("IOException when deleting " + tempDirPath.toString() + ": " + e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Installs the CA certificate in Windows Store (used by Chrome, IE and Edge amongst others).
	 */
	private List<InitializationMessage> installCaCertInWindowsStore(final NexuAPI api, final File caCert, final ResourceBundle resourceBundle, final ResourceBundle baseResourceBundle) {
		try (final FileInputStream fis = new FileInputStream(caCert)) {
			final CertificateFactory cf = CertificateFactory.getInstance("X.509");
			final X509Certificate cert = (X509Certificate) cf.generateCertificate(fis);
			// check if root SSL cert is in windows keystore as trusted
			boolean present = checkCaCertIsInstalledInWindowsStore(cert);
			if (!present) {
				DialogMessage message = new DialogMessage("install.ca.cert.missing.warning", DialogMessage.Level.WARNING, 475, 200);
				StandaloneDialog.showDialog(api, message, true);
			}
			int tries = 3, cnt = 0;
			while(!present && cnt < tries) {
				present = insertCaCertIntoWindowsStore(api, cert);
				cnt++;
				if (!present && cnt < tries) {
					DialogMessage message = new DialogMessage("install.ca.cert.not.install.warning", DialogMessage.Level.WARNING, 475, 200);
					StandaloneDialog.showDialog(api, message, true);
				}
			}
			if(!present) {
				DialogMessage message = new DialogMessage("install.ca.cert.not.installed.error", DialogMessage.Level.ERROR, 475, 200);
				StandaloneDialog.showDialog(api, message, true);
			}

			return Collections.emptyList();
		} catch(final KeyStoreException e) {
			LOGGER.warn("KeyStoreException when trying to install certificate in Windows Store", e);
      StandaloneDialog.showSslError(api, "install.ca.cert.ms.keystore", null, caCert.getName());
			// Unfortunately there is no particular exception thrown in this case
			return Arrays.asList(new InitializationMessage(
					InitializationMessage.MessageType.WARNING,
					resourceBundle.getString("warn.install.cert.title"),
					MessageFormat.format(resourceBundle.getString("warn.install.cert.header"), api.getAppConfig().getApplicationName(), "Windows Store"),
					resourceBundle.getString("warn.install.cert.windows.registry") + "\n\n" + baseResourceBundle.getString("contact.application.provider")
				)
			);
		} catch(final Exception e) {
			LOGGER.warn("Exception when trying to install certificate in Windows Store", e);
      StandaloneDialog.showSslError(api, "install.ca.cert.ms.keystore", null, caCert.getName());
      return Arrays.asList(new InitializationMessage(
					InitializationMessage.MessageType.WARNING,
					resourceBundle.getString("warn.install.cert.title"),
					MessageFormat.format(resourceBundle.getString("warn.install.cert.header"), api.getAppConfig().getApplicationName(), "Windows Store"),
					baseResourceBundle.getString("contact.application.provider")
				)
			);
		}
	}

	private boolean checkCaCertIsInstalledInWindowsStore(final X509Certificate certificate)
			throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException {
		final KeyStore keyStore = KeyStore.getInstance("Windows-ROOT");
		keyStore.load(null, null);
		return keyStore.getCertificateAlias(certificate) != null;
	}

	private boolean insertCaCertIntoWindowsStore(final NexuAPI api, final X509Certificate certificate)
			throws CertificateException, IOException, NoSuchAlgorithmException {
		try {
			final KeyStore keyStore = KeyStore.getInstance("Windows-ROOT");
			keyStore.load(null, null);
			if (keyStore.getCertificateAlias(certificate) == null) {
				keyStore.setCertificateEntry(api.getAppConfig().getApplicationName() + "-ssl-root", certificate);
			}
			return true;
		} catch (KeyStoreException e) {
			return false;
		}
	}

	/**
	 * Install the CA Cert in Firefox for Mac.
	 */
	private List<InitializationMessage> installCaCertInFirefoxForMac(final NexuAPI api, final File caCert,
																																	 final ResourceBundle resourceBundle, final ResourceBundle baseResourceBundle) {
		Path tempDirPath = null;
		try {
			// 1. Copy and unzip firefox_add-certs-mac-1.1.zip
			tempDirPath = Files.createTempDirectory("NexU-Firefox-Add_certs");
			final File tempDirFile = tempDirPath.toFile();
			final File zipFile = new File(tempDirFile, "firefox_add-certs-mac-1.1.zip");
			FileUtils.copyURLToFile(this.getClass().getResource("/firefox_add-certs-mac-1.1.zip"), zipFile);
			new ZipFile(zipFile).extractAll(tempDirPath.toString());

			// 2. Run add_certs.sh
			final ProcessBuilder pb = new ProcessBuilder("/bin/bash", "add_certs.sh",
					caCert.getName().substring(0, caCert.getName().lastIndexOf('.')),
					caCert.getAbsolutePath());
			pb.directory(new File(tempDirFile.getAbsolutePath() + File.separator +
					"firefox_add-certs-mac-1.1" + File.separator + "bin"));
			pb.redirectErrorStream(true);
			final Process p = pb.start();
			if(!p.waitFor(4, TimeUnit.SECONDS)) {
				throw new NexuException("Timeout occurred when trying to install CA certificate in Firefox");
			}
			if(p.exitValue() != 0) {
				final String output = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());
				throw new NexuException("Batch script returned " + p.exitValue() + " when trying to install CA certificate in Firefox. Output: " + output);
			}
			return Collections.emptyList();
		} catch(Exception e) {
			LOGGER.warn("Exception when trying to install certificate in Firefox", e);
			StandaloneDialog.showSslError(api, "install.ca.cert.firefox", "Firefox", caCert.getName());
			return Arrays.asList(new InitializationMessage(
							InitializationMessage.MessageType.WARNING,
							resourceBundle.getString("warn.install.cert.title"),
							MessageFormat.format(resourceBundle.getString("warn.install.cert.header"), api.getAppConfig().getApplicationName(), "FireFox"),
							baseResourceBundle.getString("contact.application.provider")
					)
			);
		} finally {
			if(tempDirPath != null) {
				try {
					FileUtils.deleteDirectory(tempDirPath.toFile());
				} catch (IOException e) {
					LOGGER.error("IOException when deleting " + tempDirPath.toString() + ": " + e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Installs the CA certificate in Mac user keychain (used by Safari amongst others).
	 */
	private List<InitializationMessage> installCaCertInMacUserKeychain(final NexuAPI api, final File caCert,
			final ResourceBundle resourceBundle, final ResourceBundle baseResourceBundle) {
		try {
			// check if root SSL cert is in macos keychain as trusted
			boolean present = checkCaCertPresentInMacKeychain(caCert);
			LOGGER.info("Root SSL certificate present in keychain: "+present);
			if (!present) {
				DialogMessage message = new DialogMessage("install.ca.cert.missing.warning", DialogMessage.Level.WARNING, 475, 200);
				StandaloneDialog.showDialog(api, message, true);
			}
			int tries = 3, cnt = 0;
			while(!present && cnt < tries) {
				present = insertCaCertIntoAppleKeychain(caCert);
				cnt++;
				LOGGER.info("Root SSL certificate has been install in keychain: "+present);
				if (!present && cnt < tries) {
					DialogMessage message = new DialogMessage("install.ca.cert.not.install.warning", DialogMessage.Level.WARNING, 475, 200);
					StandaloneDialog.showDialog(api, message, true);
				}
			}
			if(!present) {
				DialogMessage message = new DialogMessage("install.ca.cert.not.installed.error", DialogMessage.Level.ERROR, 475, 200);
				StandaloneDialog.showDialog(api, message, true);
			}

			return Collections.emptyList();
		} catch(Exception e) {
			LOGGER.warn("Exception when trying to install certificate in Mac user keychain", e);
      StandaloneDialog.showSslError(api, "install.ca.cert.mac.keychain", null, caCert.getName());
      return Arrays.asList(new InitializationMessage(
					InitializationMessage.MessageType.WARNING,
					resourceBundle.getString("warn.install.cert.title"),
					MessageFormat.format(resourceBundle.getString("warn.install.cert.header"), api.getAppConfig().getApplicationName(), "Mac user keychain"),
					baseResourceBundle.getString("contact.application.provider")
				)
			);
		}
	}

	private boolean checkCaCertPresentInMacKeychain(final File caCert) throws IOException, InterruptedException {
		String scriptPath = null;
		try {
			scriptPath = getScriptPath("mac_user_keychain_check-presence");
			final ProcessBuilder pb = new ProcessBuilder("/bin/bash", scriptPath,
					caCert.getAbsolutePath());
			pb.redirectErrorStream(true);
			final Process p = pb.start();
			int exitValue = p.waitFor();
			return exitValue == 0;
		} finally {
			deleteTempScript(scriptPath);
		}
	}

	private boolean insertCaCertIntoAppleKeychain(final File caCert) throws IOException, InterruptedException {
		String scriptPath = null;
		try {
			scriptPath = getScriptPath("mac_user_keychain_add-cert");
			final ProcessBuilder pb = new ProcessBuilder("/bin/bash", scriptPath,
					caCert.getAbsolutePath());
			pb.redirectErrorStream(true);
			final Process p = pb.start();
			int exitValue = p.waitFor();
			return exitValue == 0;
		} finally {
			deleteTempScript(scriptPath);
		}
	}

	/**
	 * Installs the CA certificate in Linux FF and Chrome/Chromium stores.
	 */
	private List<InitializationMessage> installCaCertInLinuxFFChromeStores(final NexuAPI api, final File caCert,
			final ResourceBundle resourceBundle, final ResourceBundle baseResourceBundle) {

		Path tempDirPath = null;
		try {
			// 1. Copy and unzip firefox_add-certs-linux-1.0.zip
			tempDirPath = Files.createTempDirectory("NexU-Firefox-Add_certs");
			final File tempDirFile = tempDirPath.toFile();
			final File zipFile = new File(tempDirFile, "firefox_add-certs-linux-1.0.zip");
			FileUtils.copyURLToFile(this.getClass().getResource("/firefox_add-certs-linux-1.0.zip"), zipFile);
			new ZipFile(zipFile).extractAll(tempDirPath.toString());
			// 2. make sure add_certs.sh is executable
			final ProcessBuilder setExec = new ProcessBuilder("chmod", "+x", "./add-certs.sh");
			setExec.directory(new File(tempDirFile.getAbsolutePath() + "/firefox_add-certs-linux-1.0/bin"));
			final Process pExec = setExec.start();
			pExec.waitFor(1, TimeUnit.SECONDS);
			System.out.println(new File(tempDirFile.getAbsolutePath() + File.separator +
					"firefox_add-certs-linux-1.0" + File.separator + "bin").getAbsolutePath()+" "+pExec.exitValue());
			// 3. Run add_certs.sh
			final ProcessBuilder pb = new ProcessBuilder("./add-certs.sh",
					caCert.getName().substring(0, caCert.getName().lastIndexOf('.')),
					caCert.getAbsolutePath());
			pb.directory(new File(tempDirFile.getAbsolutePath() + File.separator +
					"firefox_add-certs-linux-1.0" + File.separator + "bin"));
			pb.redirectErrorStream(true);
			final Process p = pb.start();
			if(!p.waitFor(4, TimeUnit.SECONDS)) {
				throw new NexuException("Timeout occurred when trying to install CA certificate in Linux FF and Chrome/Chromium stores.");
			}
			if(p.exitValue() != 0) {
				final String output = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());
				throw new NexuException("Batch script returned " + p.exitValue() + " when trying to install CA certificate in Linux FF and Chrome/Chromium stores. Output: " + output);
			}
			return Collections.emptyList();
		} catch(Exception e) {
			LOGGER.warn("Exception when trying to install certificate in Linux FF and Chrome/Chromium stores", e);
      StandaloneDialog.showSslError(api, "install.ca.cert.browsers", null, caCert.getName());
			return Arrays.asList(new InitializationMessage(
					InitializationMessage.MessageType.WARNING,
					resourceBundle.getString("warn.install.cert.title"),
					MessageFormat.format(resourceBundle.getString("warn.install.cert.header"), api.getAppConfig().getApplicationName(), "Linux Firefox & Chrome/Chromium stores"),
					baseResourceBundle.getString("contact.application.provider")
				)
			);
		} finally {
			if(tempDirPath != null) {
				try {
					FileUtils.deleteDirectory(tempDirPath.toFile());
				} catch (IOException e) {
					LOGGER.error("IOException when deleting " + tempDirPath.toString() + ": " + e.getMessage(), e);
				}
			}
		}
	}

	private String getScriptPath(String scriptName) throws IOException {
		Path tempFilePath = Files.createTempFile(scriptName, "sh");
		final File tempFile = tempFilePath.toFile();
		FileUtils.copyURLToFile(this.getClass().getResource("/"+scriptName+".sh"), tempFile);
		return tempFile.getAbsolutePath();
	}

	private void deleteTempScript(String scriptPath) {
		if(scriptPath != null) {
			try {
				Files.delete(Paths.get(scriptPath));
			} catch (IOException e) {
				LOGGER.error("IOException when deleting " + Paths.get(scriptPath) + ": " + e.getMessage(), e);
			}
		}
	}

	private boolean firefoxEnabled(NexuAPI api) {
		return new UserPreferences(api.getAppConfig()).getFirefoxSupport();
	}

}

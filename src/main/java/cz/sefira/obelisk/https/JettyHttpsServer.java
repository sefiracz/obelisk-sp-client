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
import cz.sefira.obelisk.Pair;
import cz.sefira.obelisk.jetty.AbstractJettyServer;
import cz.sefira.obelisk.jetty.JettyListAwareServerConnector;
import org.apache.commons.codec.binary.Base64;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static cz.sefira.obelisk.flow.Flow.IN_EXEC;
import static cz.sefira.obelisk.https.SSLPlugin.ROOT_CERT;

public class JettyHttpsServer extends AbstractJettyServer {

	private static final Logger logger = LoggerFactory.getLogger(JettyHttpsServer.class.getName());

	private Pair<String, KeyStore> ssl;
	private static final String random = UUID.randomUUID().toString();

	public JettyHttpsServer() {
		ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "SSL-Monitor");
			t.setDaemon(true);
			return t;
		});
		monitor.scheduleAtFixedRate(() -> {
			Pair<String, KeyStore> store = getCurrentSSL(openKeyStore());
			logger.info("Checking SSL validity: "+store.getFirst().equals(ssl.getFirst()));
			if (!store.getFirst().equals(ssl.getFirst())) {
				try {
					logger.info("Checking if app is in process execution: "+IN_EXEC);
					int count = 3600*23, cnt = 0;
					while(IN_EXEC && cnt < count) {
						Thread.sleep(1000); // waiting (max 23 hours) for user to stop using the app
						cnt++;
					}
					logger.info("Performing HTTP server restart (in_exec: "+IN_EXEC+")");
					stop();
					ssl = store;
					start();
				}
				catch (Exception e) {
					logger.error(e.getMessage(), e);
					throw new NexuException(e);
				}
			}
		}, 1, 1, TimeUnit.DAYS);
	}

	@Override
	public Connector[] getConnectors() {
		// HTTPS connector
		final HttpConfiguration https = new HttpConfiguration();
		https.addCustomizer(new SecureRequestCustomizer());
		// Configuring SSL
		SslContextFactory.Server server = new SslContextFactory.Server.Server();
		ssl = getCurrentSSL(openKeyStore());
		server.setKeyStore(ssl.getSecond());
		server.setKeyStorePassword(random);
		server.setKeyManagerPassword(random);
		// Configuring the connector
		final JettyListAwareServerConnector sslConnector = new JettyListAwareServerConnector(getServer(),
				new SslConnectionFactory(server, "http/1.1"));
		sslConnector.addConnectionFactory(new HttpConnectionFactory(https));
		sslConnector.setPorts((getApi().getAppConfig()).getBindingPortsHttps());
		sslConnector.setHost(InetAddress.getLoopbackAddress().getCanonicalHostName());
		return new Connector[] {sslConnector};
	}

	private KeyStore openKeyStore() {
		try (InputStream sslStore = Thread.currentThread().getContextClassLoader().getResourceAsStream("ssl.jks")) {
			final KeyStore keyStore = KeyStore.getInstance("JKS");
			keyStore.load(sslStore, new String(Base64.decodeBase64("XyRmcGR6TS5wZUxrMWxsMSwlxaFAXw==")).toCharArray());
			return keyStore;
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e)  {
			throw new NexuException(e);
		}
	}

	public static Pair<String, KeyStore> getCurrentSSL(KeyStore keyStore) {
		try {
			Enumeration<String> aliases = keyStore.aliases();
			Date now = new Date();
			while(aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
				try {
					certificate.checkValidity(now);
					Date notAfter = certificate.getNotAfter();
					Calendar cal = Calendar.getInstance();
					cal.setTime(notAfter);
					cal.add(Calendar.DAY_OF_WEEK, -7);
					if (cal.getTime().after(now)) {
						Key key = keyStore.getKey(alias, alias.toCharArray());
						final KeyStore sslStore = KeyStore.getInstance("JKS");
						sslStore.load(null, null);
						try (InputStream caCertStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(ROOT_CERT)) {
							Certificate root = CertificateFactory.getInstance("X509").generateCertificate(caCertStream);
							sslStore.setKeyEntry("ssl", key, random.toCharArray(), new Certificate[]{ certificate, root });
						}
						return Pair.getInstance(alias, sslStore);
					}
				}
				catch (CertificateNotYetValidException | CertificateExpiredException ignore) {
					// not valid, ignore
				}
				catch (UnrecoverableEntryException | NoSuchAlgorithmException | CertificateException | IOException e) {
					throw new NexuException(e);
				}
			}
		}
		catch (KeyStoreException e) {
			throw new NexuException(e);
		}
		return Pair.getInstance("", keyStore); // probably fucked
	}
}

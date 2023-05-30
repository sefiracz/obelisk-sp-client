/**
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package cz.sefira.obelisk.util;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.Utils
 *
 * Created: 08.01.2021
 * Author: hlavnicka
 */

import cz.sefira.crypto.MSCryptoStore;
import cz.sefira.crypto.StoreType;
import cz.sefira.obelisk.api.model.OS;
import cz.sefira.obelisk.api.ws.ssl.SSLCertificateProvider;
import cz.sefira.obelisk.dss.DSSException;
import cz.sefira.obelisk.dss.DigestAlgorithm;
import cz.sefira.obelisk.dss.x509.CertificateToken;
import cz.sefira.obelisk.view.StandaloneDialog;
import cz.sefira.obelisk.view.core.StageState;
import javafx.stage.Stage;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.hc.client5.http.utils.Hex;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.util.io.pem.PemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class X509Utils {

  private static final Logger logger = LoggerFactory.getLogger(X509Utils.class.getName());

  public static X509Certificate getCertificateFromBase64(String base64certificate) throws CertificateException {
    return getCertificateFromBytes(Base64.decodeBase64(base64certificate));
  }

  public static X509Certificate getCertificateFromBytes(byte[] encoded) throws CertificateException {
    return getCertificateFromStream(new ByteArrayInputStream(encoded));
  }

  public static X509Certificate getCertificateFromStream(InputStream in) throws CertificateException {
    CertificateFactory factory = CertificateFactory.getInstance("X509");
    return (X509Certificate) factory.generateCertificate(in);
  }

  public static boolean isSelfSigned(X509Certificate x509Certificate) {
    final String subject = x509Certificate.getSubjectX500Principal().getName(X500Principal.CANONICAL);
    final String issuer = x509Certificate.getIssuerX500Principal().getName(X500Principal.CANONICAL);
    return subject.equals(issuer);
  }

  public static boolean validateCertificateIssuer(X509Certificate subject, X509Certificate issuer) {
    try {
      subject.verify(issuer.getPublicKey());
      return true;
    }
    catch (Exception e) {
      return false;
    }
  }

  public static boolean validateCertificateChain(List<X509Certificate> chain) {
    if (chain == null || chain.isEmpty() || chain.size() == 1) {
      return false;
    }
    boolean valid = true;
    for (int i = chain.size() - 1; i > 0; i--) {
      valid &= validateCertificateIssuer(chain.get(i - 1), chain.get(i));
    }
    return valid;
  }

  public static String convertToPEM(final CertificateToken cert) throws DSSException {
    return convertToPEM(cert.getCertificate());
  }

  private static String convertToPEM(Object obj) throws DSSException {
    try (StringWriter out = new StringWriter(); PemWriter pemWriter = new PemWriter(out)) {
      pemWriter.writeObject(new JcaMiscPEMGenerator(obj));
      pemWriter.flush();
      return out.toString();
    } catch (Exception e) {
      throw new DSSException("Unable to convert DER to PEM", e);
    }
  }

  public static void openPEMCertificate(Stage owner, String pemCertificate) {
    try {
      X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(
          new ByteArrayInputStream(pemCertificate.getBytes(StandardCharsets.UTF_8)));
      StandaloneDialog.createDialogFromFXML("/fxml/certificate-viewer.fxml", owner, StageState.NONBLOCKING, List.of(certificate));
    }
    catch (CertificateException e) {
      logger.error(e.getMessage(), e);
    }
  }

  public static String wrapPEMCertificate(String certificate) {
    byte[] encoded = Base64.decodeBase64(certificate);
    certificate = StringUtils.newStringUtf8(Base64.encodeBase64Chunked(encoded));
    String begin = "-----BEGIN CERTIFICATE-----";
    String end = "-----END CERTIFICATE-----";
    if(!certificate.startsWith(begin)) {
      certificate = begin + "\n" + certificate;
    }
    if(!certificate.endsWith(end)) {
      certificate = certificate + end;
    }
    return certificate;
  }

  public static String createKeyUsageString(final X509Certificate certificate, final ResourceBundle resources) {
    return createKeyUsageString(certificate, resources, ", ");
  }

  public static String createKeyUsageString(final X509Certificate certificate, final ResourceBundle resources,
                                            String delimiter) {
    final boolean[] keyUsages = certificate.getKeyUsage();
    if (keyUsages == null) {
      return "";
    }
    final List<String> keyUsageList = new ArrayList<>();
    if (keyUsages[0]) {
      keyUsageList.add(resources.getString("keyUsage.digitalSignature"));
    }
    if (keyUsages[1]) {
      keyUsageList.add(resources.getString("keyUsage.nonRepudiation"));
    }
    if (keyUsages[2]) {
      keyUsageList.add(resources.getString("keyUsage.keyEncipherment"));
    }
    if (keyUsages[3]) {
      keyUsageList.add(resources.getString("keyUsage.dataEncipherment"));
    }
    if (keyUsages[4]) {
      keyUsageList.add(resources.getString("keyUsage.keyAgreement"));
    }
    if (keyUsages[5]) {
      keyUsageList.add(resources.getString("keyUsage.keyCertSign"));
    }
    if (keyUsages[6]) {
      keyUsageList.add(resources.getString("keyUsage.crlSign"));
    }
    if (keyUsages[7]) {
      keyUsageList.add(resources.getString("keyUsage.encipherOnly"));
    }
    if (keyUsages[8]) {
      keyUsageList.add(resources.getString("keyUsage.decipherOnly"));
    }
    return String.join(delimiter, keyUsageList);
  }

  public static void loadSSLCertificates(KeyStore truststore, SSLCertificateProvider provider) {
    try  (LogUtils.Time total = new LogUtils.Time("SSL certificates loaded in total time")) {
      KeyStore systemStore = null;

      // load up Windows trusted certificates
      if (OS.isWindows()) {
        // load native MSCAPI - ROOT store
        try (LogUtils.Time rootTime = new LogUtils.Time("Windows-ROOT store loaded in")) {
          List<Certificate> caList = MSCryptoStore.getCertificates(StoreType.ROOT);
          for (Certificate ca : caList) {
            X509Utils.addToTrust((X509Certificate) ca, truststore, provider);
          }
        } catch (Exception e) {
          logger.error("Native MSCAPI-ROOT failed: "+e.getMessage(), e);
          // FALLBACK - load Java MSCAPI - ROOT store
          systemStore = KeyStore.getInstance("Windows-ROOT"); // fallback back to Java
        }

        // load native MSCAPI - CA store
        try (LogUtils.Time caTime = new LogUtils.Time("Windows-CA store loaded in")) {
          List<Certificate> caList = MSCryptoStore.getCertificates(StoreType.CA);
          for (Certificate ca : caList) {
            X509Utils.addToTrust((X509Certificate) ca, truststore, provider);
          }
        } catch (Exception e) {
          logger.error("Native MSCAPI-CA failed: "+e.getMessage(), e);
        }
      }

      // load up macOS trusted certificates
      if (OS.isMacOS()) {
        systemStore = KeyStore.getInstance("KeychainStore");
        try (LogUtils.Time macOsTime = new LogUtils.Time("macOS system root loaded in")) {
          List<X509Certificate> caList = X509Utils.loadMacOSSystemRoot();
          for (X509Certificate certificate : caList) {
            X509Utils.addToTrust(certificate, truststore, provider);
          }
        }
      }

      // load up Linux trusted certificates
      if (OS.isLinux()) {
        try (Stream<Path> list = Files.list(Paths.get("/etc/ssl/certs"));
             LogUtils.Time linux = new LogUtils.Time("Linux SSL CAs loaded in")) {
          List<Path> certificates = list.filter(Files::isRegularFile).collect(Collectors.toList());
          for (Path certPath : certificates) {
            try (InputStream in = Files.newInputStream(certPath)) {
              X509Certificate certificate = X509Utils.getCertificateFromStream(in);
              X509Utils.addToTrust(certificate, truststore, provider);
            } catch (Exception e) {
              logger.error(e.getMessage());
            }
          }
        } catch (Exception e) {
          logger.error("Unable to load /etc/ssl/certs: " + e.getMessage());
        }
      }

      // load SSL certificates from system store (Win/Mac)
      if (systemStore != null) {
        try (LogUtils.Time systemTime = new LogUtils.Time("System certificate store loaded in")) {
          systemStore.load(null, null);
          Enumeration<String> trustAliases = systemStore.aliases();
          while (trustAliases.hasMoreElements()) {
            String alias = trustAliases.nextElement();
            Certificate ca = systemStore.getCertificate(alias);
            X509Utils.addToTrust((X509Certificate) ca, truststore, provider);
          }
        }
      }
    } catch (Exception e) {
      logger.error("Unable to load SSL certificates from OS: "+e.getMessage(), e);
    }
  }

  private static void addToTrust(X509Certificate cert, KeyStore truststore, SSLCertificateProvider provider)
      throws KeyStoreException, CertificateEncodingException {
    if (provider.put(cert)) {
      String alias = Hex.encodeHexString(DSSUtils.digest(DigestAlgorithm.SHA1, cert.getEncoded()));
      truststore.setCertificateEntry(alias, cert);
    }
  }

  private static List<X509Certificate> loadMacOSSystemRoot() {
    List<X509Certificate> certificates = new ArrayList<>();
    try {
      List<String> base64Certs = new ArrayList<>();
      StringBuilder sb = new StringBuilder();
      ProcessBuilder builder = new ProcessBuilder();
      builder.command("security", "find-certificate", "-a", "-p", "/System/Library/KeyChains/SystemRootCertificates.keychain");
      Process p = builder.start();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (line.contains("-----BEGIN CERTIFICATE-----")) {
            sb = new StringBuilder(); // start new string
          } else if (line.contains("-----END CERTIFICATE-----")) {
            base64Certs.add(sb.toString()); // dump base64 string
          } else if (!line.trim().isEmpty()) {
            sb.append(line); // append base64
          }
        }
      } catch (IOException e) {
        logger.error(e.getMessage(), e);
      }
      CertificateFactory factory = CertificateFactory.getInstance("X509");
      for (String cert : base64Certs) {
        try {
          certificates.add((X509Certificate) factory.generateCertificate(new ByteArrayInputStream(Base64.decodeBase64(cert))));
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
        }
      }
    } catch (Exception e) {
      logger.error("Unable to read MacOS System root certificates: "+e.getMessage(), e);
    }
    return certificates;
  }

}

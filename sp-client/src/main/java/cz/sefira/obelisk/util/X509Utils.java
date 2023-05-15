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

import cz.sefira.obelisk.dss.DSSException;
import cz.sefira.obelisk.dss.x509.CertificateToken;
import cz.sefira.obelisk.view.StandaloneDialog;
import cz.sefira.obelisk.view.core.StageState;
import javafx.stage.Stage;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.util.io.pem.PemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class X509Utils {

  private static final Logger logger = LoggerFactory.getLogger(X509Utils.class.getName());

  public static X509Certificate getCertificateFromBase64(String base64certificate) throws CertificateException {
    return getCertificateFromBytes(Base64.decodeBase64(base64certificate));
  }

  public static X509Certificate getCertificateFromBytes(byte[] encoded) throws CertificateException {
    CertificateFactory factory = CertificateFactory.getInstance("X509");
    return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(encoded));
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
      openCertificateChain(owner, List.of(certificate));
    }
    catch (CertificateException e) {
      logger.error(e.getMessage(), e);
    }
  }

  public static void openCertificateChain(Stage owner, List<X509Certificate> certificates) {
    try {
      StandaloneDialog.createDialogFromFXML("/fxml/certificate-viewer.fxml", owner, StageState.NONBLOCKING, certificates);
    } catch (Exception e) {
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

}

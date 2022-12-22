package cz.sefira.obelisk.view.x509;
/*
 * Copyright 2013 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.view.x509.FieldSeparator
 *
 * Created: 12.11.13
 * Author: hlavnicka
 */

import cz.sefira.obelisk.Utils;
import eu.europa.esig.dss.DSSPKUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.Extension;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This class separates single fields and information from Certificate
 */
public class CertificateInfoData {

  private static final Logger logger = LoggerFactory.getLogger(CertificateInfoData.class);

  final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

  private final ResourceBundle resources;
  private final String subjectDN;
  private final String issuerDN;
  private final X509Certificate x509cert;

  public CertificateInfoData(Certificate certificate) {
    this.resources = ResourceBundle.getBundle("bundles/nexu");
    this.x509cert = (X509Certificate) certificate;
    // Get subject
    this.subjectDN = x509cert.getSubjectDN().getName();
    // Get issuer
    this.issuerDN = x509cert.getIssuerDN().getName();
  }

  /**
   * Returns start date of certificate
   *
   * @return Returns start date of certificate (NotBefore)
   */
  public String getStartDate() {
    SimpleDateFormat sdf = new SimpleDateFormat("EEEE d. MMMM yyyy HH:mm:ss z", Locale.getDefault());
    return sdf.format(x509cert.getNotBefore());
  }

  /**
   * Returns end date of certificate
   *
   * @return Returns end date of certificate (NotAfter)
   */
  public String getEndDate() {
    SimpleDateFormat sdf = new SimpleDateFormat("EEEE d. MMMM yyyy HH:mm:ss z", Locale.getDefault());
    return sdf.format(x509cert.getNotAfter());
  }

  /**
   * Parse given field name
   *
   * @param DN        Distinguish name of subject/issuer
   * @param fieldName Wanted field
   * @return Returns field information
   */
  private String getField(String DN, String fieldName) {
    String field = null;
    try {
      LdapName ln = new LdapName(DN);
      for (Rdn rdn : ln.getRdns()) {
        if (rdn.getType().equalsIgnoreCase(fieldName)) {
          field = (String) rdn.getValue();
          break;
        }
      }
    }
    catch (InvalidNameException e) {
      field = getCNBackUp(DN, fieldName);
    }
    return field;
  }


  /**
   * This method serves as fallback backup in case that getField() would not perform.
   *
   * @param DN        Distinguish name of subject/issuer
   * @param fieldName Wanted field
   * @return Returns common name
   */
  private String getCNBackUp(String DN, String fieldName) {
    String[] names = DN.split(",");
    String field = null;
    for (String s : names) {
      if (s.contains(fieldName + "=")) {
        field = s.substring(s.indexOf(fieldName + "="));
        field = field.split("=")[1];
      }
    }
    return field;
  }

  private String[] getVersion() {
    return new String[] {resources.getString("certificate.viewer.x509.version"), "V" + x509cert.getVersion()};
  }

  private String[] getSerialNumber() {
    return new String[] {resources.getString("certificate.viewer.x509.serialNumber"),
        "" + x509cert.getSerialNumber(),
        "dec: "+x509cert.getSerialNumber()+"\n"+"hex: "+x509cert.getSerialNumber().toString(16)
    };
  }

  private String[] getSigAlg() {
    return new String[] {resources.getString("certificate.viewer.x509.signatureAlgorithm"),
        x509cert.getSigAlgName()
    };
  }

  private String[] getPubKey() {
    PublicKey key = x509cert.getPublicKey();
    byte[] pubKey = key.getEncoded();
    String hexaSignature = bytesToHex(pubKey);
    String[] hexaBytes = hexaSignature.split(":");

    List<String> signatureLines = new ArrayList<>();
    StringBuilder hexaSignatureBuilder = new StringBuilder();
    StringBuilder rawSignature = new StringBuilder();
    for (int i = 1; i < pubKey.length + 1; i++) {
      byte b = pubKey[i - 1];
      if (b > 31 && b < 127) { // only printable characters (otherwise print period)
        rawSignature.append((char) b);
      }
      else {
        rawSignature.append(".");
      }
      hexaSignatureBuilder.append(hexaBytes[i - 1]).append(" ");
      if (i % 8 == 0) {
        hexaSignatureBuilder.append("  ");
      }
      if (i % 16 == 0) {
        hexaSignatureBuilder.deleteCharAt(hexaSignatureBuilder.length() - 1);
        hexaSignatureBuilder.append(rawSignature);
        signatureLines.add(hexaSignatureBuilder.toString());
        hexaSignatureBuilder = new StringBuilder();
        rawSignature = new StringBuilder();
      }
    }

    StringBuilder sb = new StringBuilder();
    int lineIndex = 0;
    for (String line : signatureLines) {
      String hexaLineIndex = Integer.toHexString(lineIndex);
      int trailingZeroesCount = 4 - hexaLineIndex.length();
      StringBuilder trailingZeroes = new StringBuilder();
      for (int i = 0; i < trailingZeroesCount; i++) {
        trailingZeroes.append("0");
      }
      hexaLineIndex = trailingZeroes + hexaLineIndex;
      sb.append(hexaLineIndex.toUpperCase()).append(": ").append(line).append("\n");
      lineIndex += 16;
    }
    String keyAlg = key.getAlgorithm()+" ("+ DSSPKUtils.getPublicKeySize(key)+"bits)";
    return new String[] {resources.getString("certificate.viewer.x509.pubKey"), keyAlg, sb.toString()};
  }

  private String[] getAuthorityKeyIdentifier() {
    try {
      byte[] fullExtValue = x509cert.getExtensionValue(Extension.authorityKeyIdentifier.getId());
      if (fullExtValue == null) {
        return null;
      }
      byte[] extValue = ASN1OctetString.getInstance(fullExtValue).getOctets();
      AuthorityKeyIdentifier authorityKeyIdentifier = AuthorityKeyIdentifier.getInstance(extValue);
      byte[] aki = authorityKeyIdentifier.getKeyIdentifier();
      if (aki == null)
        return null;
      return new String[] {resources.getString("certificate.viewer.x509.aki"), bytesToHex(aki)};
    } catch (Exception e) {
      return null;
    }
  }

  private String[] getSubjectKeyIdentifier() {
    try {
      byte[] fullExtValue = x509cert.getExtensionValue(Extension.subjectKeyIdentifier.getId());
      if (fullExtValue == null) {
        return null;
      }
      byte[] extValue = ASN1OctetString.getInstance(fullExtValue).getOctets();
      SubjectKeyIdentifier subjectKeyIdentifier = SubjectKeyIdentifier.getInstance(extValue);
      byte[] ski = subjectKeyIdentifier.getKeyIdentifier();
      if (ski == null)
        return null;
      return new String[] {resources.getString("certificate.viewer.x509.ski"), bytesToHex(ski)};
    } catch (Exception e) {
      return null;
    }
  }

  private String[] getField(String DN) {
    StringBuilder fieldBuilder = new StringBuilder();
    String delimiter = ", \n";
    try {
      LdapName ln = new LdapName(DN);
      for (int i = ln.getRdns().size() - 1; i >= 0; i--) {
        Rdn rdn = ln.getRdns().get(i);
        fieldBuilder.append(rdn.getType()).append("=").append(rdn.getValue());
        if (i > 0) {
          fieldBuilder.append(delimiter);
        }
      }
    }
    catch (InvalidNameException e) {
      logger.error(e.getMessage(), e);
    }
    return new String[]{ fieldBuilder.toString(), fieldBuilder.toString().replace(", \n", "\n") };
  }

  private String[] getSubject() {
    String[] dn = getField(subjectDN);
    return new String[] {resources.getString("certificate.viewer.x509.subject"), dn[0], dn[1] };
  }

  private String[] getIssuer() {
    String[] dn = getField(issuerDN);
    return new String[] {resources.getString("certificate.viewer.x509.issuer"),  dn[0], dn[1] };
  }

  private String[] getValidityFrom() {
    return new String[] {resources.getString("certificate.viewer.x509.validity.from"), getStartDate()};
  }

  private String[] getValidityTo() {
    return new String[] {resources.getString("certificate.viewer.x509.validity.to"), getEndDate()};
  }

  private String[] getKeyUsage() {
    return new String[] { resources.getString("certificate.viewer.x509.keyUsage"),
        Utils.createKeyUsageString(x509cert, resources)
    };
  }

  public String[] getFingerPrint(String digest) throws NoSuchAlgorithmException, CertificateEncodingException {
    MessageDigest m = MessageDigest.getInstance(digest);
    byte[] data = x509cert.getEncoded();
    m.update(data, 0, data.length);
    byte[] bytes = m.digest();
    return new String[] {digest + " " + resources.getString("certificate.viewer.x509.fingerPrint"),
        bytesToHex(bytes)
    };
  }

  private String bytesToHex(byte[] bytes) {
    return bytesToHex(bytes, ":");
  }

  private String bytesToHex(byte[] bytes, String delimiter) {
    int v;
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      v = b & 0xFF;
      sb.append(hexArray[v >>> 4]).append(hexArray[v & 0x0F]).append(delimiter);
    }
    String hex = sb.toString();
    return hex.substring(0, hex.length() - 1);
  }

  public List<String[]> getFieldData() {
    List<String[]> fieldData = new ArrayList<>();
    fieldData.add(getVersion());
    fieldData.add(getSerialNumber());
    fieldData.add(getSigAlg());
    fieldData.add(getSubject());
    fieldData.add(getIssuer());
    fieldData.add(getValidityFrom());
    fieldData.add(getValidityTo());
    fieldData.add(getKeyUsage());
    fieldData.add(getPubKey());
    String[] aki = getAuthorityKeyIdentifier();
    if (aki != null) {
      fieldData.add(aki);
    }
    String[] ski = getSubjectKeyIdentifier();
    if (ski != null) {
      fieldData.add(ski);
    }
//    fieldData.add(getSignature());
    try {
      fieldData.add(getFingerPrint("MD5"));
      fieldData.add(getFingerPrint("SHA-1"));
    }
    catch (GeneralSecurityException e) {
      logger.error(e.getMessage(), e);
    }
    return fieldData;
  }
}

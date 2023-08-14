package cz.sefira.obelisk.dss.x509;
/*
 * Copyright 2013 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.view.x509.FieldSeparator
 *
 * Created: 12.11.13
 * Author: hlavnicka
 */

import cz.sefira.obelisk.util.ResourceUtils;
import cz.sefira.obelisk.util.X509Utils;
import cz.sefira.obelisk.dss.DSSASN1Utils;
import cz.sefira.obelisk.util.annotation.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.Extension;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static cz.sefira.obelisk.dss.x509.ExtendedKeyUsage.*;
import static cz.sefira.obelisk.dss.x509.GeneralName.*;

/**
 * This class separates single fields and information from Certificate
 */
public class CertificateDataParser {

  private static final Logger logger = LoggerFactory.getLogger(CertificateDataParser.class);

  final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

  private final ResourceBundle resources;
  private final String subjectDN;
  private final String issuerDN;
  private final X509Certificate x509cert;
  private final List<String[]> fieldData;

  public CertificateDataParser(@NotNull X509Certificate certificate)
      throws CertificateEncodingException, NoSuchAlgorithmException {
    this.resources = ResourceUtils.getBundle();
    this.x509cert = certificate;
    // Get subject
    this.subjectDN = x509cert.getSubjectDN().getName();
    // Get issuer
    this.issuerDN = x509cert.getIssuerDN().getName();
    this.fieldData = new ArrayList<>();
    this.fieldData.add(getVersion());
    this.fieldData.add(getSerialNumber());
    this.fieldData.add(getSigAlg());
    this.fieldData.add(getSubject());
    this.fieldData.add(getIssuer());
    this.fieldData.add(getValidityFrom());
    this.fieldData.add(getValidityTo());
    this.fieldData.add(getKeyUsage());
    String[] extKeyUsage = getExtendedKeyUsages();
    if (extKeyUsage != null) {
      this.fieldData.add(extKeyUsage);
    }
    String[] san = getSubjectAlternativeNames();
    if (san != null) {
      this.fieldData.add(san);
    }
    this.fieldData.add(getPubKey());
    String[] aki = getAuthorityKeyIdentifier();
    if (aki != null) {
      this.fieldData.add(aki);
    }
    String[] ski = getSubjectKeyIdentifier();
    if (ski != null) {
      this.fieldData.add(ski);
    }
    this.fieldData.add(getFingerPrint("MD5"));
    this.fieldData.add(getFingerPrint("SHA-1"));
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
  public String getField(String DN, String fieldName) {
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
  private static String getCNBackUp(String DN, String fieldName) {
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
    String keyAlg = key.getAlgorithm()+" ("+ DSSASN1Utils.getPublicKeySize(key)+"bits)";
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

  private String[] getExtendedKeyUsages() {
    try {
      List<String> extKeyUsages = x509cert.getExtendedKeyUsage();
      if (extKeyUsages == null || extKeyUsages.isEmpty())
        return null;

      List<String> extended = new ArrayList<>();
      for (String usage : extKeyUsages) {
        switch (usage) {
          case SERVER_AUTH:
            extended.add(MessageFormat.format(resources.getString("extended.keyUsage.tslServer"), usage));
            break;
          case CLIENT_AUTH:
            extended.add(MessageFormat.format(resources.getString("extended.keyUsage.tslClient"), usage));
            break;
          case CODE_SIGN:
            extended.add(MessageFormat.format(resources.getString("extended.keyUsage.codeSign"), usage));
            break;
          case EMAIL:
            extended.add(MessageFormat.format(resources.getString("extended.keyUsage.email"), usage));
            break;
          case TIMESTAMP:
            extended.add(MessageFormat.format(resources.getString("extended.keyUsage.timestamp"), usage));
            break;
          case OCSP_SIGN:
            extended.add(MessageFormat.format(resources.getString("extended.keyUsage.ocspSign"), usage));
            break;
          case DOC_SIGN:
            extended.add(MessageFormat.format(resources.getString("extended.keyUsage.docSign"), usage));
            break;
          default:
            extended.add(MessageFormat.format(resources.getString("extended.keyUsage.unknown"), usage));
            break;
        }
      }
      return new String[] {resources.getString("certificate.viewer.x509.extKeyUsage"), String.join("\n", extended)};
    }
    catch (Exception e) {
      return null;
    }
  }

  private String[] getSubjectAlternativeNames() {
    try {
      Collection<List<?>> san = x509cert.getSubjectAlternativeNames();
      if (san == null) {
        return null;
      }
      List<String> names = new ArrayList<>();
      for (List<?> name : san) {
        String type;
        switch ((int) name.get(0)) {
          case NAME_RFC822:
            type = "RFC822 Name = {}";
            break;
          case NAME_DNS:
            type = "DNS Name = {}";
            break;
          case NAME_X400:
            type = "X.400 Address = {}";
            break;
          case NAME_DIRECTORY:
            type = "Directory Name = {}";
            break;
          case NAME_EDI:
            type = "EDI Party Name = {}";
            break;
          case NAME_URI:
            type = "URI = {}";
            break;
          case NAME_IP:
            type = "IP Address = {}";
            break;
          case NAME_OID:
            type = "OID = {}";
            break;
          default:
            type = "Other = {}";
        }
        String value = "";
        if (name.get(1) instanceof String) {
          value = (String) name.get(1);
        } else if (name.get(1) instanceof byte[]) {
          value = bytesToHex((byte[]) name.get(1), "", true);
        }
        names.add(type.replace("{}", value));
      }
      return new String[] {resources.getString("certificate.viewer.x509.san"), String.join("\n", names)};
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
    return new String[] {
        resources.getString("certificate.viewer.x509.keyUsage"),
        X509Utils.createKeyUsageString(x509cert, resources),
        X509Utils.createKeyUsageString(x509cert, resources, "\n")
    };
  }

  private String[] getFingerPrint(String digest) throws NoSuchAlgorithmException, CertificateEncodingException {
    MessageDigest m = MessageDigest.getInstance(digest);
    byte[] data = x509cert.getEncoded();
    m.update(data, 0, data.length);
    byte[] bytes = m.digest();
    String label = MessageFormat.format(resources.getString("certificate.viewer.x509.fingerPrint"), digest);
    return new String[] {label, bytesToHex(bytes)};
  }

  private String bytesToHex(byte[] bytes) {
    return bytesToHex(bytes, ":", false);
  }

  private String bytesToHex(byte[] bytes, String delimiter, boolean lowerCase) {
    int v;
    List<String> byteValues = new ArrayList<>();
    for (byte b : bytes) {
      StringBuilder sb = new StringBuilder();
      v = b & 0xFF;
      byteValues.add(sb.append(hexArray[v >>> 4]).append(hexArray[v & 0x0F]).toString());
    }
    String hex = String.join(delimiter != null ? delimiter : "", byteValues);
    if (lowerCase) {
      hex = hex.toLowerCase();
    }
    return hex;
  }

  public String getSubjectDN() {
    return subjectDN;
  }

  public String getIssuerDN() {
    return issuerDN;
  }

  public X509Certificate getX509Certificate() {
    return x509cert;
  }

  public List<String[]> getFieldData() {
    return fieldData;
  }

  @Override
  public String toString() {
    return subjectDN;
  }
}

/**
 * DSS - Digital Signature Services
 * Copyright (C) 2015 European Commission, provided under the CEF programme
 *
 * This file is part of the "DSS - Digital Signature Services" project.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package cz.sefira.obelisk.dss.x509;

import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import cz.sefira.obelisk.dss.DSSException;
import cz.sefira.obelisk.dss.DigestAlgorithm;
import cz.sefira.obelisk.dss.EncryptionAlgorithm;
import cz.sefira.obelisk.dss.SignatureAlgorithm;
import cz.sefira.obelisk.dss.KeyUsageBit;

/**
 * Whenever the signature validation process encounters an {@link X509Certificate} a certificateToken
 * is created.<br>
 * This class encapsulates some frequently used information: a certificate comes from a certain context (Trusted List,
 * CertStore, Signature), has revocation data... To expedite the processing of such information, they are kept in cache.
 */
@SuppressWarnings("serial")
public class CertificateToken extends Token {

	/**
	 * Encapsulated X509 certificate.
	 */
	private final X509Certificate x509Certificate;

	/**
	 * The default algorithm used to compute the digest value of this certificate
	 */
	private DigestAlgorithm digestAlgorithm = DigestAlgorithm.SHA1;

	private final EncryptionAlgorithm encryptionAlgorithm;


	/**
	 * Indicates if the certificate is self-signed. This attribute stays null till the first call to
	 * {@link #isSelfSigned()} function.
	 */
	private Boolean selfSigned;

	/**
	 * In the case of the XML signature this is the Id associated with the certificate if any.
	 */
	private String xmlId;

	/**
	 * The key usage bits used in the certificate
	 */
	private Set<KeyUsageBit> keyUsageBits;

	/**
	 * This method returns an instance of {@link cz.sefira.obelisk.dss.x509.CertificateToken}.
	 *
	 * @param cert
	 *            <code>X509Certificate</code>
	 * @return the wrapper for the certificate
	 */
	static CertificateToken newInstance(X509Certificate cert) {
		return new CertificateToken(cert);
	}

	/**
	 * Creates a CertificateToken wrapping the provided X509Certificate.
	 *
	 * @param x509Certificate
	 *            the X509Certificate object
	 */
	public CertificateToken(X509Certificate x509Certificate) {
		if (x509Certificate == null) {
			throw new NullPointerException("X509 certificate is missing");
		}

		this.x509Certificate = x509Certificate;
		this.issuerX500Principal = x509Certificate.getIssuerX500Principal();
		// The Algorithm OID is used and not the name {@code x509Certificate.getSigAlgName()}
		this.signatureAlgorithm = SignatureAlgorithm.forOID(x509Certificate.getSigAlgOID());
		this.digestAlgorithm = signatureAlgorithm.getDigestAlgorithm();
		this.encryptionAlgorithm = signatureAlgorithm.getEncryptionAlgorithm();
	}

	/**
	 * Returns the public key associated with the certificate.<br>
	 * To get the encryption algorithm used with this public key call getAlgorithm() method.<br>
	 * RFC 2459:<br>
	 * 4.1.2.7 Subject Public Key Info
	 * This field is used to carry the public key and identify the algorithm with which the key is used. The algorithm
	 * is
	 * identified using the AlgorithmIdentifier structure specified in section 4.1.1.2. The object identifiers for the
	 * supported algorithms and the methods for encoding the public key materials (public key and parameters) are
	 * specified in section 7.3.
	 *
	 * @return the public key of the certificate
	 */
	public PublicKey getPublicKey() {
		return x509Certificate.getPublicKey();
	}

	/**
	 * Returns the expiration date of the certificate.
	 *
	 * @return the expiration date (notAfter)
	 */
	public Date getNotAfter() {
		return x509Certificate.getNotAfter();
	}

	/**
	 * Returns the issuance date of the certificate.
	 *
	 * @return the issuance date (notBefore)
	 */
	public Date getNotBefore() {
		return x509Certificate.getNotBefore();
	}

	/**
	 * Checks if the certificate is expired on the given date.
	 *
	 * @param date
	 *            the date to be tested
	 * @return true if the certificate was expired on the given date
	 */
	public boolean isExpiredOn(final Date date) {
		if ((x509Certificate == null) || (date == null)) {
			return true;
		}
		return x509Certificate.getNotAfter().before(date);
	}

	/**
	 * Checks if the given date is in the validity period of the certificate.
	 *
	 * @param date
	 *            the date to be tested
	 * @return true if the given date is in the certificate period validity
	 */
	public boolean isValidOn(final Date date) {
		if ((x509Certificate == null) || (date == null)) {
			return false;
		}
		try {
			x509Certificate.checkValidity(date);
			return true;
		} catch (CertificateExpiredException | CertificateNotYetValidException e) {
			return false;
		}
	}

	/**
	 * Gets the enclosed X509 Certificate.
	 *
	 * @return the X509Certificate object
	 */
	public X509Certificate getCertificate() {
		return x509Certificate;
	}

	/**
	 * Returns the encoded form of this certificate. X.509 certificates would be encoded as ASN.1 DER.
	 *
	 * @return the encoded form of this certificate
	 */
	@Override
	public byte[] getEncoded() {
		try {
			return x509Certificate.getEncoded();
		} catch (CertificateEncodingException e) {
			throw new DSSException(e);
		}
	}

	/**
	 * Gets the serialNumber value from the encapsulated certificate. The serial number is an integer assigned by the
	 * certification authority to each certificate. It must be unique for each certificate issued by a given CA.
	 *
	 * @return the certificate serial number
	 */
	public BigInteger getSerialNumber() {
		return x509Certificate.getSerialNumber();
	}

	/**
	 * Returns the subject (subject distinguished name) value from the certificate as an X500Principal. If the subject
	 * value is empty, then the getName() method of the returned X500Principal object returns an empty string ("").
	 *
	 * @return the Subject X500Principal
	 */
	public X500Principal getSubjectX500Principal() {
		return x509Certificate.getSubjectX500Principal();
	}

	/**
	 * Returns the used digest algorithm when the certificate was signed
	 * 
	 * @return the used digest algorithm
	 */
	public DigestAlgorithm getDigestAlgorithm() {
		return digestAlgorithm;
	}

	/**
	 * Returns the used encryption algorithm when the certificate was signed (issuer private key algorithm)
	 * 
	 * @return the used encryption algorithm
	 */
	public EncryptionAlgorithm getEncryptionAlgorithm() {
		return encryptionAlgorithm;
	}

	/**
	 * This method checks if the certificate contains the given key usage bit.
	 *
	 * @param keyUsageBit
	 *            the keyUsageBit to be checked.
	 * @return true if contains
	 */
	public boolean checkKeyUsage(final KeyUsageBit keyUsageBit) {
		Set<KeyUsageBit> keyUsageBits = getKeyUsageBits();
		return keyUsageBits.contains(keyUsageBit);
	}

	@Override
	public String toString(String indentStr) {
		try {
			final StringBuilder out = new StringBuilder();
			out.append(indentStr).append("CertificateToken[\n");
			indentStr += "\t";

			String issuerAsString = "";
			if (issuerToken == null) {
				if (isSelfSigned()) {
					issuerAsString = "[SELF-SIGNED]";
				} else {
					issuerAsString = getIssuerX500Principal().toString();
				}
			} else {
				issuerAsString = issuerToken.getDSSIdAsString();
			}
			String certSource = "UNKNOWN";
			out.append(indentStr).append(getDSSIdAsString()).append("<--").append(issuerAsString).append(", source=").append(certSource);
			out.append(", serial=" + x509Certificate.getSerialNumber()).append('\n');
			// Validity period
			out.append(indentStr).append("Validity period    : ").append(x509Certificate.getNotBefore()).append(" - ").append(x509Certificate.getNotAfter())
					.append('\n');
			out.append(indentStr).append("Subject name       : ").append(getSubjectX500Principal()).append('\n');
			out.append(indentStr).append("Issuer subject name: ").append(getIssuerX500Principal()).append('\n');
			out.append(indentStr).append("Signature algorithm: ").append(signatureAlgorithm == null ? "?" : signatureAlgorithm).append('\n');
			if (isTrusted()) {
				out.append(indentStr).append("Signature validity : Signature verification is not needed: trusted certificate\n");
			} else {
				if (signatureValid) {
					out.append(indentStr).append("Signature validity : VALID").append('\n');
				} else {
					if (!signatureInvalidityReason.isEmpty()) {
						out.append(indentStr).append("Signature validity : INVALID").append(" - ").append(signatureInvalidityReason).append('\n');
					}
				}
			}
			if (issuerToken != null) {
				out.append(indentStr).append("Issuer certificate[\n");
				indentStr += "\t";
				if (issuerToken.isSelfSigned()) {
					out.append(indentStr).append(issuerToken.getDSSIdAsString()).append(" SELF-SIGNED");
				} else {
					out.append(issuerToken.toString(indentStr));
				}
				out.append('\n');
				indentStr = indentStr.substring(1);
				out.append(indentStr).append("]\n");
			}
			indentStr = indentStr.substring(1);
			out.append(indentStr).append(']');
			return out.toString();
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	/**
	 * Returns a XML compliant ID
	 * 
	 * @return the id associated with the certificate in case of an XML signature, or null
	 */
	public String getXmlId() {
		return xmlId;
	}

	/**
	 * Sets the Id associated with the certificate in case of an XML signature.
	 *
	 * @param xmlId
	 *            xml compliant id
	 */
	public void setXmlId(final String xmlId) {
		this.xmlId = xmlId;
	}

	/**
	 * This method returns a list {@code KeyUsageBit} representing the key usages of the certificate.
	 *
	 * @return {@code List} of {@code KeyUsageBit}s of different certificate's key usages
	 */
	public Set<KeyUsageBit> getKeyUsageBits() {
		if (keyUsageBits == null) {
			boolean[] keyUsageArray = x509Certificate.getKeyUsage();
			keyUsageBits = new HashSet<KeyUsageBit>();
			if (keyUsageArray != null) {
				for (KeyUsageBit keyUsageBit : KeyUsageBit.values()) {
					if (keyUsageArray[keyUsageBit.getIndex()]) {
						keyUsageBits.add(keyUsageBit);
					}
				}
			}
		}
		return keyUsageBits;
	}

	/**
	 * The signature value of the certificate
	 * 
	 * @return the signature value
	 */
	public byte[] getSignature() {
		return x509Certificate.getSignature();
	}

}

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

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import cz.sefira.obelisk.dss.DSSException;
import cz.sefira.obelisk.dss.DigestAlgorithm;
import cz.sefira.obelisk.dss.SignatureAlgorithm;

/**
 * This is the base class for the different types of tokens (certificate, OCSP, CRL, Timestamp) used in the process of
 * signature validation.
 */
@SuppressWarnings("serial")
public abstract class Token implements Serializable {

	private String dssId;

	/**
	 * This attribute represents the {@link CertificateToken} which is the issuer of the encapsulated Token. In other
	 * words this is the CertificateToken used to sign the enclosed Token.
	 */
	protected CertificateToken issuerToken;

	/**
	 * The normalized {@link X500Principal} of the signer's certificate of this token.
	 */
	protected X500Principal issuerX500Principal;

	/**
	 * Indicates the token signature is valid.
	 */
	protected boolean signatureValid = false;

	/**
	 * Indicates the token signature invalidity reason.
	 */
	protected String signatureInvalidityReason = "";

	/**
	 * The algorithm that was used to sign the token.
	 */
	protected SignatureAlgorithm signatureAlgorithm;

	/**
	 * The token identifier to avoid to compute more than one time the digest value
	 */
	private TokenIdentifier tokenIdentifier;

	private final Map<DigestAlgorithm, byte[]> digests = new HashMap<>();

	@Override
	public final boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Token)) {
			return false;
		}

		Token o2 = (Token) obj;
		return getDSSId().equals(o2.getDSSId());
	}

	@Override
	public final int hashCode() {
		return getDSSId().hashCode();
	}

	/**
	 * Checks if the certificate is provided by the trusted list. For all tokens different from CertificateToken this
	 * method always returns false. This method was introduced in order to manage in a uniform manner the different
	 * tokens.
	 *
	 * @return true if the token is trusted
	 */
	public boolean isTrusted() {
		return false;
	}

	/**
	 * Checks if the certificate is self-signed. For all tokens different from CertificateToken this method always
	 * returns false. This method was introduced in order to manage in a uniform manner the different tokens.
	 *
	 * @return true if the token is self-signed
	 */
	public boolean isSelfSigned() {
		return false;
	}

	/**
	 * Returns a DSS unique token identifier.
	 * 
	 * @return an instance of TokenIdentifier
	 */
	public TokenIdentifier getDSSId() {
		if (tokenIdentifier == null) {
			tokenIdentifier = new TokenIdentifier(this);
		}
		return tokenIdentifier;
	}

	/**
	 * Returns a string representation of the unique DSS token identifier.
	 * 
	 * @return the unique string for the token
	 */
	public String getDSSIdAsString() {
		if (dssId == null) {
			dssId = getDSSId().asXmlId();
		}
		return dssId;
	}

	/**
	 * Returns the {@code X500Principal} of the certificate which was used to sign this token.
	 *
	 * @return the issuer's {@code X500Principal}
	 */
	public X500Principal getIssuerX500Principal() {
		return issuerX500Principal;
	}

	/**
	 * It returns the issuer certificate token that was used to sign this token (CertificateToken, CRLToken,
	 * OCSPRespToken, TimestampToken).
	 *
	 * @return the issuer certificate token
	 */
	public CertificateToken getIssuerToken() {
		return issuerToken;
	}

	/**
	 * Returns the algorithm that was used to sign the token (ex: SHA1WithRSAEncryption, SHA1withRSA...).
	 *
	 * @return the used signature algorithm to sign this token
	 */
	public SignatureAlgorithm getSignatureAlgorithm() {
		return signatureAlgorithm;
	}

	/**
	 * Returns a string representation of the token.
	 *
	 * @param indentStr
	 *            the indentation to use
	 * @return string representation of the token
	 */
	public abstract String toString(String indentStr);

	@Override
	public String toString() {
		return toString("");
	}

	/**
	 * Returns the encoded form of the wrapped token.
	 *
	 * @return the encoded form of the wrapped token
	 */
	public abstract byte[] getEncoded();

	/**
	 * Returns the digest value of the wrapped token
	 * 
	 * @param digestAlgorithm
	 *            the requested digest algorithm
	 * @return the digest value in binaries
	 */
	public byte[] getDigest(DigestAlgorithm digestAlgorithm) {
		byte[] digestValue = digests.get(digestAlgorithm);
		if (digestValue == null) {
			try {
				MessageDigest md = MessageDigest.getInstance(digestAlgorithm.getJavaName());
				digestValue = md.digest(getEncoded());
				digests.put(digestAlgorithm, digestValue);
			} catch (Exception e) {
				throw new DSSException("Unable to compute digest with algo " + digestAlgorithm, e);
			}
		}
		return digestValue;
	}

}

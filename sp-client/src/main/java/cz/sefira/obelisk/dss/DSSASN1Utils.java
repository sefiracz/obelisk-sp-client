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
package cz.sefira.obelisk.dss;

import cz.sefira.obelisk.dss.x509.CertificateToken;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.ocsp.BasicOCSPResponse;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.qualified.QCStatement;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.JCEECPublicKey;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class that contains some ASN1 related method.
 *
 */
public final class DSSASN1Utils {

	private static final Logger LOG = LoggerFactory.getLogger(DSSASN1Utils.class);
	private static final BouncyCastleProvider securityProvider = new BouncyCastleProvider();

	static {
		Security.addProvider(securityProvider);
	}

	/**
	 * This class is an utility class and cannot be instantiated.
	 */
	private DSSASN1Utils() {
	}

	/**
	 * This method returns DER encoded ASN1 attribute. The {@code IOException} is
	 * transformed in {@code DSSException}.
	 *
	 * @param asn1Encodable
	 *            asn1Encodable to be DER encoded
	 * @return array of bytes representing the DER encoded asn1Encodable
	 */
	public static byte[] getDEREncoded(ASN1Encodable asn1Encodable) {
		return getEncoded(asn1Encodable, ASN1Encoding.DER);
	}

	/**
	 * This method returns encoded ASN1 attribute. The {@code IOException} is
	 * transformed in {@code DSSException}.
	 *
	 * @param asn1Encodable
	 *            asn1Encodable to be the given encoding
	 * @param encoding
	 *            the expected encoding
	 * @return array of bytes representing the encoded asn1Encodable
	 */
	private static byte[] getEncoded(ASN1Encodable asn1Encodable, String encoding) {
		try {
			return asn1Encodable.toASN1Primitive().getEncoded(encoding);
		} catch (IOException e) {
			throw new DSSException("Unable to encode to " + encoding, e);
		}
	}

	public static byte[] getEncoded(BasicOCSPResp basicOCSPResp) {
		try {
			BasicOCSPResponse basicOCSPResponse = BasicOCSPResponse.getInstance(basicOCSPResp.getEncoded());
			return getDEREncoded(basicOCSPResponse);
		} catch (IOException e) {
			throw new DSSException(e);
		}
	}

	public static String toString(final ASN1OctetString value) {
		return new String(value.getOctets());
	}

	/**
	 * Returns an ASN.1 encoded bytes representing the {@code TimeStampToken}
	 *
	 * @param timeStampToken
	 *            {@code TimeStampToken}
	 * @return the binary of the {@code TimeStampToken}
	 * @throws DSSException
	 *             if the {@code TimeStampToken} encoding fails
	 */
	public static byte[] getEncoded(final TimeStampToken timeStampToken) throws DSSException {
		try {
			final byte[] encoded = timeStampToken.getEncoded();
			return encoded;
		} catch (IOException e) {
			throw new DSSException(e);
		}
	}

	/**
	 * This method returns the {@code ASN1Sequence} encapsulated in {@code DEROctetString}. The {@code DEROctetString}
	 * is represented as {@code byte} array.
	 *
	 * @param bytes
	 *            {@code byte} representation of {@code DEROctetString}
	 * @return encapsulated {@code ASN1Sequence}
	 * @throws DSSException
	 *             in case of a decoding problem
	 */
	public static ASN1Sequence getAsn1SequenceFromDerOctetString(byte[] bytes) throws DSSException {
		return getASN1Sequence(getDEROctetStringContent(bytes));
	}

	private static byte[] getDEROctetStringContent(byte[] bytes) throws DSSException {
		try (ASN1InputStream input = new ASN1InputStream(bytes)) {
			final DEROctetString s = (DEROctetString) input.readObject();
			return s.getOctets();
		} catch (IOException e) {
			throw new DSSException("Unable to retrieve the DEROctetString content", e);
		}
	}

	private static ASN1Sequence getASN1Sequence(byte[] bytes) throws DSSException {
		try (ASN1InputStream input = new ASN1InputStream(bytes)) {
			return (ASN1Sequence) input.readObject();
		} catch (IOException e) {
			throw new DSSException("Unable to retrieve the ASN1Sequence", e);
		}
	}

	public static int getPublicKeySize(final PublicKey publicKey) {
		int publicKeySize = -1;
		if (publicKey instanceof RSAPublicKey rsaPublicKey) {
			publicKeySize = rsaPublicKey.getModulus().bitLength();
		} else if (publicKey instanceof JCEECPublicKey jceecPublicKey) {
			ECParameterSpec spec = jceecPublicKey.getParameters();
			if (spec != null) {
				publicKeySize = spec.getN().bitLength();
			} else {
				// We support the key, but we don't know the key length
				publicKeySize = 0;
			}
		} else if (publicKey instanceof ECPublicKey ecPublicKey) {
			java.security.spec.ECParameterSpec spec = ecPublicKey.getParams();
			if (spec != null) {
				publicKeySize = spec.getCurve().getField().getFieldSize();
			} else {
				publicKeySize = 0;
			}
		} else if (publicKey instanceof DSAPublicKey dsaPublicKey) {
			publicKeySize = dsaPublicKey.getParams().getP().bitLength();
		} else {
			LOG.error("Unknown public key infrastructure: " + publicKey.getClass().getName());
		}
		return publicKeySize;
	}

	/**
	 * Get the list of all QCStatement Ids that are present in the certificate.
	 * (As per ETSI EN 319 412-5 V2.1.1)
	 * 
	 * @param certToken
	 *            the certificate
	 * @return the list of QC Statements oids
	 */
	public static List<String> getQCStatementsIdList(final CertificateToken certToken) {
		final List<String> extensionIdList = new ArrayList<String>();
		final byte[] qcStatement = certToken.getCertificate().getExtensionValue(Extension.qCStatements.getId());
		if (!Arrays.isNullOrEmpty(qcStatement)) {
			try {
				final ASN1Sequence seq = getAsn1SequenceFromDerOctetString(qcStatement);
				// Sequence of QCStatement
				for (int ii = 0; ii < seq.size(); ii++) {
					final QCStatement statement = QCStatement.getInstance(seq.getObjectAt(ii));
					extensionIdList.add(statement.getStatementId().getId());
				}
			} catch (Exception e) {
				LOG.warn("Unable to parse the qCStatements extension '" + Base64.encodeBase64String(qcStatement) + "' : " + e.getMessage(), e);
			}
		}
		return extensionIdList;
	}

	public static CertificateToken getCertificate(final X509CertificateHolder x509CertificateHolder) {
		try {
			JcaX509CertificateConverter converter = new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
			X509Certificate x509Certificate = converter.getCertificate(x509CertificateHolder);
			return new CertificateToken(x509Certificate);
		} catch (CertificateException e) {
			throw new DSSException(e);
		}
	}

	public static Map<String, String> get(final X500Principal x500Principal) {
		Map<String, String> treeMap = new HashMap<String, String>();
		final byte[] encoded = x500Principal.getEncoded();
		final ASN1Sequence asn1Sequence = ASN1Sequence.getInstance(encoded);
		final ASN1Encodable[] asn1Encodables = asn1Sequence.toArray();
		for (final ASN1Encodable asn1Encodable : asn1Encodables) {

			final DLSet dlSet = (DLSet) asn1Encodable;
			for (int ii = 0; ii < dlSet.size(); ii++) {

				final DLSequence dlSequence = (DLSequence) dlSet.getObjectAt(ii);
				if (dlSequence.size() != 2) {

					throw new DSSException("The DLSequence must contains exactly 2 elements.");
				}
				final ASN1Encodable asn1EncodableAttributeType = dlSequence.getObjectAt(0);
				final String stringAttributeType = getString(asn1EncodableAttributeType);
				final ASN1Encodable asn1EncodableAttributeValue = dlSequence.getObjectAt(1);
				final String stringAttributeValue = getString(asn1EncodableAttributeValue);
				treeMap.put(stringAttributeType, stringAttributeValue);
			}
		}
		return treeMap;
	}

	private static String getString(ASN1Encodable attributeValue) {
		String string;
		if (attributeValue instanceof ASN1String) {
			string = ((ASN1String) attributeValue).getString();
		} else if (attributeValue instanceof ASN1ObjectIdentifier) {
			string = ((ASN1ObjectIdentifier) attributeValue).getId();
		} else {
			LOG.error("!!!*******!!! This encoding is unknown: " + attributeValue.getClass().getSimpleName());
			string = attributeValue.toString();
			LOG.error("!!!*******!!! value: " + string);
		}
		return string;
	}

	public static String extractAttributeFromX500Principal(ASN1ObjectIdentifier identifier, X500Principal x500PrincipalName) {
		final X500Name x500Name = X500Name.getInstance(x500PrincipalName.getEncoded());
		RDN[] rdns = x500Name.getRDNs(identifier);
		for (RDN rdn : rdns) {
			if (rdn.isMultiValued()) {
				AttributeTypeAndValue[] typesAndValues = rdn.getTypesAndValues();
				for (AttributeTypeAndValue typeAndValue : typesAndValues) {
					if (identifier.equals(typeAndValue.getType())) {
						return typeAndValue.getValue().toString();
					}
				}
			} else {
				AttributeTypeAndValue typeAndValue = rdn.getFirst();
				if (identifier.equals(typeAndValue.getType())) {
					return typeAndValue.getValue().toString();
				}
			}
		}
		return null;
	}

	public static String getSubjectCommonName(CertificateToken cert) {
		return extractAttributeFromX500Principal(BCStyle.CN, cert.getSubjectX500Principal());
	}

	public static boolean isEmpty(AttributeTable attributeTable) {
		return (attributeTable == null) || (attributeTable.size() == 0);
	}

}
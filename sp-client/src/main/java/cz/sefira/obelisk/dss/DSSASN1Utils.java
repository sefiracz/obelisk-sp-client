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

import java.io.IOException;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

import cz.sefira.obelisk.util.DSSUtils;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.BERTags;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.DLSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.ocsp.BasicOCSPResponse;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.asn1.x509.qualified.QCStatement;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.JCEECPublicKey;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.x509.extension.X509ExtensionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.sefira.obelisk.dss.x509.CertificateToken;

/**
 * Utility class that contains some ASN1 related method.
 *
 */
public final class DSSASN1Utils {

	private static final Logger LOG = LoggerFactory.getLogger(DSSASN1Utils.class);

	private static final String QC_TYPE_STATEMENT_OID = "0.4.0.1862.1.6";

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
	 * This method returns {@code T extends ASN1Primitive} created from array of bytes. The {@code IOException} is
	 * transformed in {@code DSSException}.
	 *
	 * @param bytes
	 *            array of bytes to be transformed to {@code ASN1Primitive}
	 * @param <T>
	 *            the expected return type
	 * @return new {@code T extends ASN1Primitive}
	 */
	public static <T extends ASN1Primitive> T toASN1Primitive(final byte[] bytes) throws DSSException {
		try {
			@SuppressWarnings("unchecked")
			final T asn1Primitive = (T) ASN1Primitive.fromByteArray(bytes);
			return asn1Primitive;
		} catch (IOException e) {
			throw new DSSException(e);
		}
	}

	/**
	 * This method checks if a given {@code DEROctetString} is null.
	 *
	 * @param derOctetString
	 *            the {@code DEROctetString} to check
	 * @return true if the {@code DEROctetString} contains DERNull
	 */
	private static boolean isDEROctetStringNull(final DEROctetString derOctetString) {
		final byte[] derOctetStringBytes = derOctetString.getOctets();
		final ASN1Primitive asn1Null = toASN1Primitive(derOctetStringBytes);
		return DERNull.INSTANCE.equals(asn1Null);
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
	 * This method returns BER encoded ASN1 attribute. The {@code IOException} is
	 * transformed in {@code DSSException}.
	 *
	 * @param asn1Encodable
	 *            asn1Encodable to be BER encoded
	 * @return array of bytes representing the BER encoded asn1Encodable
	 */
	public static byte[] getBEREncoded(ASN1Encodable asn1Encodable) {
		return getEncoded(asn1Encodable, ASN1Encoding.BER);
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

	public static Date toDate(final ASN1GeneralizedTime asn1Date) throws DSSException {
		try {
			return asn1Date.getDate();
		} catch (ParseException e) {
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

	/**
	 * This method can be removed the simple IssuerSerial verification can be
	 * performed. In fact the hash verification is sufficient.
	 *
	 * @param generalNames
	 *            the generalNames
	 * @return the canonicalized name
	 */
	public static String getCanonicalizedName(final GeneralNames generalNames) {
		GeneralName[] names = generalNames.getNames();
		TreeMap<String, String> treeMap = new TreeMap<String, String>();
		for (GeneralName name : names) {
			String ldapString = String.valueOf(name.getName());
			LOG.debug("ldapString to canonicalize: {} ", ldapString);
			try {
				LdapName ldapName = new LdapName(ldapString);
				List<Rdn> rdns = ldapName.getRdns();
				for (final Rdn rdn : rdns) {
					treeMap.put(rdn.getType().toLowerCase(), String.valueOf(rdn.getValue()).toLowerCase());
				}
			} catch (InvalidNameException e) {
				throw new DSSException(e);
			}
		}
		StringBuilder stringBuilder = new StringBuilder();
		for (Entry<String, String> entry : treeMap.entrySet()) {
			stringBuilder.append(entry.getKey()).append('=').append(entry.getValue()).append('|');
		}
		final String canonicalizedName = stringBuilder.toString();
		LOG.debug("canonicalizedName: {} ", canonicalizedName);
		return canonicalizedName;
	}

	public static int getPublicKeySize(final PublicKey publicKey) {
		int publicKeySize = -1;
		if (publicKey instanceof RSAPublicKey) {
			RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
			publicKeySize = rsaPublicKey.getModulus().bitLength();
		} else if (publicKey instanceof JCEECPublicKey) {
			final JCEECPublicKey jceecPublicKey = (JCEECPublicKey) publicKey;
			ECParameterSpec spec = jceecPublicKey.getParameters();
			if (spec != null) {
				publicKeySize = spec.getN().bitLength();
			} else {
				// We support the key, but we don't know the key length
				publicKeySize = 0;
			}
		} else if (publicKey instanceof ECPublicKey) {
			ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
			java.security.spec.ECParameterSpec spec = ecPublicKey.getParams();
			if (spec != null) {
				publicKeySize = spec.getCurve().getField().getFieldSize();
			} else {
				publicKeySize = 0;
			}
		} else if (publicKey instanceof DSAPublicKey) {
			DSAPublicKey dsaPublicKey = (DSAPublicKey) publicKey;
			publicKeySize = dsaPublicKey.getParams().getP().bitLength();
		} else {
			LOG.error("Unknown public key infrastructure: " + publicKey.getClass().getName());
		}
		return publicKeySize;
	}

	/**
	 * Gets the ASN.1 algorithm identifier structure corresponding to a digest algorithm
	 *
	 * @param digestAlgorithm
	 *            the digest algorithm to encode
	 * @return the ASN.1 algorithm identifier structure
	 */
	public static AlgorithmIdentifier getAlgorithmIdentifier(DigestAlgorithm digestAlgorithm) {

		/*
		 * The recommendation (cf. RFC 3380 section 2.1) is to omit the parameter for SHA-1, but some implementations
		 * still expect a
		 * NULL there. Therefore, we always include a NULL parameter even with SHA-1, despite the recommendation, because
		 * the RFC
		 * states that implementations SHOULD support it as well anyway
		 */
		final ASN1ObjectIdentifier asn1ObjectIdentifier = new ASN1ObjectIdentifier(digestAlgorithm.getOid());
		return new AlgorithmIdentifier(asn1ObjectIdentifier, DERNull.INSTANCE);
	}

	/**
	 * Indicates if the revocation data should be checked for an OCSP signing certificate.<br>
	 * http://www.ietf.org/rfc/rfc2560.txt?number=2560<br>
	 * A CA may specify that an OCSP client can trust a responder for the lifetime of the responder's certificate. The
	 * CA does so by including the extension id-pkix-ocsp-nocheck. This SHOULD be a non-critical extension. The value of
	 * the extension should be NULL.
	 *
	 * @param token
	 *            the certificate to be checked
	 * @return true if the certificate has the id_pkix_ocsp_nocheck extension
	 */
	public static boolean hasIdPkixOcspNoCheckExtension(CertificateToken token) {
		final byte[] extensionValue = token.getCertificate().getExtensionValue(OCSPObjectIdentifiers.id_pkix_ocsp_nocheck.getId());
		if (extensionValue != null) {
			try {
				final ASN1Primitive derObject = toASN1Primitive(extensionValue);
				if (derObject instanceof DEROctetString) {
					return isDEROctetStringNull((DEROctetString) derObject);
				}
			} catch (Exception e) {
				LOG.debug("Exception when processing 'id_pkix_ocsp_no_check'", e);
			}
		}
		return false;
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

	/**
	 * This method returns SKI bytes from the certificate extension.
	 *
	 * @param certificateToken
	 *            the {@code CertificateToken}
	 * @return ski bytes from the given certificate or null if missing
	 * @throws DSSException
	 *             if encoding error occurred
	 */
	public static byte[] getSki(final CertificateToken certificateToken) throws DSSException {
		return getSki(certificateToken, false);
	}

	/**
	 * This method returns SKI bytes from certificate.
	 *
	 * @param certificateToken
	 *            {@code CertificateToken}
	 * @param computeIfMissing
	 *            if the extension is missing and computeIfMissing = true, it will compute the SKI value from the Public
	 *            Key
	 * @return ski bytes from the given certificate
	 * @throws DSSException
	 *             if encoding error occurred
	 */
	public static byte[] getSki(final CertificateToken certificateToken, boolean computeIfMissing) throws DSSException {
		try {
			byte[] sKI = certificateToken.getCertificate().getExtensionValue(Extension.subjectKeyIdentifier.getId());
			if (!Arrays.isNullOrEmpty(sKI)) {
				ASN1Primitive extension = X509ExtensionUtil.fromExtensionValue(sKI);
				SubjectKeyIdentifier skiBC = SubjectKeyIdentifier.getInstance(extension);
				return skiBC.getKeyIdentifier();
			} else if (computeIfMissing) {
				// If extension not present, we compute it from the certificate public key
				DLSequence seq = (DLSequence) DERSequence.fromByteArray(certificateToken.getPublicKey().getEncoded());
				DERBitString item = (DERBitString) seq.getObjectAt(1);
				return DSSUtils.digest(DigestAlgorithm.SHA1, item.getOctets());
			}
			return null;
		} catch (Exception e) {
			throw new DSSException(e);
		}
	}

	private static String parseGn(GeneralName gn) {
		try {
			if (GeneralName.uniformResourceIdentifier == gn.getTagNo()) {
				ASN1String str = (ASN1String) ((DERTaggedObject) gn.toASN1Primitive()).getObject();
				return str.getString();
			}
		} catch (Exception e) {
			LOG.warn("Unable to parse GN " + gn, e);
		}
		return null;
	}

	/**
	 * Returns a {@code X509CertificateHolder} encapsulating the given {@code X509Certificate}.
	 * 
	 * @param certToken
	 *            the certificate to be encapsulated
	 * @return a X509CertificateHolder holding this certificate
	 */
	public static X509CertificateHolder getX509CertificateHolder(CertificateToken certToken) {
		try {
			return new X509CertificateHolder(certToken.getEncoded());
		} catch (IOException e) {
			throw new DSSException(e);
		}
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

	public static String getUtf8String(final X500Principal x500Principal) {

		final byte[] encoded = x500Principal.getEncoded();
		final ASN1Sequence asn1Sequence = ASN1Sequence.getInstance(encoded);
		final ASN1Encodable[] asn1Encodables = asn1Sequence.toArray();
		final StringBuilder stringBuilder = new StringBuilder();
		/**
		 * RFC 4514 LDAP: Distinguished Names
		 * 2.1. Converting the RDNSequence
		 *
		 * If the RDNSequence is an empty sequence, the result is the empty or
		 * zero-length string.
		 *
		 * Otherwise, the output consists of the string encodings of each
		 * RelativeDistinguishedName in the RDNSequence (according to Section
		 * 2.2), starting with the last element of the sequence and moving
		 * backwards toward the first.
		 * ...
		 */
		for (int ii = asn1Encodables.length - 1; ii >= 0; ii--) {

			final ASN1Encodable asn1Encodable = asn1Encodables[ii];

			final DLSet dlSet = (DLSet) asn1Encodable;
			for (int jj = 0; jj < dlSet.size(); jj++) {

				final DLSequence dlSequence = (DLSequence) dlSet.getObjectAt(jj);
				if (dlSequence.size() != 2) {

					throw new DSSException("The DLSequence must contains exactly 2 elements.");
				}
				final ASN1Encodable attributeType = dlSequence.getObjectAt(0);
				final ASN1Encodable attributeValue = dlSequence.getObjectAt(1);
				String string = getString(attributeValue);

				/**
				 * RFC 4514 LDAP: Distinguished Names
				 * ...
				 * Other characters may be escaped.
				 *
				 * Each octet of the character to be escaped is replaced by a backslash
				 * and two hex digits, which form a single octet in the code of the
				 * character. Alternatively, if and only if the character to be escaped
				 * is one of
				 *
				 * ' ', '"', '#', '+', ',', ';', '<', '=', '>', or '\'
				 * (U+0020, U+0022, U+0023, U+002B, U+002C, U+003B,
				 * U+003C, U+003D, U+003E, U+005C, respectively)
				 *
				 * it can be prefixed by a backslash ('\' U+005C).
				 */
				string = Rdn.escapeValue(string);
				if (stringBuilder.length() != 0) {
					stringBuilder.append(',');
				}
				stringBuilder.append(attributeType).append('=').append(string);
			}
		}
		return stringBuilder.toString();
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

	public static String getHumanReadableName(CertificateToken cert) {
		return firstNotNull(cert, BCStyle.CN, BCStyle.GIVENNAME, BCStyle.SURNAME, BCStyle.NAME, BCStyle.PSEUDONYM, BCStyle.O, BCStyle.OU);
	}

	private static String firstNotNull(CertificateToken cert, ASN1ObjectIdentifier... oids) {
		for (ASN1ObjectIdentifier oid : oids) {
			String value = extractAttributeFromX500Principal(oid, cert.getSubjectX500Principal());
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	/**
	 * Returns the first {@code SignerInformation} extracted from {@code CMSSignedData}.
	 *
	 * @param cms
	 *            CMSSignedData
	 * @return returns {@code SignerInformation}
	 */
	public static SignerInformation getFirstSignerInformation(final CMSSignedData cms) {
		final Collection<SignerInformation> signers = cms.getSignerInfos().getSigners();
		if (signers.size() > 1) {
			LOG.warn("!!! The framework handles only one signer (SignerInformation) !!!");
		}
		return signers.iterator().next();
	}

	public static boolean isASN1SequenceTag(byte tagByte) {
		// BERTags.SEQUENCE | BERTags.CONSTRUCTED = 0x30
		return (BERTags.SEQUENCE | BERTags.CONSTRUCTED) == tagByte;
	}

	public static Date getDate(ASN1Encodable encodable) {
		try {
			return Time.getInstance(encodable).getDate();
		} catch (Exception e) {
			LOG.warn("Unable to retrieve the date : " + encodable, e);
			return null;
		}
	}

	public static boolean isEmpty(AttributeTable attributeTable) {
		return (attributeTable == null) || (attributeTable.size() == 0);
	}

	public static AttributeTable emptyIfNull(AttributeTable original) {
		if (original == null) {
			return new AttributeTable(new Hashtable<ASN1ObjectIdentifier, Attribute>());
		} else {
			return original;
		}
	}

	public static List<String> getExtendedKeyUsage(CertificateToken certToken) {
		try {
			return certToken.getCertificate().getExtendedKeyUsage();
		} catch (CertificateParsingException e) {
			LOG.warn("Unable to retrieve ExtendedKeyUsage " + e.getMessage());
			return Collections.emptyList();
		}
	}

}
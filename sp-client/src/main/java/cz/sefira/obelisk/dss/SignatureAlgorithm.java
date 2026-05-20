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

import cz.sefira.obelisk.util.annotation.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public enum SignatureAlgorithm {

	// RSA
	RSA_RAW(EncryptionAlgorithm.RSA, DigestAlgorithm.NONE),
	RSA_SHA1(EncryptionAlgorithm.RSA, DigestAlgorithm.SHA1),
	RSA_SHA224(EncryptionAlgorithm.RSA, DigestAlgorithm.SHA224),
	RSA_SHA256(EncryptionAlgorithm.RSA,	DigestAlgorithm.SHA256),
	RSA_SHA384(EncryptionAlgorithm.RSA, DigestAlgorithm.SHA384),
	RSA_SHA512(EncryptionAlgorithm.RSA, DigestAlgorithm.SHA512),
	RSA_SHA3_224(EncryptionAlgorithm.RSA, DigestAlgorithm.SHA3_224),
	RSA_SHA3_256(EncryptionAlgorithm.RSA, DigestAlgorithm.SHA3_256),
	RSA_SHA3_384(EncryptionAlgorithm.RSA, DigestAlgorithm.SHA3_384),
	RSA_SHA3_512(EncryptionAlgorithm.RSA, DigestAlgorithm.SHA3_512),
	RSA_RIPEMD160(EncryptionAlgorithm.RSA, DigestAlgorithm.RIPEMD160),
	RSA_MD5(EncryptionAlgorithm.RSA, DigestAlgorithm.MD5),

	// RSA-PSS
	RSA_SSA_PSS_RAW_MGF1(EncryptionAlgorithm.RSA, DigestAlgorithm.NONE, MaskGenerationFunction.MGF1),
	RSA_SSA_PSS_SHA1_MGF1(EncryptionAlgorithm.RSA, DigestAlgorithm.SHA1, MaskGenerationFunction.MGF1),
	RSA_SSA_PSS_SHA224_MGF1(EncryptionAlgorithm.RSA, DigestAlgorithm.SHA224, MaskGenerationFunction.MGF1),
	RSA_SSA_PSS_SHA256_MGF1(EncryptionAlgorithm.RSA, DigestAlgorithm.SHA256, MaskGenerationFunction.MGF1),
	RSA_SSA_PSS_SHA384_MGF1(EncryptionAlgorithm.RSA, DigestAlgorithm.SHA384, MaskGenerationFunction.MGF1),
	RSA_SSA_PSS_SHA512_MGF1(EncryptionAlgorithm.RSA, DigestAlgorithm.SHA512, MaskGenerationFunction.MGF1),
	RSA_SSA_PSS_SHA3_224_MGF1(EncryptionAlgorithm.RSA, DigestAlgorithm.SHA3_224, MaskGenerationFunction.MGF1),
	RSA_SSA_PSS_SHA3_256_MGF1(EncryptionAlgorithm.RSA, DigestAlgorithm.SHA3_256, MaskGenerationFunction.MGF1),
	RSA_SSA_PSS_SHA3_384_MGF1(EncryptionAlgorithm.RSA, DigestAlgorithm.SHA3_384, MaskGenerationFunction.MGF1),
	RSA_SSA_PSS_SHA3_512_MGF1(EncryptionAlgorithm.RSA, DigestAlgorithm.SHA3_512, MaskGenerationFunction.MGF1),
	RSA_SSA_PSS_RIPEMD160_MGF1(EncryptionAlgorithm.RSA, DigestAlgorithm.RIPEMD160, MaskGenerationFunction.MGF1),
	RSA_SSA_PSS_MD5_MGF1(EncryptionAlgorithm.RSA, DigestAlgorithm.MD5, MaskGenerationFunction.MGF1),

	// ECDSA
	ECDSA_RAW(EncryptionAlgorithm.ECDSA, DigestAlgorithm.NONE),
	ECDSA_SHA1(EncryptionAlgorithm.ECDSA, DigestAlgorithm.SHA1),
	ECDSA_SHA224(EncryptionAlgorithm.ECDSA, DigestAlgorithm.SHA224),
	ECDSA_SHA256(EncryptionAlgorithm.ECDSA,	DigestAlgorithm.SHA256),
	ECDSA_SHA384(EncryptionAlgorithm.ECDSA, DigestAlgorithm.SHA384),
	ECDSA_SHA512(EncryptionAlgorithm.ECDSA,	DigestAlgorithm.SHA512),
	ECDSA_SHA3_224(EncryptionAlgorithm.ECDSA, DigestAlgorithm.SHA3_224),
	ECDSA_SHA3_256(EncryptionAlgorithm.ECDSA, DigestAlgorithm.SHA3_256),
	ECDSA_SHA3_384(EncryptionAlgorithm.ECDSA, DigestAlgorithm.SHA3_384),
	ECDSA_SHA3_512(EncryptionAlgorithm.ECDSA, DigestAlgorithm.SHA3_512),
	ECDSA_RIPEMD160(EncryptionAlgorithm.ECDSA, DigestAlgorithm.RIPEMD160),

	// DSA
	DSA_RAW(EncryptionAlgorithm.DSA, DigestAlgorithm.NONE),
	DSA_SHA1(EncryptionAlgorithm.DSA, DigestAlgorithm.SHA1),
	DSA_SHA224(EncryptionAlgorithm.DSA, DigestAlgorithm.SHA224),
	DSA_SHA256(EncryptionAlgorithm.DSA, DigestAlgorithm.SHA256),
	DSA_SHA384(EncryptionAlgorithm.DSA, DigestAlgorithm.SHA384),
	DSA_SHA512(EncryptionAlgorithm.DSA, DigestAlgorithm.SHA512),
	DSA_SHA3_224(EncryptionAlgorithm.DSA, DigestAlgorithm.SHA3_224),
	DSA_SHA3_256(EncryptionAlgorithm.DSA, DigestAlgorithm.SHA3_256),
	DSA_SHA3_384(EncryptionAlgorithm.DSA, DigestAlgorithm.SHA3_384),
	DSA_SHA3_512(EncryptionAlgorithm.DSA, DigestAlgorithm.SHA3_512),

	// Pure MLDSA
	ML_DSA_44(EncryptionAlgorithm.ML_DSA_44),
	ML_DSA_65(EncryptionAlgorithm.ML_DSA_65),
	ML_DSA_87(EncryptionAlgorithm.ML_DSA_87),

	// Pre-hash MLDSA
	ML_DSA_44_WITH_SHA512(EncryptionAlgorithm.ML_DSA_44_SHA512, DigestAlgorithm.SHA512),
	ML_DSA_65_WITH_SHA512(EncryptionAlgorithm.ML_DSA_65_SHA512, DigestAlgorithm.SHA512),
	ML_DSA_87_WITH_SHA512(EncryptionAlgorithm.ML_DSA_87_SHA512, DigestAlgorithm.SHA512);

	private final EncryptionAlgorithm encryptionAlgo;
	private final DigestAlgorithm digestAlgo;
	private final MaskGenerationFunction maskGenerationFunction;

	SignatureAlgorithm(final EncryptionAlgorithm encryptionAlgorithm) {
		this(encryptionAlgorithm, null, null);
	}

	SignatureAlgorithm(final EncryptionAlgorithm encryptionAlgorithm, final DigestAlgorithm digestAlgorithm) {
		this(encryptionAlgorithm, digestAlgorithm, null);
	}

	SignatureAlgorithm(final EncryptionAlgorithm encryptionAlgorithm, final DigestAlgorithm digestAlgorithm,
	                   final MaskGenerationFunction maskGenerationFunction) {
		this.encryptionAlgo = encryptionAlgorithm;
		this.digestAlgo = digestAlgorithm;
		this.maskGenerationFunction = maskGenerationFunction;
	}

	private static class Registry {

		private final static Map<String, SignatureAlgorithm> XML_ALGORITHMS = registerXmlAlgorithms();
		private final static Map<String, SignatureAlgorithm> OID_ALGORITHMS = registerOidAlgorithms();
		private final static Map<String, SignatureAlgorithm> JAVA_ALGORITHMS = registerJavaAlgorithms();

		private static Map<String, SignatureAlgorithm> registerXmlAlgorithms() {
			final Map<String, SignatureAlgorithm> map = new HashMap<>();
			map.put("http://www.w3.org/2000/09/xmldsig#rsa-sha1", RSA_SHA1);
			map.put("http://www.w3.org/2001/04/xmldsig-more#rsa-sha224", RSA_SHA224);
			map.put("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", RSA_SHA256);
			map.put("http://www.w3.org/2001/04/xmldsig-more#rsa-sha384", RSA_SHA384);
			map.put("http://www.w3.org/2001/04/xmldsig-more#rsa-sha512", RSA_SHA512);
			map.put("http://www.w3.org/2001/04/xmldsig-more#rsa-ripemd160", RSA_RIPEMD160);
			map.put("http://www.w3.org/2001/04/xmldsig-more#rsa-md5", RSA_MD5);

			map.put("http://www.w3.org/2007/05/xmldsig-more#sha1-rsa-MGF1", RSA_SSA_PSS_SHA1_MGF1);
			map.put("http://www.w3.org/2007/05/xmldsig-more#sha224-rsa-MGF1", RSA_SSA_PSS_SHA224_MGF1);
			map.put("http://www.w3.org/2007/05/xmldsig-more#sha256-rsa-MGF1", RSA_SSA_PSS_SHA256_MGF1);
			map.put("http://www.w3.org/2007/05/xmldsig-more#sha384-rsa-MGF1", RSA_SSA_PSS_SHA384_MGF1);
			map.put("http://www.w3.org/2007/05/xmldsig-more#sha512-rsa-MGF1", RSA_SSA_PSS_SHA512_MGF1);
			map.put("http://www.w3.org/2007/05/xmldsig-more#sha3-224-rsa-MGF1", RSA_SSA_PSS_SHA3_224_MGF1);
			map.put("http://www.w3.org/2007/05/xmldsig-more#sha3-256-rsa-MGF1", RSA_SSA_PSS_SHA3_256_MGF1);
			map.put("http://www.w3.org/2007/05/xmldsig-more#sha3-384-rsa-MGF1", RSA_SSA_PSS_SHA3_384_MGF1);
			map.put("http://www.w3.org/2007/05/xmldsig-more#sha3-512-rsa-MGF1", RSA_SSA_PSS_SHA3_512_MGF1);
			map.put("http://www.w3.org/2007/05/xmldsig-more#ripemd160-rsa-MGF1", RSA_SSA_PSS_RIPEMD160_MGF1);
			map.put("http://www.w3.org/2007/05/xmldsig-more#md5-rsa-MGF1", RSA_SSA_PSS_MD5_MGF1);

			map.put("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha1", ECDSA_SHA1);
			map.put("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha224", ECDSA_SHA224);
			map.put("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256", ECDSA_SHA256);
			map.put("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384", ECDSA_SHA384);
			map.put("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha512", ECDSA_SHA512);
			map.put("http://www.w3.org/2021/04/xmldsig-more#ecdsa-sha3-224", ECDSA_SHA3_224);
			map.put("http://www.w3.org/2021/04/xmldsig-more#ecdsa-sha3-256", ECDSA_SHA3_256);
			map.put("http://www.w3.org/2021/04/xmldsig-more#ecdsa-sha3-384", ECDSA_SHA3_384);
			map.put("http://www.w3.org/2021/04/xmldsig-more#ecdsa-sha3-512", ECDSA_SHA3_512);
			map.put("http://www.w3.org/2007/05/xmldsig-more#ecdsa-ripemd160", ECDSA_RIPEMD160);

			map.put("http://www.w3.org/2000/09/xmldsig#dsa-sha1", DSA_SHA1);
			map.put("http://www.w3.org/2009/xmldsig11#dsa-sha256", DSA_SHA256);

//			map.put("http://www.w3.org/2021/04/xmldsig-more#ml-dsa-44", ML_DSA_44); // TODO draft
//			map.put("http://www.w3.org/2021/04/xmldsig-more#ml-dsa-65", ML_DSA_65); // TODO draft
//			map.put("http://www.w3.org/2021/04/xmldsig-more#ml-dsa-87", ML_DSA_87); // TODO draft
//
//			map.put("http://www.w3.org/2021/04/xmldsig-more#ml-dsa-44-sha512", ML_DSA_44_WITH_SHA512); // TODO draft
//  		map.put("http://www.w3.org/2021/04/xmldsig-more#ml-dsa-44-sha512", ML_DSA_65_WITH_SHA512); // TODO draft
//	  	map.put("http://www.w3.org/2021/04/xmldsig-more#ml-dsa-44-sha512", ML_DSA_87_WITH_SHA512); // TODO draft
			return map;
		}

		private static Map<String, SignatureAlgorithm> registerOidAlgorithms() {
			final Map<String, SignatureAlgorithm> map = new HashMap<>();
			map.put("1.2.840.113549.1.1.5", RSA_SHA1);
			map.put("1.3.14.3.2.29", RSA_SHA1);
			map.put("1.2.840.113549.1.1.14", RSA_SHA224);
			map.put("1.2.840.113549.1.1.11", RSA_SHA256);
			map.put("1.2.840.113549.1.1.12", RSA_SHA384);
			map.put("1.2.840.113549.1.1.13", RSA_SHA512);
			map.put("2.16.840.1.101.3.4.3.13", RSA_SHA3_224);
			map.put("2.16.840.1.101.3.4.3.14", RSA_SHA3_256);
			map.put("2.16.840.1.101.3.4.3.15", RSA_SHA3_384);
			map.put("2.16.840.1.101.3.4.3.16", RSA_SHA3_512);
			map.put("1.3.36.3.3.1.2", RSA_RIPEMD160);
			map.put("1.2.840.113549.1.1.4", RSA_MD5);

			map.put("1.2.840.113549.1.1.10", RSA_SSA_PSS_SHA1_MGF1);

			map.put("1.2.840.10045.4.1", ECDSA_SHA1);
			map.put("1.2.840.10045.4.3.1", ECDSA_SHA224);
			map.put("1.2.840.10045.4.3.2", ECDSA_SHA256);
			map.put("1.2.840.10045.4.3.3", ECDSA_SHA384);
			map.put("1.2.840.10045.4.3.4", ECDSA_SHA512);
			map.put("2.16.840.1.101.3.4.3.9", ECDSA_SHA3_224);
			map.put("2.16.840.1.101.3.4.3.10", ECDSA_SHA3_256);
			map.put("2.16.840.1.101.3.4.3.11", ECDSA_SHA3_384);
			map.put("2.16.840.1.101.3.4.3.12", ECDSA_SHA3_512);
			map.put("0.4.0.127.0.7.1.1.4.1.6", ECDSA_RIPEMD160);

			map.put("1.2.840.10040.4.3", DSA_SHA1);
			map.put("1.2.14888.3.0.1", DSA_SHA1);
			map.put("2.16.840.1.101.3.4.3.1", DSA_SHA224);
			map.put("2.16.840.1.101.3.4.3.2", DSA_SHA256);
			map.put("2.16.840.1.101.3.4.3.3", DSA_SHA384);
			map.put("2.16.840.1.101.3.4.3.4", DSA_SHA512);
			map.put("2.16.840.1.101.3.4.3.5", DSA_SHA3_224);
			map.put("2.16.840.1.101.3.4.3.6", DSA_SHA3_256);
			map.put("2.16.840.1.101.3.4.3.7", DSA_SHA3_384);
			map.put("2.16.840.1.101.3.4.3.8", DSA_SHA3_512);

			map.put("2.16.840.1.101.3.4.3.17", ML_DSA_44);
			map.put("2.16.840.1.101.3.4.3.18", ML_DSA_65);
			map.put("2.16.840.1.101.3.4.3.19", ML_DSA_87);

			map.put("2.16.840.1.101.3.4.3.32", ML_DSA_44_WITH_SHA512);
			map.put("2.16.840.1.101.3.4.3.33", ML_DSA_65_WITH_SHA512);
			map.put("2.16.840.1.101.3.4.3.34", ML_DSA_87_WITH_SHA512);
			return map;
		}

		private static Map<String, SignatureAlgorithm> registerJavaAlgorithms() {
			final Map<String, SignatureAlgorithm> map = new HashMap<>();
			map.put("NONEwithRSA", RSA_RAW);
			map.put("SHA1withRSA", RSA_SHA1);
			map.put("SHA224withRSA", RSA_SHA224);
			map.put("SHA256withRSA", RSA_SHA256);
			map.put("SHA384withRSA", RSA_SHA384);
			map.put("SHA512withRSA", RSA_SHA512);
			map.put("SHA3-224withRSA", RSA_SHA3_224);
			map.put("SHA3-256withRSA", RSA_SHA3_256);
			map.put("SHA3-384withRSA", RSA_SHA3_384);
			map.put("SHA3-512withRSA", RSA_SHA3_512);
			map.put("RIPEMD160withRSA", RSA_RIPEMD160);
			map.put("MD5withRSA", RSA_MD5);

			map.put("NONEwithRSAandMGF1", RSA_SSA_PSS_RAW_MGF1);
			map.put("SHA1withRSAandMGF1", RSA_SSA_PSS_SHA1_MGF1);
			map.put("SHA224withRSAandMGF1", RSA_SSA_PSS_SHA224_MGF1);
			map.put("SHA256withRSAandMGF1", RSA_SSA_PSS_SHA256_MGF1);
			map.put("SHA384withRSAandMGF1", RSA_SSA_PSS_SHA384_MGF1);
			map.put("SHA512withRSAandMGF1", RSA_SSA_PSS_SHA512_MGF1);
			map.put("SHA3-224withRSAandMGF1", RSA_SSA_PSS_SHA3_224_MGF1);
			map.put("SHA3-256withRSAandMGF1", RSA_SSA_PSS_SHA3_256_MGF1);
			map.put("SHA3-384withRSAandMGF1", RSA_SSA_PSS_SHA3_384_MGF1);
			map.put("SHA3-512withRSAandMGF1", RSA_SSA_PSS_SHA3_512_MGF1);
			map.put("RIPEMD160withRSAandMGF1", RSA_SSA_PSS_RIPEMD160_MGF1);
			map.put("MD5withRSAandMGF1", RSA_SSA_PSS_MD5_MGF1);

			map.put("NONEwithECDSA", ECDSA_RAW);
			map.put("SHA1withECDSA", ECDSA_SHA1);
			map.put("SHA224withECDSA", ECDSA_SHA224);
			map.put("SHA256withECDSA", ECDSA_SHA256);
			map.put("SHA384withECDSA", ECDSA_SHA384);
			map.put("SHA512withECDSA", ECDSA_SHA512);
			map.put("SHA3-224withECDSA", ECDSA_SHA3_224);
			map.put("SHA3-256withECDSA", ECDSA_SHA3_256);
			map.put("SHA3-384withECDSA", ECDSA_SHA3_384);
			map.put("SHA3-512withECDSA", ECDSA_SHA3_512);
			map.put("RIPEMD160withECDSA", ECDSA_RIPEMD160);

			map.put("NONEwithDSA", DSA_RAW);
			map.put("SHA1withDSA", DSA_SHA1);
			map.put("SHA224withDSA", DSA_SHA224);
			map.put("SHA256withDSA", DSA_SHA256);
			map.put("SHA384withDSA", DSA_SHA384);
			map.put("SHA512withDSA", DSA_SHA512);
			map.put("SHA3-224withDSA", DSA_SHA3_224);
			map.put("SHA3-256withDSA", DSA_SHA3_256);
			map.put("SHA3-384withDSA", DSA_SHA3_384);
			map.put("SHA3-512withDSA", DSA_SHA3_512);

			map.put("ML-DSA-44", ML_DSA_44);
			map.put("ML-DSA-65", ML_DSA_65);
			map.put("ML-DSA-87", ML_DSA_87);

			map.put("ML-DSA-44-WITH-SHA512", ML_DSA_44_WITH_SHA512);
			map.put("ML-DSA-65-WITH-SHA512", ML_DSA_65_WITH_SHA512);
			map.put("ML-DSA-87-WITH-SHA512", ML_DSA_87_WITH_SHA512);

			return map;
		}

	}

	/**
	 * This method return the {@code SignatureAlgorithm} for given algorithm string
	 *
	 * @param algorithm Algorithm name, java name, OID or XML URI of the given algorithm
	 * @return {@code SignatureAlgorithm} or default value
	 */
	public static SignatureAlgorithm forValue(final String algorithm) {
		SignatureAlgorithm signatureAlgorithm = Registry.JAVA_ALGORITHMS.get(algorithm);
		if (signatureAlgorithm == null) {
			signatureAlgorithm = Registry.XML_ALGORITHMS.get(algorithm);
			if (signatureAlgorithm == null) {
				signatureAlgorithm = Registry.OID_ALGORITHMS.get(algorithm);
				if (signatureAlgorithm == null) {
					throw new UnsupportedOperationException("Unsupported algorithm: "+ algorithm);
				}
			}
		}
		return signatureAlgorithm;
	}

	public static SignatureAlgorithm getAlgorithm(@NotNull final EncryptionAlgorithm encryptionAlgorithm) {
		return getAlgorithm(encryptionAlgorithm, null, null);
	}

	public static SignatureAlgorithm getAlgorithm(@NotNull final EncryptionAlgorithm encryptionAlgorithm,
	                                              final DigestAlgorithm digestAlgorithm) {
		return getAlgorithm(encryptionAlgorithm, digestAlgorithm, null);
	}

	public static SignatureAlgorithm getAlgorithm(@NotNull final EncryptionAlgorithm encryptionAlgorithm,
	                                              final DigestAlgorithm digestAlgorithm,
	                                              final MaskGenerationFunction mgf) {
		StringBuilder sb = new StringBuilder();
		if (encryptionAlgorithm.getName().startsWith("ML-DSA")) {
			return SignatureAlgorithm.forValue(encryptionAlgorithm.getName());
		} else {
			if (digestAlgorithm != null) {
				sb.append(digestAlgorithm.getName());
				sb.append("with");
			}
			sb.append(encryptionAlgorithm.getName());
			if (mgf != null) {
				sb.append("andMGF1");
			}
		}
		return Registry.JAVA_ALGORITHMS.get(sb.toString());
	}

	public EncryptionAlgorithm getEncryptionAlgorithm() {
		return encryptionAlgo;
	}

	public DigestAlgorithm getDigestAlgorithm() {
		return digestAlgo;
	}

	public MaskGenerationFunction getMaskGenerationFunction() {
		return maskGenerationFunction;
	}

	public String getXMLId() {
		for (Entry<String, SignatureAlgorithm> e : Registry.XML_ALGORITHMS.entrySet()) {
			if (this.equals(e.getValue())) {
				return e.getKey();
			}
		}
		return null;
	}

	public String getOID() {
		for (Entry<String, SignatureAlgorithm> e : Registry.OID_ALGORITHMS.entrySet()) {
			if (this.equals(e.getValue())) {
				return e.getKey();
			}
		}
		return null;
	}

	public String getJCEId() {
		for (Entry<String, SignatureAlgorithm> e : Registry.JAVA_ALGORITHMS.entrySet()) {
			if (this.equals(e.getValue())) {
				return e.getKey();
			}
		}
		return null;
	}

}

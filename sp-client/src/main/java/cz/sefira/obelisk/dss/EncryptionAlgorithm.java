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

import java.util.HashMap;
import java.util.Map;

public enum EncryptionAlgorithm {

	RSA("RSA", "1.2.840.113549.1.1.1", "RSA/ECB/PKCS1Padding"),
	DSA("DSA", "1.2.840.10040.4.1", "DSA"),
	ECDSA("ECDSA", "1.2.840.10045.2.1", "ECDSA"),
	ML_DSA_44("ML-DSA-44", "2.16.840.1.101.3.4.3.17", "MLDSA"),
	ML_DSA_65("ML-DSA-65", "2.16.840.1.101.3.4.3.18", "MLDSA"),
	ML_DSA_87("ML-DSA-87", "2.16.840.1.101.3.4.3.19", "MLDSA"),
	ML_DSA_44_SHA512("ML-DSA-44-WITH-SHA512", "2.16.840.1.101.3.4.3.32", "MLDSA"),
	ML_DSA_65_SHA512("ML-DSA-65-WITH-SHA512", "2.16.840.1.101.3.4.3.33", "MLDSA"),
	ML_DSA_87_SHA512("ML-DSA-87-WITH-SHA512", "2.16.840.1.101.3.4.3.34", "MLDSA");

	private final String name;
	private final String oid;
	private final String padding;

	EncryptionAlgorithm(String name, String oid, String padding) {
		this.name = name;
		this.oid = oid;
		this.padding = padding;
	}

	private static class Registry {

		private static final Map<String, EncryptionAlgorithm> ALGORITHMS = registerAlgorithms();

		private static Map<String, EncryptionAlgorithm> registerAlgorithms() {
			Map<String, EncryptionAlgorithm> map = new HashMap<>();
			for (EncryptionAlgorithm encryptionAlgorithm : values()) {
				map.put(encryptionAlgorithm.oid, encryptionAlgorithm);
				map.put(encryptionAlgorithm.name, encryptionAlgorithm);
			}
			// EC + ECC
			map.put("EC", ECDSA);
			map.put("ECC", ECDSA);
			// org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey.getAlgorithm()
			map.put("RSASSA-PSS", RSA);
			return map;
		}
	}

	public static EncryptionAlgorithm forValue(String encryptionAlg) {
		EncryptionAlgorithm encryptionAlgorithm = Registry.ALGORITHMS.get(encryptionAlg);
		if (encryptionAlgorithm == null) {
			throw new UnsupportedOperationException("Unsupported algorithm: "+ encryptionAlg);
		}
		return encryptionAlgorithm;
	}

	public String getName() {
		return name;
	}

	public String getOid() {
		return oid;
	}

	public String getPadding() {
		return padding;
	}

}
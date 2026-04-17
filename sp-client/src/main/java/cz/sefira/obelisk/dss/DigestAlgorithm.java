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

/**
 * Supported Algorithms
 *
 */
public enum DigestAlgorithm {

	NONE("NONE", "NONE", "", "", -1),

	SHA1("SHA1", "SHA-1","1.3.14.3.2.26", "http://www.w3.org/2000/09/xmldsig#sha1", 20),
	SHA224("SHA224", "SHA-224","2.16.840.1.101.3.4.2.4", "http://www.w3.org/2001/04/xmldsig-more#sha224", 28),
	SHA256("SHA256", "SHA-256","2.16.840.1.101.3.4.2.1", "http://www.w3.org/2001/04/xmlenc#sha256", 32),
	SHA384("SHA384", "SHA-384","2.16.840.1.101.3.4.2.2", "http://www.w3.org/2001/04/xmldsig-more#sha384", 48),
	SHA512("SHA512", "SHA-512","2.16.840.1.101.3.4.2.3", "http://www.w3.org/2001/04/xmlenc#sha512", 64),

	SHA3_224("SHA3-224", "SHA3-224", "2.16.840.1.101.3.4.2.7", "http://www.w3.org/2007/05/xmldsig-more#sha3-224", 28),
	SHA3_256("SHA3-256", "SHA3-256", "2.16.840.1.101.3.4.2.8", "http://www.w3.org/2007/05/xmldsig-more#sha3-256", 32),
	SHA3_384("SHA3-384", "SHA3-384", "2.16.840.1.101.3.4.2.9", "http://www.w3.org/2007/05/xmldsig-more#sha3-384", 48),
	SHA3_512("SHA3-512", "SHA3-512", "2.16.840.1.101.3.4.2.10", "http://www.w3.org/2007/05/xmldsig-more#sha3-512", 64),

	RIPEMD160("RIPEMD160", "RIPEMD160", "1.3.36.3.2.1", "http://www.w3.org/2001/04/xmlenc#ripemd160"),
	MD5("MD5", "MD5", "1.2.840.113549.2.5", "http://www.w3.org/2001/04/xmldsig-more#md5");

	private final String name;
	private final String javaName;
	private final String oid;
	private final String xmlId;
	private final int saltLength;

	DigestAlgorithm(final String name, final String javaName, final String oid, final String xmlId) {
		this(name, javaName, oid, xmlId, 0);
	}

	DigestAlgorithm(final String name, final String javaName, final String oid, final String xmlId, final int saltLength) {
		this.name = name;
		this.javaName = javaName;
		this.oid = oid;
		this.xmlId = xmlId;
		this.saltLength = saltLength;
	}

	private static class Registry {

		private static final Map<String, DigestAlgorithm> ALGORITHMS = registerAlgorithms();

		private static Map<String, DigestAlgorithm> registerAlgorithms() {
			final Map<String, DigestAlgorithm> map = new HashMap<>();
			for (final DigestAlgorithm digestAlgorithm : values()) {
				map.putIfAbsent(digestAlgorithm.name, digestAlgorithm);
				map.putIfAbsent(digestAlgorithm.javaName, digestAlgorithm);
				map.putIfAbsent(digestAlgorithm.oid, digestAlgorithm);
				map.putIfAbsent(digestAlgorithm.xmlId, digestAlgorithm);
			}
			return map;
		}

	}

	public static DigestAlgorithm forValue(String digestMethod) {
		DigestAlgorithm digestAlgorithm = Registry.ALGORITHMS.get(digestMethod);
		if (digestAlgorithm == null) {
			throw new UnsupportedOperationException("Unsupported algorithm: "+ digestMethod);
		}
		return digestAlgorithm;
	}

	public String getName() {
		return name;
	}

	public String getJavaName() {
		return javaName;
	}

	public String getOid() {
		return oid;
	}

	public String getXmlId() {
		return xmlId;
	}

	public int getSaltLength() {
		return saltLength;
	}

}

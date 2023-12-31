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

import cz.sefira.obelisk.dss.Digest;
import cz.sefira.obelisk.dss.DigestAlgorithm;
import org.apache.commons.codec.binary.Hex;

/**
 * This class is used to obtain a unique id for Token
 */
public final class TokenIdentifier implements Serializable {

	private final Digest tokenDigest;

	TokenIdentifier(DigestAlgorithm digestAlgo, byte[] digest) {
		this.tokenDigest = new Digest(digestAlgo, digest);
	}

	public TokenIdentifier(final Token token) {
		this(DigestAlgorithm.SHA256, token.getDigest(DigestAlgorithm.SHA256));
	}

	/**
	 * Return an ID conformant to XML Id
	 * 
	 * @return the XML encoded ID
	 */
	public String asXmlId() {
		return Hex.encodeHexString(tokenDigest.getValue(), false);
	}

	@Override
	public String toString() {
		return "{id:" + tokenDigest + "}";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((tokenDigest == null) ? 0 : tokenDigest.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TokenIdentifier other = (TokenIdentifier) obj;
		if (tokenDigest == null) {
			return other.tokenDigest == null;
		} else return tokenDigest.equals(other.tokenDigest);
	}

}

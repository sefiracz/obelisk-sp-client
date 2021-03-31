/**
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package lu.nowina.nexu.api;

/*
 * Copyright 2020 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.api.GetTokenResponse
 *
 * Created: 16.12.2020
 * Author: hlavnicka
 */

import eu.europa.esig.dss.EncryptionAlgorithm;
import eu.europa.esig.dss.x509.CertificateToken;

public class GetTokenResponse {

	private TokenId tokenId;

	private String keyId;

	private CertificateToken certificate;

	private CertificateToken[] certificateChain;

	private EncryptionAlgorithm encryptionAlgorithm;

	public GetTokenResponse() {
		super();
	}

	public TokenId getTokenId() {
		return tokenId;
	}

	public void setTokenId(TokenId tokenId) {
		this.tokenId = tokenId;
	}

	public String getKeyId() {
		return keyId;
	}

	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}

	public CertificateToken getCertificate() {
		return certificate;
	}

	public void setCertificate(CertificateToken certificate) {
		this.certificate = certificate;
	}

	public CertificateToken[] getCertificateChain() {
		return certificateChain;
	}

	public void setCertificateChain(CertificateToken[] certificateChain) {
		this.certificateChain = certificateChain;
	}

	public EncryptionAlgorithm getEncryptionAlgorithm() {
		return encryptionAlgorithm;
	}

	public void setEncryptionAlgorithm(EncryptionAlgorithm encryptionAlgorithm) {
		this.encryptionAlgorithm = encryptionAlgorithm;
	}

}

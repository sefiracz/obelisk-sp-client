/**
 * © Nowina Solutions, 2015-2015
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package cz.sefira.obelisk.api.ws.model;

import cz.sefira.obelisk.dss.x509.CertificateToken;

public class CertificateFilter {

	/**
	 * if true the expiration validity is not checked. If false, only certificate with valid date will be
	 * returned.
	 */
	private Boolean allowExpired = false;

	/**
	 * if false the nonRepudiation bit is not checked. If true, only certificate with nonRepudiationKeyUsage will be 
	 * returned.
	 */
	private Boolean nonRepudiationBit = true;

	/**
	 * if false the digitalSignature bit is not checked. If true, only certificate with digitalSignatureKeyUsage will be
	 * returned.
	 */
	private Boolean digitalSignatureBit = false;

	/**
	 * if null the certificate SHA256 digest is not checked. If value is set, only certificate with equal digest will be
	 * returned.
	 */
	private byte[] certificateId;

	/**
	 * if null the certificate issuer is not checked. If value is set, only certificate issued by given CA certificate
	 * will be returned.
	 */
	private CertificateToken issuer;

	public CertificateFilter() {
		
	}
	
	public CertificateFilter(byte[] certificateSHA256) {
		this.certificateId = certificateSHA256;
	}

	public CertificateFilter(CertificateToken issuer) {
		this.issuer = issuer;
	}

	public CertificateToken getIssuer() {
		return issuer;
	}

	public void setIssuer(CertificateToken issuer) {
		this.issuer = issuer;
	}

	public byte[] getCertificateId() {
		return certificateId;
	}

	public void setCertificateId(byte[] certificateId) {
		this.certificateId = certificateId;
	}

	public Boolean getNonRepudiationBit() {
		return nonRepudiationBit;
	}

	public void setNonRepudiationBit(Boolean nonRepudiationBit) {
		this.nonRepudiationBit = nonRepudiationBit;
	}

	public Boolean getDigitalSignatureBit() {
		return digitalSignatureBit;
	}

	public void setDigitalSignatureBit(Boolean digitalSignatureBit) {
		this.digitalSignatureBit = digitalSignatureBit;
	}

	public Boolean getAllowExpired() {
		return allowExpired;
	}

	public void setAllowExpired(Boolean allowExpired) {
		this.allowExpired = allowExpired;
	}
}

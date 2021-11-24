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
package cz.sefira.obelisk.api;

public class CertificateFilter {

	private Purpose purpose;

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
	 * if null the certificate SHA1 digest is not checked. If value is set, only certificate with equal digest will be
	 * returned.
	 */
	private byte[] certificateSHA1;

	public CertificateFilter() {
		
	}
	
	public CertificateFilter(Purpose purpose) {
		this.purpose = purpose;
	}
	
	public CertificateFilter(byte[] certificateSHA1) {
		this.certificateSHA1 = certificateSHA1;
	}
	
	public Purpose getPurpose() {
		return purpose;
	}

	public void setPurpose(Purpose purpose) {
		this.purpose = purpose;
	}

	public byte[] getCertificateSHA1() {
		return certificateSHA1;
	}

	public void setCertificateSHA1(byte[] certificateSHA1) {
		this.certificateSHA1 = certificateSHA1;
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

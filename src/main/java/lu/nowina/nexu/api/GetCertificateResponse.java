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
package lu.nowina.nexu.api;

import eu.europa.esig.dss.EncryptionAlgorithm;
import eu.europa.esig.dss.x509.CertificateToken;

import java.math.BigInteger;

public class GetCertificateResponse {

	private CertificateToken certificate;

	private CertificateToken[] certificateChain;

	private EncryptionAlgorithm encryptionAlgorithm;

	private String subjectCN;

	private String subjectOrg;

	private String notBefore;

	private String notAfter;

	private BigInteger serialNumber;

	private String issuerCN;

	private String issuerOrg;

	public GetCertificateResponse() {
		super();
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

	public String getSubjectCN() {
		return subjectCN;
	}

	public void setSubjectCN(String subjectCN) {
		this.subjectCN = subjectCN;
	}

	public String getSubjectOrg() {
		return subjectOrg;
	}

	public void setSubjectOrg(String subjectOrg) {
		this.subjectOrg = subjectOrg;
	}

	public String getNotBefore() {
		return notBefore;
	}

	public void setNotBefore(String notBefore) {
		this.notBefore = notBefore;
	}

	public String getNotAfter() {
		return notAfter;
	}

	public void setNotAfter(String notAfter) {
		this.notAfter = notAfter;
	}

	public BigInteger getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(BigInteger serialNumber) {
		this.serialNumber = serialNumber;
	}

	public String getIssuerCN() {
		return issuerCN;
	}

	public void setIssuerCN(String issuerCN) {
		this.issuerCN = issuerCN;
	}

	public String getIssuerOrg() {
		return issuerOrg;
	}

	public void setIssuerOrg(String issuerOrg) {
		this.issuerOrg = issuerOrg;
	}
}

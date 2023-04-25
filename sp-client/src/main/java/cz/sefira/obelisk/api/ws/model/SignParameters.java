package cz.sefira.obelisk.api.ws.model;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.ws.model.SignatureInput
 *
 * Created: 18.04.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.dss.DigestAlgorithm;
import cz.sefira.obelisk.dss.x509.CertificateToken;

public class SignParameters {

  private Long stepId;

  private CertificateToken certificate;

  private byte[] toBeSigned;

  private DigestAlgorithm digestAlgorithm;

  private boolean useRsaPss;

  public Long getStepId() {
    return stepId;
  }

  public void setStepId(Long stepId) {
    this.stepId = stepId;
  }

  public CertificateToken getCertificate() {
    return certificate;
  }

  public void setCertificate(CertificateToken certificate) {
    this.certificate = certificate;
  }

  public byte[] getToBeSigned() {
    return toBeSigned;
  }

  public void setToBeSigned(byte[] toBeSigned) {
    this.toBeSigned = toBeSigned;
  }

  public DigestAlgorithm getDigestAlgorithm() {
    return digestAlgorithm;
  }

  public void setDigestAlgorithm(DigestAlgorithm digestAlgorithm) {
    this.digestAlgorithm = digestAlgorithm;
  }

  public boolean isUseRsaPss() {
    return useRsaPss;
  }

  public void setUseRsaPss(boolean useRsaPss) {
    this.useRsaPss = useRsaPss;
  }

}

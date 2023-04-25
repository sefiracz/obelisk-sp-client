package cz.sefira.obelisk.api;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.AbstractProduct
 *
 * Created: 05.01.2021
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.model.KeystoreType;
public abstract class AbstractProduct implements Product {

  protected String certificateId;
  protected String certificate;
  protected KeystoreType type;
  protected String keyAlias;

  public AbstractProduct() {
  }

  /**
   * Returns the ID (SHA256 hex digest) of used certificate.
   *
   * @return The ID (SHA256 hex digest) of used certificate.
   */
  public String getCertificateId() {
    return certificateId;
  }

  /**
   * Sets the ID (SHA256 hex digest) of used certificate.
   *
   * @param certificateId The ID (SHA256 hex digest) of used certificate.
   */
  public void setCertificateId(String certificateId) {
    this.certificateId = certificateId;
  }

  /**
   * Returns the base64 encoded X509Certificate.
   *
   * @return The base64 encoded X509Certificate.
   */
  public String getCertificate() {
    return certificate;
  }

  /**
   * Sets the base64 encoded X509Certificate.
   *
   * @param certificate Base64 encoded X509Certificate
   */
  public void setCertificate(String certificate) {
    this.certificate = certificate;
  }

  /**
   * Returns the type of the keystore.
   *
   * @return The type of the keystore.
   */
  public KeystoreType getType() {
    return type;
  }

  /**
   * Sets the type of the keystore.
   *
   * @param type The type of the keystore.
   */
  public void setType(KeystoreType type) {
    this.type = type;
  }

  /**
   * Returns the alias of used private key from the keystore.
   *
   * @return The alias of used private key from the keystore.
   */
  public String getKeyAlias() {
    return keyAlias;
  }

  /**
   * Sets the alias of used private key from the keystore.
   *
   * @param keyAlias The alias of used private key from the keystore.
   */
  public void setKeyAlias(String keyAlias) {
    this.keyAlias = keyAlias;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AbstractProduct that = (AbstractProduct) o;

    if (getCertificateId() == null || !getCertificateId().equalsIgnoreCase(that.getCertificateId())) return false;
    return (getCertificate() == null && that.getCertificate() == null) ||
        (getCertificate().equals(that.getCertificate()));
  }

  @Override
  public int hashCode() {
    int result = getCertificateId().hashCode();
    result = 31 * result + getKeyAlias().hashCode();
    return result;
  }

}

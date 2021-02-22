package lu.nowina.nexu.api;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.api.AbstractProduct
 *
 * Created: 05.01.2021
 * Author: hlavnicka
 */

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"certificateId", "type", "keyAlias", "certificate" })
public abstract class AbstractProduct implements Product {

  protected String certificateId;
  protected String certificate;
  protected KeystoreType type;
  protected String keyAlias;
  @XmlTransient
  protected boolean toBeSaved;
  @XmlTransient
  protected String sessionId;

  public AbstractProduct() {
    this.toBeSaved = false;
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

  /**
   * Returns <code>true</code> if the <code>Product</code> must be saved and
   * <code>false</code> otherwise.
   *
   * @return <code>true</code> if the <code>Product</code> must be saved and
   * <code>false</code> otherwise.
   */
  public boolean isToBeSaved() {
    return toBeSaved;
  }

  /**
   * Sets the value of the <code>toBeSaved</code> property.
   *
   * @param toBeSaved The new value for the <code>toBeSaved</code> property.
   */
  public void setToBeSaved(boolean toBeSaved) {
    this.toBeSaved = toBeSaved;
  }

  /**
   * Returns session identification
   *
   * @return Session identification
   */
  public String getSessionId() {
    return sessionId;
  }

  /**
   * Sets session identification
   *
   * @param sessionId Session identification value
   */
  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AbstractProduct that = (AbstractProduct) o;

    if (!getCertificateId().equals(that.getCertificateId())) return false;
    return getKeyAlias().equals(that.getKeyAlias());
  }

  @Override
  public int hashCode() {
    int result = getCertificateId().hashCode();
    result = 31 * result + getKeyAlias().hashCode();
    return result;
  }

}
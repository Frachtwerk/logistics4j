package de.frachtwerk.xmlstatus4j;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class VersenderDaten {
  @JacksonXmlProperty(localName = "IDNR")
  private String adressnummer;

  @JacksonXmlProperty(localName = "NAM1")
  private String name1;

  @JacksonXmlProperty(localName = "NAM2")
  private String name2;

  @JacksonXmlProperty(localName = "STHS")
  private String strasseUndHausnummer;

  @JacksonXmlProperty(localName = "LAND")
  private String land;

  @JacksonXmlProperty(localName = "PLZ")
  private String postleitzahl;

  @JacksonXmlProperty(localName = "ORT")
  private String ort;

  @JacksonXmlProperty(localName = "TELE")
  private String telefonnummer;

  @JacksonXmlProperty(localName = "MAIL")
  private String email;

  @JacksonXmlProperty(localName = "BELA")
  private String beladeOrt;

  @JacksonXmlProperty(localName = "REFE")
  private String referenz;

  public String getAdressnummer() {
    return adressnummer;
  }

  public void setAdressnummer(String adressnummer) {
    this.adressnummer = adressnummer;
  }

  public String getName1() {
    return name1;
  }

  public void setName1(String name1) {
    this.name1 = name1;
  }

  public String getName2() {
    return name2;
  }

  public void setName2(String name2) {
    this.name2 = name2;
  }

  public String getStrasseUndHausnummer() {
    return strasseUndHausnummer;
  }

  public void setStrasseUndHausnummer(String strasseUndHausnummer) {
    this.strasseUndHausnummer = strasseUndHausnummer;
  }

  public String getLand() {
    return land;
  }

  public void setLand(String land) {
    this.land = land;
  }

  public String getPostleitzahl() {
    return postleitzahl;
  }

  public void setPostleitzahl(String postleitzahl) {
    this.postleitzahl = postleitzahl;
  }

  public String getOrt() {
    return ort;
  }

  public void setOrt(String ort) {
    this.ort = ort;
  }

  public String getTelefonnummer() {
    return telefonnummer;
  }

  public void setTelefonnummer(String telefonnummer) {
    this.telefonnummer = telefonnummer;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getBeladeOrt() {
    return beladeOrt;
  }

  public void setBeladeOrt(String beladeOrt) {
    this.beladeOrt = beladeOrt;
  }

  public String getReferenz() {
    return referenz;
  }

  public void setReferenz(String referenz) {
    this.referenz = referenz;
  }
}

package de.frachtwerk.xmlstatus4j;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class FahrzeugDaten {
  @JacksonXmlProperty(localName = "FAZ_NR01")
  private String fahrzeug1Nummer;

  @JacksonXmlProperty(localName = "FAZ_KZ01")
  private String fahrzeug1Kennzeichen;

  @JacksonXmlProperty(localName = "FAZ_PL01")
  private String fahrzeug1Plombe;

  @JacksonXmlProperty(localName = "FAZ_NR02")
  private String fahrzeug2Nummer;

  @JacksonXmlProperty(localName = "FAZ_KZ02")
  private String fahrzeug2Kennzeichen;

  @JacksonXmlProperty(localName = "FAZ_PL02")
  private String fahrzeug2Plombe;

  public String getFahrzeug1Nummer() {
    return fahrzeug1Nummer;
  }

  public void setFahrzeug1Nummer(String fahrzeug1Nummer) {
    this.fahrzeug1Nummer = fahrzeug1Nummer;
  }

  public String getFahrzeug1Kennzeichen() {
    return fahrzeug1Kennzeichen;
  }

  public void setFahrzeug1Kennzeichen(String fahrzeug1Kennzeichen) {
    this.fahrzeug1Kennzeichen = fahrzeug1Kennzeichen;
  }

  public String getFahrzeug1Plombe() {
    return fahrzeug1Plombe;
  }

  public void setFahrzeug1Plombe(String fahrzeug1Plombe) {
    this.fahrzeug1Plombe = fahrzeug1Plombe;
  }

  public String getFahrzeug2Nummer() {
    return fahrzeug2Nummer;
  }

  public void setFahrzeug2Nummer(String fahrzeug2Nummer) {
    this.fahrzeug2Nummer = fahrzeug2Nummer;
  }

  public String getFahrzeug2Kennzeichen() {
    return fahrzeug2Kennzeichen;
  }

  public void setFahrzeug2Kennzeichen(String fahrzeug2Kennzeichen) {
    this.fahrzeug2Kennzeichen = fahrzeug2Kennzeichen;
  }

  public String getFahrzeug2Plombe() {
    return fahrzeug2Plombe;
  }

  public void setFahrzeug2Plombe(String fahrzeug2Plombe) {
    this.fahrzeug2Plombe = fahrzeug2Plombe;
  }
}

package de.frachtwerk.xmlstatus4j;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class SendungSummen {
  @JacksonXmlProperty(localName = "SSU_ANZA")
  private String anzahl;

  @JacksonXmlProperty(localName = "SSU_GEWI")
  private String gewicht;

  public String getAnzahl() {
    return anzahl;
  }

  public void setAnzahl(String anzahl) {
    this.anzahl = anzahl;
  }

  public String getGewicht() {
    return gewicht;
  }

  public void setGewicht(String gewicht) {
    this.gewicht = gewicht;
  }
}

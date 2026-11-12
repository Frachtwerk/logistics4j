package de.frachtwerk.xmlstatus4j;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Lademittel {

  @JacksonXmlProperty(localName = "LDM_ART")
  private String lademittelArt;

  @JacksonXmlProperty(localName = "LDM_HIN")
  private String lademittelAnzahlAusgabe;

  @JacksonXmlProperty(localName = "LDM_WEG")
  private String lademittelAnzahlRuecknahme;

  public String getLademittelArt() {
    return lademittelArt;
  }

  public void setLademittelArt(String lademittelArt) {
    this.lademittelArt = lademittelArt;
  }

  public String getLademittelAnzahlAusgabe() {
    return lademittelAnzahlAusgabe;
  }

  public void setLademittelAnzahlAusgabe(String lademittelAnzahlAusgabe) {
    this.lademittelAnzahlAusgabe = lademittelAnzahlAusgabe;
  }

  public String getLademittelAnzahlRuecknahme() {
    return lademittelAnzahlRuecknahme;
  }

  public void setLademittelAnzahlRuecknahme(String lademittelAnzahlRuecknahme) {
    this.lademittelAnzahlRuecknahme = lademittelAnzahlRuecknahme;
  }
}

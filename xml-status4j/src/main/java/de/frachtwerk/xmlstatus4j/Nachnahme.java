package de.frachtwerk.xmlstatus4j;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Nachnahme {
  @JacksonXmlProperty(localName = "NAC_BETR")
  private String betrag;

  @JacksonXmlProperty(localName = "NAC_ART")
  private String art;

  @JacksonXmlProperty(localName = "NAC_WAEH")
  private String waehrung;

  @JacksonXmlProperty(localName = "NAC_BEZA")
  private String bezahlt;

  public String getBetrag() {
    return betrag;
  }

  public void setBetrag(String betrag) {
    this.betrag = betrag;
  }

  public String getArt() {
    return art;
  }

  public void setArt(String art) {
    this.art = art;
  }

  public String getWaehrung() {
    return waehrung;
  }

  public void setWaehrung(String waehrung) {
    this.waehrung = waehrung;
  }

  public String getBezahlt() {
    return bezahlt;
  }

  public void setBezahlt(String bezahlt) {
    this.bezahlt = bezahlt;
  }
}

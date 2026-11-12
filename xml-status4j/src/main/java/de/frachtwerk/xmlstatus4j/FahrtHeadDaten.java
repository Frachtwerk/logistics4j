package de.frachtwerk.xmlstatus4j;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class FahrtHeadDaten {
  @JacksonXmlProperty(localName = "FAR_FAHR")
  private String rollkartenNr;

  @JacksonXmlProperty(localName = "FAR_TSBE")
  private String fahrtBeginn;

  @JacksonXmlProperty(localName = "FAR_TSEN")
  private String fahrtEnde;

  @JacksonXmlProperty(localName = "FADA")
  private FahrerDaten fahrerDaten;

  @JacksonXmlProperty(localName = "FAZ")
  private FahrzeugDaten fahrzeugDaten;

  @JacksonXmlProperty(localName = "HOF")
  private HofDaten hofDaten;

  @JacksonXmlProperty(localName = "WAB")
  private Wechselbruecke wechselbruecke;

  public String getRollkartenNr() {
    return rollkartenNr;
  }

  public void setRollkartenNr(String rollkartenNr) {
    this.rollkartenNr = rollkartenNr;
  }

  public String getFahrtBeginn() {
    return fahrtBeginn;
  }

  public void setFahrtBeginn(String fahrtBeginn) {
    this.fahrtBeginn = fahrtBeginn;
  }

  public String getFahrtEnde() {
    return fahrtEnde;
  }

  public void setFahrtEnde(String fahrtEnde) {
    this.fahrtEnde = fahrtEnde;
  }

  public FahrerDaten getFahrerDaten() {
    return fahrerDaten;
  }

  public void setFahrerDaten(FahrerDaten fahrerDaten) {
    this.fahrerDaten = fahrerDaten;
  }

  public FahrzeugDaten getFahrzeugDaten() {
    return fahrzeugDaten;
  }

  public void setFahrzeugDaten(FahrzeugDaten fahrzeugDaten) {
    this.fahrzeugDaten = fahrzeugDaten;
  }

  public HofDaten getHofDaten() {
    return hofDaten;
  }

  public void setHofDaten(HofDaten hofDaten) {
    this.hofDaten = hofDaten;
  }

  public Wechselbruecke getWechselbruecke() {
    return wechselbruecke;
  }

  public void setWechselbruecke(Wechselbruecke wechselbruecke) {
    this.wechselbruecke = wechselbruecke;
  }
}

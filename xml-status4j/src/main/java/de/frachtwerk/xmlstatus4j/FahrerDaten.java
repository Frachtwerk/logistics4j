package de.frachtwerk.xmlstatus4j;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class FahrerDaten {
  @JacksonXmlProperty(localName = "FAR_FANR")
  private String adressnummer;

  @JacksonXmlProperty(localName = "FAR_FANA")
  private String name;

  @JacksonXmlProperty(localName = "FAR_FAID")
  private String firmenIdLBase;

  @JacksonXmlProperty(localName = "FAR_AGID")
  private String arbeitsgruppenIdLBase;

  public String getAdressnummer() {
    return adressnummer;
  }

  public void setAdressnummer(String adressnummer) {
    this.adressnummer = adressnummer;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getFirmenIdLBase() {
    return firmenIdLBase;
  }

  public void setFirmenIdLBase(String firmenIdLBase) {
    this.firmenIdLBase = firmenIdLBase;
  }

  public String getArbeitsgruppenIdLBase() {
    return arbeitsgruppenIdLBase;
  }

  public void setArbeitsgruppenIdLBase(String arbeitsgruppenIdLBase) {
    this.arbeitsgruppenIdLBase = arbeitsgruppenIdLBase;
  }
}

package de.frachtwerk.xmlstatus4j;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class HofDaten {
  @JacksonXmlProperty(localName = "HOF_ABFA")
  private String abfahrtszeit;

  @JacksonXmlProperty(localName = "HOF_ANKU")
  private String ankunfszeit;

  @JacksonXmlProperty(localName = "HOF_WART")
  private String wartezeit;

  @JacksonXmlProperty(localName = "HOF_CLSUZ")
  private String collisummeZustellung;

  @JacksonXmlProperty(localName = "HOF_CLSUA")
  private String collisummeAbholung;

  public String getAbfahrtszeit() {
    return abfahrtszeit;
  }

  public void setAbfahrtszeit(String abfahrtszeit) {
    this.abfahrtszeit = abfahrtszeit;
  }

  public String getAnkunfszeit() {
    return ankunfszeit;
  }

  public void setAnkunfszeit(String ankunfszeit) {
    this.ankunfszeit = ankunfszeit;
  }

  public String getWartezeit() {
    return wartezeit;
  }

  public void setWartezeit(String wartezeit) {
    this.wartezeit = wartezeit;
  }

  public String getCollisummeZustellung() {
    return collisummeZustellung;
  }

  public void setCollisummeZustellung(String collisummeZustellung) {
    this.collisummeZustellung = collisummeZustellung;
  }

  public String getCollisummeAbholung() {
    return collisummeAbholung;
  }

  public void setCollisummeAbholung(String collisummeAbholung) {
    this.collisummeAbholung = collisummeAbholung;
  }
}

package de.frachtwerk.xmlstatus4j;

import java.util.List;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Fahrt {
  @JacksonXmlProperty(localName = "FAR")
  private FahrtHeadDaten fahrtHeadDaten;

  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "SDG")
  private List<Sendungsdaten> sendungsdaten;

  public FahrtHeadDaten getFahrtHeadDaten() {
    return fahrtHeadDaten;
  }

  public void setFahrtHeadDaten(FahrtHeadDaten fahrtHeadDaten) {
    this.fahrtHeadDaten = fahrtHeadDaten;
  }

  public List<Sendungsdaten> getSendungsdaten() {
    return sendungsdaten;
  }

  public void setSendungsdaten(List<Sendungsdaten> sendungsdaten) {
    this.sendungsdaten = sendungsdaten;
  }
}

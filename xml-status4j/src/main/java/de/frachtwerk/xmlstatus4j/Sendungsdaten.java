package de.frachtwerk.xmlstatus4j;

import java.util.List;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Sendungsdaten {
  @JacksonXmlProperty(localName = "SDG_HEAD")
  private SendungsdatenKopf sendungsdatenKopf;

  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "POSI")
  private List<PositionsDaten> positionsDaten;

  public SendungsdatenKopf getSendungsdatenKopf() {
    return sendungsdatenKopf;
  }

  public void setSendungsdatenKopf(SendungsdatenKopf sendungsdatenKopf) {
    this.sendungsdatenKopf = sendungsdatenKopf;
  }

  public List<PositionsDaten> getPositionsDaten() {
    return positionsDaten;
  }

  public void setPositionsDaten(List<PositionsDaten> positionsDaten) {
    this.positionsDaten = positionsDaten;
  }
}

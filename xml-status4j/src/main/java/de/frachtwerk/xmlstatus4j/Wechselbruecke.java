package de.frachtwerk.xmlstatus4j;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Wechselbruecke {
  @JacksonXmlProperty(localName = "WAB_WB01")
  private String wechselbruecke1;

  @JacksonXmlProperty(localName = "WAB_WB02")
  private String wechselbruecke2;

  public String getWechselbruecke1() {
    return wechselbruecke1;
  }

  public void setWechselbruecke1(String wechselbruecke1) {
    this.wechselbruecke1 = wechselbruecke1;
  }

  public String getWechselbruecke2() {
    return wechselbruecke2;
  }

  public void setWechselbruecke2(String wechselbruecke2) {
    this.wechselbruecke2 = wechselbruecke2;
  }
}

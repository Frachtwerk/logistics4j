/*
 * Copyright (C) 2026 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
 *
 * This file is part of logistics4j.
 *
 * logistics4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * logistics4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with logistics4j. If not, see <http://www.gnu.org/licenses/>.
 */

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

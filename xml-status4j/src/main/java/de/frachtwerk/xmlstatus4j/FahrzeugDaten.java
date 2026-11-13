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

public class FahrzeugDaten {
  @JacksonXmlProperty(localName = "FAZ_NR01")
  private String fahrzeug1Nummer;

  @JacksonXmlProperty(localName = "FAZ_KZ01")
  private String fahrzeug1Kennzeichen;

  @JacksonXmlProperty(localName = "FAZ_PL01")
  private String fahrzeug1Plombe;

  @JacksonXmlProperty(localName = "FAZ_NR02")
  private String fahrzeug2Nummer;

  @JacksonXmlProperty(localName = "FAZ_KZ02")
  private String fahrzeug2Kennzeichen;

  @JacksonXmlProperty(localName = "FAZ_PL02")
  private String fahrzeug2Plombe;

  public String getFahrzeug1Nummer() {
    return fahrzeug1Nummer;
  }

  public void setFahrzeug1Nummer(String fahrzeug1Nummer) {
    this.fahrzeug1Nummer = fahrzeug1Nummer;
  }

  public String getFahrzeug1Kennzeichen() {
    return fahrzeug1Kennzeichen;
  }

  public void setFahrzeug1Kennzeichen(String fahrzeug1Kennzeichen) {
    this.fahrzeug1Kennzeichen = fahrzeug1Kennzeichen;
  }

  public String getFahrzeug1Plombe() {
    return fahrzeug1Plombe;
  }

  public void setFahrzeug1Plombe(String fahrzeug1Plombe) {
    this.fahrzeug1Plombe = fahrzeug1Plombe;
  }

  public String getFahrzeug2Nummer() {
    return fahrzeug2Nummer;
  }

  public void setFahrzeug2Nummer(String fahrzeug2Nummer) {
    this.fahrzeug2Nummer = fahrzeug2Nummer;
  }

  public String getFahrzeug2Kennzeichen() {
    return fahrzeug2Kennzeichen;
  }

  public void setFahrzeug2Kennzeichen(String fahrzeug2Kennzeichen) {
    this.fahrzeug2Kennzeichen = fahrzeug2Kennzeichen;
  }

  public String getFahrzeug2Plombe() {
    return fahrzeug2Plombe;
  }

  public void setFahrzeug2Plombe(String fahrzeug2Plombe) {
    this.fahrzeug2Plombe = fahrzeug2Plombe;
  }
}

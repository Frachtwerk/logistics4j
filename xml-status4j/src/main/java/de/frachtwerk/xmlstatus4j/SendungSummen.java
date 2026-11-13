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

public class SendungSummen {
  @JacksonXmlProperty(localName = "SSU_ANZA")
  private String anzahl;

  @JacksonXmlProperty(localName = "SSU_GEWI")
  private String gewicht;

  public String getAnzahl() {
    return anzahl;
  }

  public void setAnzahl(String anzahl) {
    this.anzahl = anzahl;
  }

  public String getGewicht() {
    return gewicht;
  }

  public void setGewicht(String gewicht) {
    this.gewicht = gewicht;
  }
}

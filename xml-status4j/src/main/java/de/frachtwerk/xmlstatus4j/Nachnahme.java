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

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

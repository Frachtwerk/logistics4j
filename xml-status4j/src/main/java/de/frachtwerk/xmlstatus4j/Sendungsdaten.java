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

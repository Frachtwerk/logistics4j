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

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

public class PositionsDaten {
  @JacksonXmlProperty(localName = "POS_ANZA")
  private String anzahl;

  @JacksonXmlProperty(localName = "POS_ART")
  private String art;

  @JacksonXmlProperty(localName = "POS_ZENR")
  private String zeichen;

  @JacksonXmlProperty(localName = "POS_BEZE")
  private String bezeichnung;

  @JacksonXmlProperty(localName = "POS_GEWI")
  private String gewicht;

  @JacksonXmlProperty(localName = "POS_GGVS")
  private String gefahrgutKennzeichen;

  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "BCD")
  private List<Barcode> barcodes;

  public String getAnzahl() {
    return anzahl;
  }

  public void setAnzahl(String anzahl) {
    this.anzahl = anzahl;
  }

  public String getArt() {
    return art;
  }

  public void setArt(String art) {
    this.art = art;
  }

  public String getZeichen() {
    return zeichen;
  }

  public void setZeichen(String zeichen) {
    this.zeichen = zeichen;
  }

  public String getBezeichnung() {
    return bezeichnung;
  }

  public void setBezeichnung(String bezeichnung) {
    this.bezeichnung = bezeichnung;
  }

  public String getGewicht() {
    return gewicht;
  }

  public void setGewicht(String gewicht) {
    this.gewicht = gewicht;
  }

  public String getGefahrgutKennzeichen() {
    return gefahrgutKennzeichen;
  }

  public void setGefahrgutKennzeichen(String gefahrgutKennzeichen) {
    this.gefahrgutKennzeichen = gefahrgutKennzeichen;
  }

  public List<Barcode> getBarcodes() {
    return barcodes;
  }

  public void setBarcodes(List<Barcode> barcodes) {
    this.barcodes = barcodes;
  }
}

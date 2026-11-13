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

public class EmpfaengerDaten {
  @JacksonXmlProperty(localName = "IDNR")
  private String adressnummer;

  @JacksonXmlProperty(localName = "NAM1")
  private String name1;

  @JacksonXmlProperty(localName = "NAM2")
  private String name2;

  @JacksonXmlProperty(localName = "STHS")
  private String strasseUndHausnummer;

  @JacksonXmlProperty(localName = "LAND")
  private String land;

  @JacksonXmlProperty(localName = "PLZ")
  private String postleitzahl;

  @JacksonXmlProperty(localName = "ORT")
  private String ort;

  @JacksonXmlProperty(localName = "TELE")
  private String telefonnummer;

  @JacksonXmlProperty(localName = "MAIL")
  private String email;

  @JacksonXmlProperty(localName = "ENLA")
  private String entladeAdresse;

  @JacksonXmlProperty(localName = "ANSP")
  private String ansprechpartner;

  public String getAdressnummer() {
    return adressnummer;
  }

  public void setAdressnummer(String adressnummer) {
    this.adressnummer = adressnummer;
  }

  public String getName1() {
    return name1;
  }

  public void setName1(String name1) {
    this.name1 = name1;
  }

  public String getName2() {
    return name2;
  }

  public void setName2(String name2) {
    this.name2 = name2;
  }

  public String getStrasseUndHausnummer() {
    return strasseUndHausnummer;
  }

  public void setStrasseUndHausnummer(String strasseUndHausnummer) {
    this.strasseUndHausnummer = strasseUndHausnummer;
  }

  public String getLand() {
    return land;
  }

  public void setLand(String land) {
    this.land = land;
  }

  public String getPostleitzahl() {
    return postleitzahl;
  }

  public void setPostleitzahl(String postleitzahl) {
    this.postleitzahl = postleitzahl;
  }

  public String getOrt() {
    return ort;
  }

  public void setOrt(String ort) {
    this.ort = ort;
  }

  public String getTelefonnummer() {
    return telefonnummer;
  }

  public void setTelefonnummer(String telefonnummer) {
    this.telefonnummer = telefonnummer;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getEntladeAdresse() {
    return entladeAdresse;
  }

  public void setEntladeAdresse(String entladeAdresse) {
    this.entladeAdresse = entladeAdresse;
  }

  public String getAnsprechpartner() {
    return ansprechpartner;
  }

  public void setAnsprechpartner(String ansprechpartner) {
    this.ansprechpartner = ansprechpartner;
  }
}

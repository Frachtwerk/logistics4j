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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Barcode {
  @JacksonXmlProperty(localName = "BCD_ART")
  private String barcodeArt;

  @JacksonXmlProperty(localName = "BCD_NUMM")
  private String barcode;

  @JacksonXmlProperty(localName = "BCD_STAT")
  private String zustellStatusPosition;

  @JacksonXmlProperty(localName = "BCD_ZUST")
  private String zustellStatus;

  @JacksonXmlProperty(localName = "BCD_TSST")
  private String timestampScannung;

  @JacksonXmlProperty(localName = "BCD_TEXT")
  private String zugehoerigerText;

  public String getBarcodeArt() {
    return barcodeArt;
  }

  public void setBarcodeArt(String barcodeArt) {
    this.barcodeArt = barcodeArt;
  }

  public String getBarcode() {
    return barcode;
  }

  public void setBarcode(String barcode) {
    this.barcode = barcode;
  }

  public String getZustellStatusPosition() {
    return zustellStatusPosition;
  }

  public void setZustellStatusPosition(String zustellStatusPosition) {
    this.zustellStatusPosition = zustellStatusPosition;
  }

  public String getZustellStatus() {
    return zustellStatus;
  }

  public void setZustellStatus(String zustellStatus) {
    this.zustellStatus = zustellStatus;
  }

  public String getTimestampScannung() {
    return timestampScannung;
  }

  public void setTimestampScannung(String timestampScannung) {
    this.timestampScannung = timestampScannung;
  }

  public String getZugehoerigerText() {
    return zugehoerigerText;
  }

  public void setZugehoerigerText(String zugehoerigerText) {
    this.zugehoerigerText = zugehoerigerText;
  }

  @JsonIgnore
  public LocalDateTime getTimestampScannungAsDateTime() {
    if (isBlank(timestampScannung)) {
      return null;
    }
    try {
      return LocalDateTime.parse(timestampScannung, FahrDaten.BASIC_TIMESTAMP_FORMATTER);
    } catch (DateTimeParseException e) {
      try {
        return LocalDateTime.parse(
            timestampScannung, FahrDaten.BASIC_DATE_TIME_FORMATTER_WITHOUT_SECONDS);
      } catch (DateTimeParseException ex) {
        return null;
      }
    }
  }

  public void setTimestampScannungFromDateTime(LocalDateTime timestampScannung) {
    this.timestampScannung = timestampScannung.format(FahrDaten.BASIC_TIMESTAMP_FORMATTER);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}

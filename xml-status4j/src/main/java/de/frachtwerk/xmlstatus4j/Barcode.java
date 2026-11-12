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

package de.frachtwerk.xmlstatus4j;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class SendungsStatus {
  @JacksonXmlProperty(localName = "STA_CODE")
  private String code;

  @JacksonXmlProperty(localName = "STA_TEXT")
  private String text;

  @JacksonXmlProperty(localName = "STA_QUIG")
  private String quittungsGeber;

  @JacksonXmlProperty(localName = "STA_QUID")
  private String quittungsDatum;

  @JacksonXmlProperty(localName = "STA_QUIZ")
  private String quittungsUhrzeit;

  @JacksonXmlProperty(localName = "STA_UNTE")
  private String quittungsUnterschrift;

  @JacksonXmlProperty(localName = "STA_UNTE_FORM")
  private String quittungsUnterschriftFormat;

  @JacksonXmlProperty(localName = "STA_GRUP")
  private String gruppierungsNummer;

  @JacksonXmlProperty(localName = "STA_TSSO")
  private String sollZustellungTimestamp;

  @JacksonXmlProperty(localName = "STA_TSBI")
  private String beginIstTimestamp;

  @JacksonXmlProperty(localName = "STA_TSEI")
  private String stopEndeTimestamp;

  @JacksonXmlProperty(localName = "STA_VCOD")
  private String verspaetungsCode;

  @JacksonXmlProperty(localName = "STA_VTEX")
  private String verspaetungsText;

  @JacksonXmlProperty(localName = "STA_WAZE")
  private String wartezeit;

  @JacksonXmlProperty(localName = "STA_LAT")
  private String breitenGrad;

  @JacksonXmlProperty(localName = "STA_LNG")
  private String leangenGrad;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getQuittungsGeber() {
    return quittungsGeber;
  }

  public void setQuittungsGeber(String quittungsGeber) {
    this.quittungsGeber = quittungsGeber;
  }

  public String getQuittungsDatum() {
    return quittungsDatum;
  }

  public void setQuittungsDatum(String quittungsDatum) {
    this.quittungsDatum = quittungsDatum;
  }

  public String getQuittungsUhrzeit() {
    return quittungsUhrzeit;
  }

  public void setQuittungsUhrzeit(String quittungsUhrzeit) {
    this.quittungsUhrzeit = quittungsUhrzeit;
  }

  public String getQuittungsUnterschrift() {
    return quittungsUnterschrift;
  }

  public void setQuittungsUnterschrift(String quittungsUnterschrift) {
    this.quittungsUnterschrift = quittungsUnterschrift;
  }

  public String getQuittungsUnterschriftFormat() {
    return quittungsUnterschriftFormat;
  }

  public void setQuittungsUnterschriftFormat(String quittungsUnterschriftFormat) {
    this.quittungsUnterschriftFormat = quittungsUnterschriftFormat;
  }

  public String getGruppierungsNummer() {
    return gruppierungsNummer;
  }

  public void setGruppierungsNummer(String gruppierungsNummer) {
    this.gruppierungsNummer = gruppierungsNummer;
  }

  public String getSollZustellungTimestamp() {
    return sollZustellungTimestamp;
  }

  public void setSollZustellungTimestamp(String sollZustellungTimestamp) {
    this.sollZustellungTimestamp = sollZustellungTimestamp;
  }

  public String getBeginIstTimestamp() {
    return beginIstTimestamp;
  }

  public void setBeginIstTimestamp(String beginIstTimestamp) {
    this.beginIstTimestamp = beginIstTimestamp;
  }

  public String getStopEndeTimestamp() {
    return stopEndeTimestamp;
  }

  public void setStopEndeTimestamp(String stopEndeTimestamp) {
    this.stopEndeTimestamp = stopEndeTimestamp;
  }

  public String getVerspaetungsCode() {
    return verspaetungsCode;
  }

  public void setVerspaetungsCode(String verspaetungsCode) {
    this.verspaetungsCode = verspaetungsCode;
  }

  public String getVerspaetungsText() {
    return verspaetungsText;
  }

  public void setVerspaetungsText(String verspaetungsText) {
    this.verspaetungsText = verspaetungsText;
  }

  public String getWartezeit() {
    return wartezeit;
  }

  public void setWartezeit(String wartezeit) {
    this.wartezeit = wartezeit;
  }

  public String getBreitenGrad() {
    return breitenGrad;
  }

  public void setBreitenGrad(String breitenGrad) {
    this.breitenGrad = breitenGrad;
  }

  public String getLeangenGrad() {
    return leangenGrad;
  }

  public void setLeangenGrad(String leangenGrad) {
    this.leangenGrad = leangenGrad;
  }

  @JsonIgnore
  public Optional<LocalDate> getQuittungsDatumAsDate() {
    if (isBlank(quittungsDatum)) {
      return Optional.empty();
    }
    return Optional.of(LocalDate.parse(quittungsDatum, FahrDaten.BASIC_DATE_FORMATTER));
  }

  public void setQuittungsDatumFromDate(LocalDate quittungsDatum) {
    this.quittungsDatum = quittungsDatum.format(FahrDaten.BASIC_DATE_FORMATTER);
  }

  @JsonIgnore
  public Optional<LocalTime> getQuittungsUhrzeitAsTime() {
    if (isBlank(quittungsUhrzeit)) {
      return Optional.empty();
    }
    return Optional.of(LocalTime.parse(quittungsUhrzeit, FahrDaten.BASIC_TIME_FORMATTER));
  }

  public void setQuittungsUhrzeitFromTime(LocalTime quittungsUhrzeit) {
    this.quittungsUhrzeit = quittungsUhrzeit.format(FahrDaten.BASIC_TIME_FORMATTER);
  }

  @JsonIgnore
  public Optional<LocalDateTime> getQuittungsTimeStamp() {
    Optional<LocalDate> date = getQuittungsDatumAsDate();
    Optional<LocalTime> time = getQuittungsUhrzeitAsTime();

    if (date.isPresent() && time.isPresent()) {
      return Optional.of(LocalDateTime.of(date.get(), time.get()));
    }

    return Optional.empty();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}

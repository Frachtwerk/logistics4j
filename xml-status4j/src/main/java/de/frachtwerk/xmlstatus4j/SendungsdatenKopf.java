package de.frachtwerk.xmlstatus4j;

import com.fasterxml.jackson.annotation.JsonIgnore;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class SendungsdatenKopf {

  @JacksonXmlProperty(localName = "SDG_LADZ")
  private String sendungsladungszuordnung;

  @JacksonXmlProperty(localName = "SDG_UEAR")
  private String uebertragungsart;

  @JacksonXmlProperty(localName = "SDG_LAUID")
  private String internesKennzeichenLAUntertyp;

  @JacksonXmlProperty(localName = "SDG_SDID")
  private String sendungsnummerLBaseDepot;

  @JacksonXmlProperty(localName = "SDG_IDABK")
  private String sendungsnummerVersandspediteurABKO;

  @JacksonXmlProperty(localName = "SDG_TYP")
  private String sendungstyp;

  @JacksonXmlProperty(localName = "SDG_IDGV")
  private String sdGID;

  @JacksonXmlProperty(localName = "SDG_ABKO")
  private String versandSpediteur;

  @JacksonXmlProperty(localName = "SDG_EMKO")
  private String empfangsSpediteur;

  @JacksonXmlProperty(localName = "SDG_LDAT")
  private String ladedatum;

  @JacksonXmlProperty(localName = "SDG_LZEI")
  private String ladezeit;

  @JacksonXmlProperty(localName = "SDG_LBEG")
  private String ladeBeginn;

  @JacksonXmlProperty(localName = "SDG_LEND")
  private String ladeende;

  @JacksonXmlProperty(localName = "SDG_ADAT")
  private String auftragsdatum;

  @JacksonXmlProperty(localName = "SDG_BEME")
  private String bemerkungen;

  @JacksonXmlProperty(localName = "SDG_KOOP")
  private String kooperationsKuerzel;

  @JacksonXmlProperty(localName = "SDG_STOP")
  private String stop;

  @JacksonXmlProperty(localName = "VER")
  private VersenderDaten versenderDaten;

  @JacksonXmlProperty(localName = "EMP")
  private EmpfaengerDaten empfaengerDaten;

  @JacksonXmlProperty(localName = "GGVS")
  private String kennzeichenGefahrgut;

  @JacksonXmlProperty(localName = "NAC")
  private Nachnahme nachnahme;

  @JacksonXmlProperty(localName = "SSU")
  private SendungSummen sendungSummen;

  @JacksonXmlProperty(localName = "STA")
  private SendungsStatus sendungsStatus;

  @JacksonXmlProperty(localName = "LDM")
  private Lademittel lademittel;

  public String getSendungsladungszuordnung() {
    return sendungsladungszuordnung;
  }

  public void setSendungsladungszuordnung(String sendungsladungszuordnung) {
    this.sendungsladungszuordnung = sendungsladungszuordnung;
  }

  public String getUebertragungsart() {
    return uebertragungsart;
  }

  public void setUebertragungsart(String uebertragungsart) {
    this.uebertragungsart = uebertragungsart;
  }

  public String getInternesKennzeichenLAUntertyp() {
    return internesKennzeichenLAUntertyp;
  }

  public void setInternesKennzeichenLAUntertyp(String internesKennzeichenLAUntertyp) {
    this.internesKennzeichenLAUntertyp = internesKennzeichenLAUntertyp;
  }

  public String getSendungsnummerLBaseDepot() {
    return sendungsnummerLBaseDepot;
  }

  public void setSendungsnummerLBaseDepot(String sendungsnummerLBaseDepot) {
    this.sendungsnummerLBaseDepot = sendungsnummerLBaseDepot;
  }

  public String getSendungsnummerVersandspediteurABKO() {
    return sendungsnummerVersandspediteurABKO;
  }

  public void setSendungsnummerVersandspediteurABKO(String sendungsnummerVersandspediteurABKO) {
    this.sendungsnummerVersandspediteurABKO = sendungsnummerVersandspediteurABKO;
  }

  public String getSendungstyp() {
    return sendungstyp;
  }

  public void setSendungstyp(String sendungstyp) {
    this.sendungstyp = sendungstyp;
  }

  public String getSdGID() {
    return sdGID;
  }

  public void setSdGID(String sdGID) {
    this.sdGID = sdGID;
  }

  public String getVersandSpediteur() {
    return versandSpediteur;
  }

  public void setVersandSpediteur(String versandSpediteur) {
    this.versandSpediteur = versandSpediteur;
  }

  public String getEmpfangsSpediteur() {
    return empfangsSpediteur;
  }

  public void setEmpfangsSpediteur(String empfangsSpediteur) {
    this.empfangsSpediteur = empfangsSpediteur;
  }

  public String getLadedatum() {
    return ladedatum;
  }

  public void setLadedatum(String ladedatum) {
    this.ladedatum = ladedatum;
  }

  public String getLadezeit() {
    return ladezeit;
  }

  public void setLadezeit(String ladezeit) {
    this.ladezeit = ladezeit;
  }

  public String getLadeBeginn() {
    return ladeBeginn;
  }

  public void setLadeBeginn(String ladeBeginn) {
    this.ladeBeginn = ladeBeginn;
  }

  public String getLadeende() {
    return ladeende;
  }

  public void setLadeende(String ladeende) {
    this.ladeende = ladeende;
  }

  public String getAuftragsdatum() {
    return auftragsdatum;
  }

  public void setAuftragsdatum(String auftragsdatum) {
    this.auftragsdatum = auftragsdatum;
  }

  public String getBemerkungen() {
    return bemerkungen;
  }

  public void setBemerkungen(String bemerkungen) {
    this.bemerkungen = bemerkungen;
  }

  public String getKooperationsKuerzel() {
    return kooperationsKuerzel;
  }

  public void setKooperationsKuerzel(String kooperationsKuerzel) {
    this.kooperationsKuerzel = kooperationsKuerzel;
  }

  public String getStop() {
    return stop;
  }

  public void setStop(String stop) {
    this.stop = stop;
  }

  public VersenderDaten getVersenderDaten() {
    return versenderDaten;
  }

  public void setVersenderDaten(VersenderDaten versenderDaten) {
    this.versenderDaten = versenderDaten;
  }

  public EmpfaengerDaten getEmpfaengerDaten() {
    return empfaengerDaten;
  }

  public void setEmpfaengerDaten(EmpfaengerDaten empfaengerDaten) {
    this.empfaengerDaten = empfaengerDaten;
  }

  public String getKennzeichenGefahrgut() {
    return kennzeichenGefahrgut;
  }

  public void setKennzeichenGefahrgut(String kennzeichenGefahrgut) {
    this.kennzeichenGefahrgut = kennzeichenGefahrgut;
  }

  public Nachnahme getNachnahme() {
    return nachnahme;
  }

  public void setNachnahme(Nachnahme nachnahme) {
    this.nachnahme = nachnahme;
  }

  public SendungSummen getSendungSummen() {
    return sendungSummen;
  }

  public void setSendungSummen(SendungSummen sendungSummen) {
    this.sendungSummen = sendungSummen;
  }

  public SendungsStatus getSendungsStatus() {
    return sendungsStatus;
  }

  public void setSendungsStatus(SendungsStatus sendungsStatus) {
    this.sendungsStatus = sendungsStatus;
  }

  public Lademittel getLademittel() {
    return lademittel;
  }

  public void setLademittel(Lademittel lademittel) {
    this.lademittel = lademittel;
  }

  @JsonIgnore
  public String getFirstNonBlankShipmentNumber() {
    if (!isBlank(this.sendungsnummerVersandspediteurABKO)) {
      return this.sendungsnummerVersandspediteurABKO;
    }

    return this.sendungsnummerLBaseDepot;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}

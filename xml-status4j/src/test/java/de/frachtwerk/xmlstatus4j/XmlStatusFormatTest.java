package de.frachtwerk.xmlstatus4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class XmlStatusFormatTest {

  @Test
  void readsShipmentStatusAndBarcodes() throws Exception {
    FahrDaten fahrDaten = XmlStatusFormat.read(lines("status/status.valid.xml"));

    Sendungsdaten sendung = fahrDaten.getFahrten().getFirst().getSendungsdaten().getFirst();
    SendungsdatenKopf kopf = sendung.getSendungsdatenKopf();
    assertThat(kopf.getSendungsnummerVersandspediteurABKO()).isEqualTo("12345678901234567890");
    assertThat(kopf.getSendungsnummerLBaseDepot()).isEqualTo("GEX111222333");
    assertThat(kopf.getVersenderDaten().getName1()).isEqualTo("Versender GmbH");
    assertThat(kopf.getSendungsStatus().getCode()).isEqualTo("300");
    assertThat(kopf.getSendungsStatus().getQuittungsTimeStamp())
        .contains(LocalDateTime.of(2024, 10, 14, 13, 11, 33));

    Barcode barcode = sendung.getPositionsDaten().getFirst().getBarcodes().getFirst();
    assertThat(barcode.getBarcode()).isEqualTo("1234568901234567890");
    assertThat(barcode.getZustellStatus()).isEqualTo("001");
    // timestamps without seconds are accepted as well
    assertThat(barcode.getTimestampScannungAsDateTime())
        .isEqualTo(LocalDateTime.of(2024, 9, 16, 13, 12));
  }

  @Test
  void refusesMalformedDocuments() throws Exception {
    assertThatThrownBy(() -> XmlStatusFormat.read(lines("status/status.invalid.xml")))
        .isInstanceOf(XmlStatusException.class);
  }

  @Test
  void ignoresDoctypeAndLineBreaksInValues() {
    FahrDaten fahrDaten =
        XmlStatusFormat.read(
            List.of(
                "<?xml version=\"1.0\"?>",
                "<!DOCTYPE FAR_DATA SYSTEM \"far.dtd\">",
                "<FAR_DATA><FAHRT><SDG><SDG_HEAD><STA><STA_TEXT>zwei",
                "Zeilen </STA_TEXT></STA></SDG_HEAD></SDG></FAHRT></FAR_DATA>"));

    SendungsdatenKopf kopf =
        fahrDaten.getFahrten().getFirst().getSendungsdaten().getFirst().getSendungsdatenKopf();
    assertThat(kopf.getSendungsStatus().getText()).isEqualTo("zweiZeilen");
  }

  @Test
  void writesWhatItReads() throws Exception {
    List<String> written =
        XmlStatusFormat.write(XmlStatusFormat.read(lines("status/status.valid.xml")));

    assertThat(written.getFirst()).isEqualTo(XmlStatusFormat.DECLARATION);
    assertThat(written)
        .contains("  <FAHRT>", "        <SDG_IDABK>12345678901234567890</SDG_IDABK>");
    assertThat(XmlStatusFormat.write(XmlStatusFormat.read(written))).isEqualTo(written);
  }

  private static List<String> lines(String resource) throws IOException, URISyntaxException {
    Path path = Path.of(XmlStatusFormatTest.class.getClassLoader().getResource(resource).toURI());
    return Files.readAllLines(path, StandardCharsets.ISO_8859_1);
  }
}

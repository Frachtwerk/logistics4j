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
    List<String> lines = lines("status/status.invalid.xml");

    assertThatThrownBy(() -> XmlStatusFormat.read(lines)).isInstanceOf(XmlStatusException.class);
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

  @Test
  void writesTheSameAsTheFirstRelease() throws Exception {
    // status.written.xml is the output of 0.1.0 for status.valid.xml
    List<String> written =
        XmlStatusFormat.write(XmlStatusFormat.read(lines("status/status.valid.xml")));

    assertThat(written).isEqualTo(lines("status/status.written.xml"));
  }

  private static List<String> lines(String resource) throws IOException, URISyntaxException {
    Path path = Path.of(XmlStatusFormatTest.class.getClassLoader().getResource(resource).toURI());
    return Files.readAllLines(path, StandardCharsets.ISO_8859_1);
  }
}

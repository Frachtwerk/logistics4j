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

import de.frachtwerk.xmlstatus4j.XmlStatusConfig.NullObjects;
import de.frachtwerk.xmlstatus4j.XmlStatusConfig.Nulls;
import de.frachtwerk.xmlstatus4j.XmlStatusConfig.Order;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

class XmlStatusGroupsTest {

  // FAHRT (FAR?, SDG+)+, POSI (POS_ANZA?, ..., POS_BEZE, ..., BCD*)+ and BCD (BCD_ART, ...)*
  private static final String REPEATED =
      "<FAR_DATA><FAHRT>"
          + "<FAR><FAR_FAHR>T1</FAR_FAHR></FAR>"
          + sdg("S1")
          + sdg("S2")
          + "<FAR><FAR_FAHR>T2</FAR_FAHR></FAR>"
          + sdg("S3")
          + "</FAHRT></FAR_DATA>";

  private static String sdg(String id) {
    return "<SDG><SDG_HEAD><SDG_SDID>"
        + id
        + "</SDG_SDID><STA><STA_CODE>1</STA_CODE></STA></SDG_HEAD>"
        + "<POSI>"
        + "<POS_ANZA>1</POS_ANZA><POS_BEZE>p1</POS_BEZE>"
        + "<BCD><BCD_ART>NVE</BCD_ART><BCD_NUMM>b1</BCD_NUMM>"
        + "<BCD_ART>NVE</BCD_ART><BCD_NUMM>b2</BCD_NUMM></BCD>"
        + "<BCD><BCD_ART>NVE</BCD_ART><BCD_NUMM>b3</BCD_NUMM></BCD>"
        + "<POS_BEZE>p2</POS_BEZE>"
        + "</POSI></SDG>";
  }

  @Test
  void readsTheGroupsOfTheDtd() {
    Fahrt fahrt = XmlStatusFormat.read(List.of(REPEATED)).getFahrten().getFirst();

    assertThat(fahrt.getAbschnitte())
        .extracting(a -> a.getFahrtHeadDaten().getRollkartenNr())
        .containsExactly("T1", "T2");
    assertThat(fahrt.getAbschnitte())
        .extracting(a -> a.getSendungsdaten().size())
        .containsExactly(2, 1);
    List<Position> positionen =
        fahrt
            .getAbschnitte()
            .getLast()
            .getSendungsdaten()
            .getFirst()
            .getPositionsDaten()
            .getFirst()
            .getPositionen();
    assertThat(positionen).extracting(Position::getBezeichnung).containsExactly("p1", "p2");
    assertThat(positionen.getFirst().getAnzahl()).isEqualTo("1");
    assertThat(positionen.getLast().getBarcodeDaten()).isNull();
    assertThat(positionen.getFirst().getBarcodeDaten())
        .extracting(bcd -> bcd.getBarcodes().stream().map(Barcode::getBarcode).toList())
        .containsExactly(List.of("b1", "b2"), List.of("b3"));
  }

  @Test
  void writesTheGroupsBackInTheirOrder() throws Exception {
    XmlStatusCodec codec = XmlStatusCodec.of(XmlStatusConfig.builder().order(Order.DTD).build());

    List<String> written = codec.write(XmlStatusFormat.read(List.of(REPEATED)));

    assertThat(children(written, "FAHRT")).containsExactly("FAR", "SDG", "SDG", "FAR", "SDG");
    assertThat(children(written, "POSI"))
        .containsExactly(
            "POS_ANZA",
            "POS_ART",
            "POS_ZENR",
            "POS_BEZE",
            "POS_GEWI",
            "POS_GGVS",
            "BCD",
            "BCD",
            "POS_ANZA",
            "POS_ART",
            "POS_ZENR",
            "POS_BEZE",
            "POS_GEWI",
            "POS_GGVS");
    assertThat(children(written, "BCD"))
        .containsExactly(
            "BCD_ART",
            "BCD_NUMM",
            "BCD_STAT",
            "BCD_ZUST",
            "BCD_TSST",
            "BCD_TEXT",
            "BCD_ART",
            "BCD_NUMM",
            "BCD_STAT",
            "BCD_ZUST",
            "BCD_TSST",
            "BCD_TEXT");
    // without null values as empty elements, reading gives back the same model
    XmlStatusCodec omitting =
        XmlStatusCodec.of(XmlStatusConfig.builder().order(Order.DTD).nulls(Nulls.OMIT).build());
    FahrDaten read = XmlStatusFormat.read(List.of(REPEATED));
    assertThat(omitting.read(omitting.write(read))).usingRecursiveComparison().isEqualTo(read);
  }

  @Test
  void writesTheOrderOfTheDtdIfConfigured() throws Exception {
    FahrDaten fahrDaten = XmlStatusFormat.read(lines("status/status.valid.xml"));
    XmlStatusCodec codec = XmlStatusCodec.of(XmlStatusConfig.builder().order(Order.DTD).build());

    List<String> written = codec.write(fahrDaten);

    assertThat(children(written, "SDG")).containsExactly("SDG_HEAD", "POSI", "POSI");
    assertThat(children(written, "SDG_HEAD"))
        .startsWith("SDG_LADZ", "SDG_UEAR", "SDG_LAUID", "SDG_SDID", "SDG_IDABK")
        .endsWith("SDG_KOOP", "SDG_STOP", "VER", "EMP", "GGVS", "NAC", "SSU", "STA", "LDM");
    assertThat(children(written, "STA"))
        .containsSubsequence("STA_UNTE_FORM", "STA_LAT", "STA_LNG", "STA_GRUP")
        .endsWith("STA_WAZE");
    assertThat(children(written, "VER")).startsWith("IDNR", "NAM1", "NAM2", "STHS", "LAND");
    // the default keeps the alphabetical order of 0.1.0
    assertThat(children(XmlStatusFormat.write(fahrDaten), "SDG")).startsWith("POSI");
  }

  @Test
  void omitsNullContainersIfConfigured() throws Exception {
    Fahrt fahrt = XmlStatusFormat.read(List.of(REPEATED)).getFahrten().getFirst();
    FahrDaten fahrDaten = new FahrDaten();
    fahrDaten.setFahrten(List.of(fahrt));
    XmlStatusCodec codec =
        XmlStatusCodec.of(
            XmlStatusConfig.builder().order(Order.DTD).nullObjects(NullObjects.OMIT).build());

    List<String> written = codec.write(fahrDaten);

    // VER, EMP, SSU ... are null and left out, null values still become empty elements
    assertThat(children(written, "SDG_HEAD"))
        .containsExactly(
            "SDG_LADZ",
            "SDG_UEAR",
            "SDG_LAUID",
            "SDG_SDID",
            "SDG_IDABK",
            "SDG_TYP",
            "SDG_IDGV",
            "SDG_ABKO",
            "SDG_EMKO",
            "SDG_LDAT",
            "SDG_LZEI",
            "SDG_LBEG",
            "SDG_LEND",
            "SDG_ADAT",
            "SDG_BEME",
            "SDG_KOOP",
            "SDG_STOP",
            "GGVS",
            "STA");
    assertThat(children(XmlStatusFormat.write(fahrDaten), "SDG_HEAD")).contains("VER", "EMP");
  }

  @Test
  void readsAnEmptyElementAsNoGroups() {
    FahrDaten fahrDaten =
        XmlStatusFormat.read(
            List.of(
                "<FAR_DATA><FAHRT><SDG><SDG_HEAD/><POSI><BCD/></POSI></SDG></FAHRT></FAR_DATA>"));

    Position position =
        fahrDaten
            .getFahrten()
            .getFirst()
            .getAbschnitte()
            .getFirst()
            .getSendungsdaten()
            .getFirst()
            .getPositionsDaten()
            .getFirst()
            .getPositionen()
            .getFirst();
    assertThat(fahrDaten.getFahrten().getFirst().getAbschnitte().getFirst().getFahrtHeadDaten())
        .isNull();
    assertThat(position.getBarcodeDaten().getFirst().getBarcodes()).isEmpty();
  }

  /** Names of the child elements of the first element with the given name. */
  private static List<String> children(List<String> lines, String name) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    Element element =
        (Element)
            factory
                .newDocumentBuilder()
                .parse(
                    new ByteArrayInputStream(
                        String.join("\n", lines).getBytes(StandardCharsets.ISO_8859_1)))
                .getElementsByTagName(name)
                .item(0);
    List<String> names = new ArrayList<>();
    for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element e) {
        names.add(e.getTagName());
      }
    }
    return names;
  }

  private static List<String> lines(String resource) throws IOException, URISyntaxException {
    Path path = Path.of(XmlStatusGroupsTest.class.getClassLoader().getResource(resource).toURI());
    return Files.readAllLines(path, StandardCharsets.ISO_8859_1);
  }
}

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

import de.frachtwerk.xmlstatus4j.XmlStatusConfig.Doctype;
import de.frachtwerk.xmlstatus4j.XmlStatusConfig.LineBreaks;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class XmlStatusCodecTest {

  private static final List<String> WRAPPED_TEXT =
      List.of(
          "<?xml version=\"1.0\"?>",
          "<!DOCTYPE FAR_DATA SYSTEM \"far.dtd\">",
          "<FAR_DATA><FAHRT><SDG><SDG_HEAD><STA><STA_TEXT>zwei",
          "  Zeilen </STA_TEXT></STA></SDG_HEAD></SDG></FAHRT></FAR_DATA>");

  @Test
  void writesByteForByteWhatTheFirstReleaseWrote() throws Exception {
    // status.written.xml is the output of 0.1.0 for status.valid.xml
    FahrDaten fahrDaten = XmlStatusFormat.read(lines("status/status.valid.xml"));
    List<String> expected = lines("status/status.written.xml");

    assertThat(XmlStatusFormat.write(fahrDaten)).isEqualTo(expected);
    assertThat(XmlStatusCodec.create().write(fahrDaten)).isEqualTo(expected);
    assertThat(XmlStatusCodec.of(XmlStatusConfig.builder().build()))
        .isSameAs(XmlStatusCodec.create());
  }

  @Test
  void writesTheDeclarationOfTheEncoding() throws Exception {
    FahrDaten fahrDaten = XmlStatusFormat.read(lines("status/status.valid.xml"));

    assertThat(codec(StandardCharsets.UTF_8).write(fahrDaten).getFirst())
        .isEqualTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    assertThat(codec(Charset.forName("IBM850")).write(fahrDaten).getFirst())
        .isEqualTo("<?xml version=\"1.0\" encoding=\"IBM850\"?>");
    assertThat(codec(Charset.forName("windows-1252")).write(fahrDaten).getFirst())
        .isEqualTo("<?xml version=\"1.0\" encoding=\"windows-1252\"?>");
    // apart from the declaration the document is the same
    assertThat(codec(StandardCharsets.UTF_8).write(fahrDaten).subList(1, 110))
        .isEqualTo(XmlStatusFormat.write(fahrDaten).subList(1, 110));
  }

  @Test
  void writesWithoutDeclarationOrCompact() throws Exception {
    FahrDaten fahrDaten = XmlStatusFormat.read(lines("status/status.valid.xml"));

    List<String> withoutDeclaration =
        XmlStatusCodec.of(XmlStatusConfig.builder().noDeclaration().build()).write(fahrDaten);
    assertThat(withoutDeclaration.getFirst()).isEqualTo("<FAR_DATA>");

    List<String> compact =
        XmlStatusCodec.of(XmlStatusConfig.builder().indent(false).build()).write(fahrDaten);
    assertThat(compact).hasSize(2);
    assertThat(compact.get(0)).isEqualTo(XmlStatusFormat.DECLARATION);
    assertThat(compact.get(1)).startsWith("<FAR_DATA><FAHRT><").endsWith("</FAHRT></FAR_DATA>");
  }

  @Test
  void handlesLineBreaksInValuesAsConfigured() {
    assertThat(statusText(LineBreaks.STRIP)).isEqualTo("zwei  Zeilen");
    assertThat(statusText(LineBreaks.SPACE)).isEqualTo("zwei Zeilen");
    assertThat(statusText(LineBreaks.KEEP)).isEqualTo("zwei\n  Zeilen ");
  }

  @Test
  void rejectsADoctypeIfConfigured() {
    XmlStatusCodec codec =
        XmlStatusCodec.of(XmlStatusConfig.builder().doctype(Doctype.REJECT).build());

    assertThatThrownBy(() -> codec.read(WRAPPED_TEXT))
        .isInstanceOf(XmlStatusException.class)
        .hasMessage("DOCTYPE declarations are not allowed");
    assertThat(codec.read(WRAPPED_TEXT.subList(2, 4)).getFahrten()).hasSize(1);
  }

  @Test
  void renamesElementsAndTheRoot() throws Exception {
    XmlStatusCodec codec =
        XmlStatusCodec.of(
            XmlStatusConfig.builder()
                .rootElement("STATUS")
                .element(Fahrt.class, "sendungsdaten", "SHIPMENT")
                .element(SendungsStatus.class, "code", "STATUS_CODE")
                .build());
    FahrDaten fahrDaten = XmlStatusFormat.read(lines("status/status.valid.xml"));

    List<String> written = codec.write(fahrDaten);

    assertThat(written)
        .contains("<STATUS>", "    <SHIPMENT>", "          <STATUS_CODE>300</STATUS_CODE>")
        .doesNotContain("<FAR_DATA>", "    <SDG>", "          <STA_CODE>300</STA_CODE>");
    FahrDaten read = codec.read(written);
    assertThat(
            read.getFahrten()
                .getFirst()
                .getSendungsdaten()
                .getFirst()
                .getSendungsdatenKopf()
                .getSendungsStatus()
                .getCode())
        .isEqualTo("300");
    // the default names are unaffected
    assertThat(XmlStatusFormat.write(fahrDaten)).contains("<FAR_DATA>", "    <SDG>");
  }

  @Test
  void readsAndWritesStreamsInTheConfiguredEncoding() throws Exception {
    FahrDaten fahrDaten =
        XmlStatusFormat.read(lines("status/status.valid.xml", StandardCharsets.UTF_8));

    for (Charset charset :
        List.of(
            StandardCharsets.UTF_8,
            Charset.forName("IBM850"),
            Charset.forName("windows-1252"),
            StandardCharsets.ISO_8859_1)) {
      XmlStatusCodec codec = codec(charset);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      codec.write(fahrDaten, out);

      String text = out.toString(charset);
      assertThat(text).startsWith(codec.config().declaration().orElseThrow() + "\r\n");
      assertThat(text).contains("Empfänger GbR").endsWith("</FAR_DATA>\r\n");
      FahrDaten read = codec.read(new ByteArrayInputStream(out.toByteArray()));
      assertThat(codec.write(read)).isEqualTo(codec.write(fahrDaten));
    }
  }

  @Test
  void refusesCharactersTheEncodingCannotHold() {
    FahrDaten fahrDaten = XmlStatusFormat.read(List.of("<FAR_DATA><FAHRT/></FAR_DATA>"));
    fahrDaten.getFahrten().getFirst().setSendungsdaten(List.of(sendungWithStatusText("1 €")));
    XmlStatusCodec ibm850 = codec(Charset.forName("IBM850"));

    assertThatThrownBy(() -> ibm850.write(fahrDaten, new ByteArrayOutputStream()))
        .isInstanceOf(XmlStatusException.class)
        .hasMessage("cannot encode in IBM850");
    byte[] invalidUtf8 = {'<', 'F', (byte) 0xC3, '>'};
    assertThatThrownBy(
            () -> codec(StandardCharsets.UTF_8).read(new ByteArrayInputStream(invalidUtf8)))
        .isInstanceOf(XmlStatusException.class)
        .hasMessage("not valid UTF-8");
  }

  @Test
  void canBeSharedBetweenThreads() throws Exception {
    XmlStatusCodec codec = codec(StandardCharsets.UTF_8);
    FahrDaten fahrDaten = XmlStatusFormat.read(lines("status/status.valid.xml"));
    List<String> expected = codec.write(fahrDaten);

    try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
      List<Future<List<String>>> results =
          executor.invokeAll(
              Collections.nCopies(32, () -> codec.write(codec.read(codec.write(fahrDaten)))));
      for (Future<List<String>> result : results) {
        assertThat(result.get()).isEqualTo(expected);
      }
    }
  }

  private static String statusText(LineBreaks lineBreaks) {
    XmlStatusCodec codec =
        XmlStatusCodec.of(XmlStatusConfig.builder().lineBreaks(lineBreaks).build());
    return codec
        .read(WRAPPED_TEXT)
        .getFahrten()
        .getFirst()
        .getSendungsdaten()
        .getFirst()
        .getSendungsdatenKopf()
        .getSendungsStatus()
        .getText();
  }

  private static Sendungsdaten sendungWithStatusText(String text) {
    SendungsStatus status = new SendungsStatus();
    status.setText(text);
    SendungsdatenKopf kopf = new SendungsdatenKopf();
    kopf.setSendungsStatus(status);
    Sendungsdaten sendung = new Sendungsdaten();
    sendung.setSendungsdatenKopf(kopf);
    return sendung;
  }

  private static XmlStatusCodec codec(Charset charset) {
    return XmlStatusCodec.of(XmlStatusConfig.builder().encoding(charset).build());
  }

  private static List<String> lines(String resource) throws IOException, URISyntaxException {
    return lines(resource, StandardCharsets.ISO_8859_1);
  }

  private static List<String> lines(String resource, Charset charset)
      throws IOException, URISyntaxException {
    Path path = Path.of(XmlStatusCodecTest.class.getClassLoader().getResource(resource).toURI());
    return Files.readAllLines(path, charset);
  }
}

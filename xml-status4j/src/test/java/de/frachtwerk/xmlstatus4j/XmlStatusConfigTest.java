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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XmlStatusConfigTest {

  @Test
  void defaultsAreTheBehaviourOfTheFirstRelease() {
    XmlStatusConfig config = XmlStatusConfig.defaults();

    assertThat(config.encoding()).isEqualTo(StandardCharsets.ISO_8859_1);
    assertThat(config.declaration()).contains(XmlStatusFormat.DECLARATION);
    assertThat(config.indent()).isTrue();
    assertThat(config.lineBreaks()).isEqualTo(LineBreaks.STRIP);
    assertThat(config.doctype()).isEqualTo(Doctype.IGNORE);
    assertThat(config.rootElement()).isEqualTo("FAR_DATA");
    assertThat(config.elements()).isEmpty();
    assertThat(XmlStatusConfig.builder().build()).isEqualTo(config);
  }

  @Test
  void derivesTheDeclarationFromTheEncoding() {
    assertThat(declaration(StandardCharsets.UTF_8))
        .isEqualTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    assertThat(declaration(Charset.forName("IBM850")))
        .isEqualTo("<?xml version=\"1.0\" encoding=\"IBM850\"?>");
    // aliases resolve to the canonical name
    assertThat(declaration(Charset.forName("Cp1252")))
        .isEqualTo("<?xml version=\"1.0\" encoding=\"windows-1252\"?>");
    assertThat(declaration(StandardCharsets.ISO_8859_1)).isEqualTo(XmlStatusFormat.DECLARATION);
  }

  @Test
  void keepsAnExplicitDeclarationWhateverTheEncoding() {
    String declaration = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";

    XmlStatusConfig before =
        XmlStatusConfig.builder().declaration(declaration).encoding(StandardCharsets.UTF_8).build();
    XmlStatusConfig after =
        XmlStatusConfig.builder().encoding(StandardCharsets.UTF_8).declaration(declaration).build();

    assertThat(before.declaration()).contains(declaration);
    assertThat(after.declaration()).contains(declaration);
    assertThat(XmlStatusConfig.builder().noDeclaration().build().declaration()).isEmpty();
  }

  @Test
  void toBuilderKeepsEverythingAndFollowsANewEncoding() {
    XmlStatusConfig config =
        XmlStatusConfig.builder()
            .indent(false)
            .lineBreaks(LineBreaks.KEEP)
            .element(Fahrt.class, "sendungsdaten", "SHIPMENT")
            .build();

    assertThat(config.toBuilder().build()).isEqualTo(config);
    assertThat(config.toBuilder().encoding(StandardCharsets.UTF_8).build().declaration())
        .contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    XmlStatusConfig explicit = config.toBuilder().declaration("<?xml version=\"1.0\"?>").build();
    assertThat(explicit.toBuilder().encoding(StandardCharsets.UTF_8).build().declaration())
        .contains("<?xml version=\"1.0\"?>");
  }

  @Test
  void loadsEveryOptionFromYaml() {
    XmlStatusConfig config =
        XmlStatusConfig.loadClasspath(
            "config/all-options.yaml", XmlStatusConfigTest.class.getClassLoader());

    assertThat(config)
        .isEqualTo(
            XmlStatusConfig.builder()
                .encoding(Charset.forName("windows-1252"))
                .indent(false)
                .lineBreaks(LineBreaks.SPACE)
                .doctype(Doctype.REJECT)
                .rootElement("STATUS")
                .element("Fahrt.sendungsdaten", "SHIPMENT")
                .element(SendungsStatus.class, "code", "STATUS_CODE")
                .build());
    assertThat(config.declaration()).contains("<?xml version=\"1.0\" encoding=\"windows-1252\"?>");
  }

  @Test
  void loadsAFileAndTheDeclarationKeywords(@TempDir Path dir) throws Exception {
    Path file = dir.resolve("status.yaml");
    Files.writeString(file, "encoding: UTF-8\ndeclaration: none\n");
    assertThat(XmlStatusConfig.load(file).declaration()).isEmpty();

    Files.writeString(file, "declaration: \"<?xml version='1.0'?>\"\n");
    assertThat(XmlStatusConfig.load(file).declaration()).contains("<?xml version='1.0'?>");

    Files.writeString(file, "");
    assertThat(XmlStatusConfig.load(file)).isEqualTo(XmlStatusConfig.defaults());
  }

  @Test
  void reportsEveryErrorOfAConfiguration() {
    String yaml =
        """
        encoding: no-such-charset
        indent: maybe
        lineBreaks: fold
        doctype: allow
        rootElement: "1st"
        declaration: "encoding=utf-8"
        colour: blue
        elements:
          Fahrt.sendungsdaten: "two words"
          Fahrt.nothing: X
          Unknown.code: X
          nodot: X
        """;

    assertThatThrownBy(() -> load(yaml))
        .isInstanceOfSatisfying(
            XmlStatusConfigException.class,
            e ->
                assertThat(e.errors())
                    .hasSize(11)
                    .anySatisfy(m -> assertThat(m).contains("colour: unknown key"))
                    .anySatisfy(
                        m -> assertThat(m).contains("encoding: unsupported charset 'no-such"))
                    .anySatisfy(m -> assertThat(m).contains("indent: must be true or false"))
                    .anySatisfy(m -> assertThat(m).contains("lineBreaks: unknown value 'fold'"))
                    .anySatisfy(m -> assertThat(m).contains("doctype: unknown value 'allow'"))
                    .anySatisfy(m -> assertThat(m).contains("rootElement: not an XML name"))
                    .anySatisfy(m -> assertThat(m).contains("declaration: must start with"))
                    .anySatisfy(
                        m -> assertThat(m).contains("elements.Fahrt.sendungsdaten: not an XML"))
                    .anySatisfy(m ->
                        assertThat(m)
                            .contains("elements.Fahrt.nothing: unknown property; known: [")
                            .contains("sendungsdaten"))
                    .anySatisfy(m -> assertThat(m).contains("elements.Unknown.code: unknown class"))
                    .anySatisfy(m -> assertThat(m).contains("elements.nodot: key must be")));
  }

  @Test
  void refusesYamlThatIsNoMapping() {
    assertThatThrownBy(() -> load("- a\n- b\n"))
        .isInstanceOf(XmlStatusConfigException.class)
        .hasMessageContaining("test.yaml: a configuration must be a YAML mapping");
    assertThatThrownBy(() -> load("a: [\n"))
        .isInstanceOf(XmlStatusConfigException.class)
        .hasMessageContaining("test.yaml: not valid YAML");
    assertThatThrownBy(() -> load("elements: [a]\n"))
        .isInstanceOf(XmlStatusConfigException.class)
        .hasMessageContaining("elements: must be a mapping");
    assertThatThrownBy(
            () -> XmlStatusConfig.loadClasspath("missing.yaml", getClass().getClassLoader()))
        .isInstanceOf(XmlStatusConfigException.class)
        .hasMessageContaining("missing.yaml: not found");
  }

  @Test
  void validatesTheBuilderLikeTheYaml() {
    XmlStatusConfig.Builder builder =
        XmlStatusConfig.builder().element(Barcode.class, "nummer", "NR");

    assertThatThrownBy(builder::build)
        .isInstanceOf(XmlStatusConfigException.class)
        .hasMessageContaining("elements.Barcode.nummer: unknown property");
  }

  @Test
  void elementsAreImmutable() {
    Map<String, String> elements =
        XmlStatusConfig.builder().element(Fahrt.class, "sendungsdaten", "S").build().elements();

    assertThatThrownBy(() -> elements.put("Fahrt.fahrtHeadDaten", "H"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private static String declaration(Charset encoding) {
    return XmlStatusConfig.builder().encoding(encoding).build().declaration().orElseThrow();
  }

  private static XmlStatusConfig load(String yaml) {
    return XmlStatusConfig.load(
        "test.yaml", new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
  }
}

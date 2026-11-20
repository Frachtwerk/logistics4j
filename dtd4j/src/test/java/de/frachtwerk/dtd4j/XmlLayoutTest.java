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

package de.frachtwerk.dtd4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XmlLayoutTest {

  private static final Dtd DTD =
      Dtd.parse(
          "test.dtd",
          "<!ELEMENT R (G)> <!ELEMENT G (A?, B+)+> <!ELEMENT A (#PCDATA)> <!ELEMENT B (#PCDATA)>");

  @Test
  void loadsEveryOption() {
    XmlLayout layout = Tours.layout();

    assertThat(layout.root()).isEqualTo("TOURS");
    assertThat(layout.encoding()).isEqualTo(StandardCharsets.ISO_8859_1);
    assertThat(layout.declaration()).contains("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>");
    assertThat(layout.nullObjects()).isEqualTo(XmlLayout.NullObjects.OMIT);
    assertThat(layout.order()).isEqualTo(XmlLayout.Order.DTD);
    assertThat(layout.validation()).isEqualTo(XmlLayout.Validation.WARNING);
    assertThat(layout.property("LEG_NO")).isEqualTo("number");
    assertThat(layout.property("UNMAPPED")).isEqualTo("UNMAPPED");
    assertThat(layout.groups()).containsEntry("TOUR", "legs");
  }

  @Test
  void loadsAFileAndSeveralDtds(@TempDir Path dir) throws Exception {
    Files.writeString(dir.resolve("base.dtd"), "<!ELEMENT R (A)> <!ELEMENT A (#PCDATA)>");
    Files.writeString(dir.resolve("more.dtd"), "<!ELEMENT B (#PCDATA)>");
    Files.writeString(
        dir.resolve("layout.yaml"),
        """
        dtd: [base.dtd, more.dtd]
        root: R
        declaration: none
        indent: false
        nulls: nil
        order: alphabetical
        lineBreaks: keep
        doctype: reject
        validation: error
        """);

    XmlLayout layout = XmlLayout.load(dir.resolve("layout.yaml"));

    assertThat(layout.dtd().elements()).containsOnlyKeys("R", "A", "B");
    assertThat(layout.declaration()).isEmpty();
    assertThat(layout.indent()).isFalse();
    assertThat(layout.nulls()).isEqualTo(XmlLayout.Nulls.NIL);
    assertThat(layout.order()).isEqualTo(XmlLayout.Order.ALPHABETICAL);
    assertThat(layout.lineBreaks()).isEqualTo(XmlLayout.LineBreaks.KEEP);
    assertThat(layout.doctype()).isEqualTo(XmlLayout.Doctype.REJECT);
    assertThat(layout.validation()).isEqualTo(XmlLayout.Validation.ERROR);
  }

  @Test
  void toBuilderKeepsEverythingAndFollowsANewEncoding() {
    XmlLayout layout = Tours.layout();

    XmlLayout utf8 = layout.toBuilder().encoding(StandardCharsets.UTF_8).build();

    assertThat(utf8.declaration()).contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    assertThat(utf8.properties()).isEqualTo(layout.properties());
    assertThat(utf8.groups()).isEqualTo(layout.groups());
    assertThat(utf8.nullObjects()).isEqualTo(layout.nullObjects());
    XmlLayout explicit = layout.toBuilder().declaration("<?xml version=\"1.0\"?>").build();
    assertThat(explicit.toBuilder().encoding(StandardCharsets.UTF_8).build().declaration())
        .contains("<?xml version=\"1.0\"?>");
  }

  @Test
  void reportsEveryError() {
    String yaml =
        """
        dtd: test.dtd
        root: R
        colour: blue
        nulls: maybe
        encoding: no-such-charset
        properties:
          X: x
        groups:
          A: as
          R: rs
          G: gs
        """;

    assertThatThrownBy(() ->
            XmlLayout.load(
                "test.yaml",
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)),
                name -> DTD))
        .isInstanceOfSatisfying(
            XmlLayoutException.class,
            e ->
                assertThat(e.errors())
                    .hasSize(6)
                    .anySatisfy(m -> assertThat(m).contains("colour: unknown key"))
                    .anySatisfy(m -> assertThat(m).contains("nulls: unknown value 'maybe'"))
                    .anySatisfy(m -> assertThat(m).contains("encoding: unsupported charset"))
                    .anySatisfy(m -> assertThat(m).contains("properties.X: undeclared element"))
                    .anySatisfy(m -> assertThat(m).contains("groups.A: element A has no element"))
                    .anySatisfy(m -> assertThat(m).contains("groups.R: the content model (G)")));
  }

  @Test
  void requiresADtdAndARootThatIsDeclared() {
    assertThatThrownBy(() -> XmlLayout.builder(DTD, "X").build())
        .isInstanceOf(XmlLayoutException.class)
        .hasMessageContaining("root: element X is not declared");
    assertThatThrownBy(() ->
            XmlLayout.load(
                "empty.yaml",
                new ByteArrayInputStream("root: R".getBytes(StandardCharsets.UTF_8)),
                name -> DTD))
        .isInstanceOf(XmlLayoutException.class)
        .hasMessageContaining("dtd: one DTD or a list of DTDs is required");
  }
}

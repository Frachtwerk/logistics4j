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

import de.frachtwerk.dtd4j.ContentModel.Children;
import java.util.List;
import org.junit.jupiter.api.Test;

class DtdTest {

  private static final Dtd TOURS =
      Dtd.loadClasspath("dtd/tours.dtd", DtdTest.class.getClassLoader());

  @Test
  void readsTheContentModels() {
    assertThat(content("TOURS")).hasToString("(TOUR)");
    assertThat(content("TOUR")).hasToString("(LEG_HEAD?, SHIPMENT+)+");
    assertThat(content("HEAD")).hasToString("(ID, SENDER?, STATUS, NOTE?)");
    assertThat(content("CODES")).hasToString("(KIND, VALUE, STATE?)*");
    assertThat(content("ID")).isInstanceOf(ContentModel.Text.class);
    assertThat(((Children) content("TOUR")).particle().occurs()).isEqualTo(Occurs.ONE_OR_MORE);
  }

  @Test
  void readsChoicesMixedEmptyAndAny() {
    Dtd dtd =
        Dtd.parse(
            "test.dtd",
            """
            <!ELEMENT A ((B | C)*, D?)>
            <!ELEMENT B (#PCDATA | C)*>
            <!ELEMENT C EMPTY>
            <!ELEMENT D ANY>
            """);

    assertThat(dtd.element("A").orElseThrow().content()).hasToString("((B | C)*, D?)");
    assertThat(dtd.element("B").orElseThrow().content()).hasToString("(#PCDATA | C)*");
    assertThat(dtd.element("C").orElseThrow().content()).isInstanceOf(ContentModel.Empty.class);
    assertThat(dtd.element("D").orElseThrow().content()).isInstanceOf(ContentModel.Any.class);
  }

  @Test
  void decodesTheCharsetOfTheTextDeclaration() {
    // the comment of tours.dtd contains umlauts in ISO-8859-1; parsing must not fail
    assertThat(TOURS.elements()).containsKeys("TOURS", "STATE");
  }

  @Test
  void reportsEveryError() {
    String text =
        """
        <!ELEMENT A (B, C>
        <!ELEMENT B (#PCDATA)>
        <!ELEMENT B (#PCDATA)>
        <!ENTITY % e "x">
        <!ELEMENT D (E)>
        """;

    assertThatThrownBy(() -> Dtd.parse("test.dtd", text))
        .isInstanceOfSatisfying(
            DtdException.class,
            e ->
                assertThat(e.errors())
                    .anySatisfy(m -> assertThat(m).startsWith("test.dtd:1: expected"))
                    .anySatisfy(
                        m -> assertThat(m).contains("test.dtd:3: element B is declared twice"))
                    .anySatisfy(
                        m -> assertThat(m).contains("test.dtd:4: entities are not supported"))
                    .anySatisfy(m -> assertThat(m).contains("D: undeclared element E")));
  }

  @Test
  void combinesSeveralDtds() {
    Dtd base = Dtd.parse("base.dtd", "<!ELEMENT A (B)> <!ELEMENT B (#PCDATA)>");
    Dtd more = Dtd.parse("more.dtd", "<!ELEMENT C (#PCDATA)>");

    assertThat(Dtd.of(List.of(base, more)).elements()).containsOnlyKeys("A", "B", "C");
    assertThatThrownBy(() -> Dtd.of(List.of(base, base)))
        .isInstanceOf(DtdException.class)
        .hasMessageContaining("element A is declared in more than one DTD");
  }

  private static ContentModel content(String element) {
    return TOURS.element(element).orElseThrow().content();
  }
}

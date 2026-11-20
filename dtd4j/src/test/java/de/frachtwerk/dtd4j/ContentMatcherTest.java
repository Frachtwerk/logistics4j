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

import de.frachtwerk.dtd4j.ContentMatcher.Items;
import de.frachtwerk.dtd4j.ContentModel.Children;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContentMatcherTest {

  private static Particle model(String content) {
    Dtd dtd =
        Dtd.parse(
            "test.dtd",
            "<!ELEMENT X "
                + content
                + "> <!ELEMENT A EMPTY> <!ELEMENT B EMPTY> <!ELEMENT C EMPTY>");
    return ((Children) dtd.element("X").orElseThrow().content()).particle();
  }

  @Test
  void splitsARepeatedGroupIntoItsOccurrences() {
    Particle particle = model("(A?, B+)+");

    var match = ContentMatcher.match(particle, List.of("A", "B", "B", "A", "B", "B", "B"));

    assertThat(match).isPresent();
    assertThat(match.get().occurrences()).hasSize(2);
    Items second = (Items) match.get().occurrences().get(1);
    assertThat(second.parts().get(1).occurrences()).hasSize(3);
  }

  @Test
  void startsANewOccurrenceWhenAnElementComesAgain() {
    Particle particle = model("(A?, B, C*)+");

    var match = ContentMatcher.match(particle, List.of("A", "B", "C", "C", "B", "B", "C"));

    assertThat(match).isPresent();
    assertThat(match.get().occurrences()).hasSize(3);
  }

  @Test
  void rejectsWhatDoesNotMatch() {
    assertThat(ContentMatcher.match(model("(A, B?)"), List.of("B", "A"))).isEmpty();
    assertThat(ContentMatcher.match(model("(A, B)"), List.of("A"))).isEmpty();
    assertThat(ContentMatcher.match(model("(A | B)"), List.of("A", "B"))).isEmpty();
    assertThat(ContentMatcher.match(model("(A, B*)"), List.of("A"))).isPresent();
    assertThat(ContentMatcher.match(model("(A*)"), List.of())).isPresent();
  }
}

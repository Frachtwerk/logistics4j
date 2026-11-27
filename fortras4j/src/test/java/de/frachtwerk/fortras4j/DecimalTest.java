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

package de.frachtwerk.fortras4j;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DecimalTest {

  private static final Fortras4j FORTRAS =
      Fortras4j.of(
          Demo.withLayout(
                  Demo.layout(
                      """
                      A:
                        fields:
                          - { name: amount, start: 2, end: 9, type: decimal, point: explicit }
                      """,
                      "- { record: A, as: a }"))
              .build());

  @ParameterizedTest
  @ValueSource(strings = {"  -12.50", "      12", "     12.", "     .25", "   +1.25"})
  void readsNumbersWithAPoint(String value) {
    FortrasDocument document = FORTRAS.read(List.of("A" + value));

    assertThat(document.problems()).isEmpty();
    assertThat(document.records("A").getFirst().getDecimal("amount"))
        .isEqualByComparingTo(new BigDecimal(value.strip()));
    assertThat(FORTRAS.write(document)).containsExactly("A" + value);
  }

  @ParameterizedTest
  @ValueSource(strings = {"     1E3", "  1.5e-2", "    12,5", "   1.2.3", "       ."})
  void reportsTextThatIsNoNumberWithAPoint(String value) {
    assertThat(FORTRAS.read(List.of("A" + value)).problems())
        .singleElement()
        .satisfies(p -> {
          assertThat(p.code()).isEqualTo(ProblemCode.INVALID_VALUE);
          assertThat(p.field()).isEqualTo("amount");
        });
  }

  @Test
  void writesTheShortestTextLeftAligned() {
    FortrasDocument document = FORTRAS.read(List.of("A    1.00"));
    document.records("A").getFirst().set("amount", new BigDecimal("-3.140"));

    assertThat(FORTRAS.write(document)).containsExactly("A-3.14   ");
  }
}

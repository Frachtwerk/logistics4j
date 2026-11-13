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

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class DateTimeTest {

  private static final Fortras4j FORTRAS =
      Fortras4j.of(
          Demo.withLayout(
                  Demo.layout(
                      """
                      A:
                        fields:
                          - { name: stamp, start: 2, end: 17, type: datetime, pattern: ddMMyyyyHHmmssSS }
                          - { name: zeros, start: 18, end: 29, type: datetime, null: zeros }
                      """,
                      "- { record: A, as: a }"))
              .build());

  record Line(Stamps a) {}

  record Stamps(LocalDateTime stamp, LocalDateTime zeros) {}

  private static List<Problem> problems(String line) {
    return FORTRAS.read(List.of(line)).problems();
  }

  @Test
  void readsAndWritesDateTimes() {
    String line = "A1311202614300045" + "010220260304";
    FortrasDocument document = FORTRAS.read(List.of(line));
    Record a = document.records("A").getFirst();

    assertThat(document.problems()).isEmpty();
    assertThat(a.getDateTime("stamp"))
        .isEqualTo(LocalDateTime.of(2026, 11, 13, 14, 30, 0, 450_000_000));
    // default pattern ddMMyyyyHHmm
    assertThat(a.getDateTime("zeros")).isEqualTo(LocalDateTime.of(2026, 2, 1, 3, 4));
    assertThat(FORTRAS.write(document)).containsExactly(line);

    a.set("stamp", "2026-01-02T03:04:05.06");
    a.set("zeros", null);
    assertThat(FORTRAS.write(document)).containsExactly("A0201202603040506" + "0".repeat(12));
  }

  @Test
  void reportsInvalidDateTimes() {
    assertThat(problems("A3213202614300045"))
        .singleElement()
        .satisfies(p -> {
          assertThat(p.code()).isEqualTo(ProblemCode.INVALID_VALUE);
          assertThat(p.field()).isEqualTo("stamp");
          assertThat(p.message()).isEqualTo("'3213202614300045' is not a valid datetime at 2-17");
        });
  }

  @Test
  void readsZerosAsEmptyOnlyWithNullZeros() {
    assertThat(problems("A" + "0".repeat(16) + "0".repeat(12)))
        .singleElement()
        .extracting(Problem::field)
        .isEqualTo("stamp");
  }

  @Test
  void bindsDateTimes() {
    String line = "A1311202614300045" + "010220260304";

    Line bound = FORTRAS.read(List.of(line), Line.class);

    assertThat(bound.a().zeros()).isEqualTo(LocalDateTime.of(2026, 2, 1, 3, 4));
    assertThat(FORTRAS.write(bound, "t")).containsExactly(line);
  }
}

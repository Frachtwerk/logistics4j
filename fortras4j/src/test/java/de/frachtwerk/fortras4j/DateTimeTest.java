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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

  private static final Fortras4j DATES =
      Fortras4j.of(
          Demo.withLayout(
                  Demo.layout(
                      """
                      A:
                        fields:
                          - { name: date, start: 2, end: 9, type: date }
                          - { name: time, start: 10, end: 13, type: time }
                          - { name: stamp, start: 14, end: 25, type: datetime }
                          - { name: short, start: 26, end: 31, type: date, pattern: ddMMyy }
                          - { name: iso, start: 32, end: 39, type: date, pattern: uuuuMMdd }
                      """,
                      "- { record: A, as: a }"))
              .build());

  @Test
  void readsValidDatesAndTimes() {
    String line = "A29022028" + "2359" + "311220262359" + "010126" + "20260131";
    FortrasDocument document = DATES.read(List.of(line));
    Record a = document.records("A").getFirst();

    assertThat(document.problems()).isEmpty();
    assertThat(a.getDate("date")).isEqualTo(LocalDate.of(2028, 2, 29));
    assertThat(a.getTime("time")).isEqualTo(LocalTime.of(23, 59));
    assertThat(a.getDateTime("stamp")).isEqualTo(LocalDateTime.of(2026, 12, 31, 23, 59));
    assertThat(a.getDate("short")).isEqualTo(LocalDate.of(2026, 1, 1));
    // u is a proleptic year without an era, unlike y
    assertThat(a.getDate("iso")).isEqualTo(LocalDate.of(2026, 1, 31));
    assertThat(DATES.write(document)).containsExactly(line);
  }

  @Test
  void readsAndWritesTheYearZeroWithAProlepticYear() {
    // an era set by default for u contradicted the year 0 and broke the round trip
    String line = "A01012026" + "0000" + "010120260000" + "010126" + "00000101";
    FortrasDocument document = DATES.read(List.of(line));

    assertThat(document.problems()).isEmpty();
    assertThat(document.records("A").getFirst().getDate("iso")).isEqualTo(LocalDate.of(0, 1, 1));
    assertThat(DATES.write(document)).containsExactly(line);

    document.records("A").getFirst().set("iso", LocalDate.of(0, 1, 2));
    assertThat(DATES.write(document))
        .containsExactly("A01012026" + "0000" + "010120260000" + "010126" + "00000102");
  }

  @Test
  void reportsDaysAndTimesThatDoNotExist() {
    // before, a day past the end of the month was moved to the last day without a problem
    assertThat(
            DATES
                .read(List.of("A31022026" + "2400" + "290220270000" + "300226" + "20260231"))
                .problems())
        .extracting(Problem::code, Problem::field)
        .containsExactly(
            tuple(ProblemCode.INVALID_VALUE, "date"),
            tuple(ProblemCode.INVALID_VALUE, "time"),
            tuple(ProblemCode.INVALID_VALUE, "stamp"),
            tuple(ProblemCode.INVALID_VALUE, "short"),
            tuple(ProblemCode.INVALID_VALUE, "iso"));
  }

  @Test
  void rejectsADateFromPartsThatDoesNotExist() {
    Record a =
        DATES
            .read(List.of("A01012026" + "0000" + "010120260000" + "010126" + "20260101"))
            .records("A")
            .getFirst();
    List<Integer> parts = List.of(2026, 2, 31);

    // Jackson writes a LocalDate as [year, month, day]; before, the DateTimeException escaped
    assertThatThrownBy(() -> a.set("date", parts))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("A.date: ");
  }

  @Test
  void bindsDateTimes() {
    String line = "A1311202614300045" + "010220260304";

    Line bound = FORTRAS.read(List.of(line), Line.class);

    assertThat(bound.a().zeros()).isEqualTo(LocalDateTime.of(2026, 2, 1, 3, 4));
    assertThat(FORTRAS.write(bound, "t")).containsExactly(line);
  }
}

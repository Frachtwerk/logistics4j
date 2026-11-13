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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReaderTest {

  private static final FortrasReader READER = FortrasReader.of(Demo.SCHEMAS);

  @Test
  void readsRecordsIntoGroupsSeparatedByKey() {
    FortrasDocument document =
        READER.read(
            List.of(
                "HDR00112112026",
                "ORD001RED001250Y",
                "ITM001SN1abcdN2efgh",
                "ITM001M",
                "ORD002GRN000100N",
                "MSGhello",
                "END0006"));

    assertThat(document.problems()).isEmpty();
    List<Group> orders = document.root().groups("orders");
    assertThat(orders).hasSize(2);
    Record order = orders.getFirst().record("ORD").orElseThrow();
    assertThat(order.getInt("orderNo")).isEqualTo(1);
    assertThat(order.getDecimal("weight")).isEqualTo(new BigDecimal("12.50"));
    assertThat(order.getBoolean("express")).isTrue();
    assertThat(orders.getFirst().records("ITM")).hasSize(2);
    assertThat(orders.getFirst().records("ITM").getFirst().getBlocks("notes"))
        .extracting(b -> b.get("text"))
        .containsExactly("abcd", "efgh");
    assertThat(document.root().record("MSG").orElseThrow().getString("text")).isEqualTo("hello");
  }

  @Test
  void keyChangeStartsNewGroup() {
    FortrasDocument document =
        READER.read(List.of("HDR00112112026", "ORD001RED", "ITM002S", "END0002"));

    // ITM 002 does not belong to order 001 and cannot start an order on its own
    assertThat(document.errors())
        .extracting(Problem::code)
        .containsExactly(ProblemCode.UNEXPECTED_RECORD);
  }

  @Test
  void tolerantKeyKeepsSubordinateRecordsAndWarns() {
    String yaml =
        Demo.layout(
                """
                ORD:
                  fields:
                    - { name: no, start: 4, end: 6, type: integer }
                ITM:
                  fields:
                    - { name: no, start: 4, end: 6, type: integer }
                """,
                """
                - group: orders
                  occurs: "1..*"
                  key: no
                  keyMismatch: warn
                  children:
                    - { record: ORD }
                    - { record: ITM, occurs: "*" }
                """)
            .replace("^A", "^ORD");
    FortrasReader reader = FortrasReader.of(Demo.withLayout(yaml).build());

    FortrasDocument document =
        reader.read(List.of("ORD001", "ITM000", "ITM001", "ORD002", "ITM002"));

    assertThat(document.root().groups("orders")).hasSize(2);
    assertThat(document.root().groups("orders").getFirst().records("ITM")).hasSize(2);
    assertThat(document.problems())
        .singleElement()
        .satisfies(p -> {
          assertThat(p.severity()).isEqualTo(Severity.WARNING);
          assertThat(p.code()).isEqualTo(ProblemCode.KEY_MISMATCH);
          assertThat(p.lineNumber()).isEqualTo(2);
        });
  }

  @Test
  void reportsValueProblemsAndKeepsReading() {
    FortrasDocument document =
        READER.read(List.of("HDR001", "ORD00xBLU0012x0Q", "ITM001Z", "END0002"));

    assertThat(document.problems())
        .extracting(Problem::severity, Problem::code, Problem::field)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(
                Severity.ERROR, ProblemCode.MISSING_VALUE, "created"),
            org.assertj.core.groups.Tuple.tuple(
                Severity.ERROR, ProblemCode.INVALID_VALUE, "orderNo"),
            org.assertj.core.groups.Tuple.tuple(Severity.ERROR, ProblemCode.UNKNOWN_CODE, "colour"),
            org.assertj.core.groups.Tuple.tuple(
                Severity.ERROR, ProblemCode.INVALID_VALUE, "weight"),
            org.assertj.core.groups.Tuple.tuple(
                Severity.ERROR, ProblemCode.INVALID_VALUE, "express"),
            org.assertj.core.groups.Tuple.tuple(
                Severity.WARNING, ProblemCode.UNKNOWN_CODE, "size"));
    // unknown codes are retained
    assertThat(document.records("ORD").getFirst().getString("colour")).isEqualTo("BLU");
  }

  @Test
  void checksOccurrencesAndConstraints() {
    FortrasDocument document =
        READER.read(
            List.of(
                "HDR00112112026",
                "ORD001RED",
                "ITM001S",
                "ITM001S",
                "ITM001M",
                "ITM001L",
                "END0005"));

    assertThat(document.errors())
        .extracting(Problem::code, Problem::lineNumber)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(ProblemCode.DUPLICATE_VALUE, 4),
            org.assertj.core.groups.Tuple.tuple(ProblemCode.TOO_MANY, 6));
  }

  @Test
  void reportsMissingMandatoryElements() {
    FortrasDocument document = READER.read(List.of("HDR00112112026", "END0000"));

    assertThat(document.errors())
        .singleElement()
        .satisfies(p -> {
          assertThat(p.code()).isEqualTo(ProblemCode.MISSING);
          assertThat(p.field()).isEqualTo("entries");
          assertThat(p.lineNumber()).isEqualTo(2);
        });
  }

  @Test
  void keepsUnknownAndPassthroughLines() {
    FortrasDocument document =
        READER.read(List.of("HDR00112112026", "MSGa", "X99bilateral", "???", "END0001"));

    assertThat(document.errors())
        .singleElement()
        .satisfies(p -> assertThat(p.code()).isEqualTo(ProblemCode.UNKNOWN_RECORD));
    assertThat(document.records()).hasSize(5);
  }

  @Test
  void toleratesShortLinesAndTrailingEmptyLines() {
    FortrasDocument document = READER.read(List.of("HDR00112112026", "ORD001", "END0001", "", ""));

    assertThat(document.problems()).isEmpty();
    Record order = document.records("ORD").getFirst();
    assertThat(order.get("colour")).isNull();
    assertThat(order.getDecimal("weight")).isNull();
  }

  @Test
  void failsOnErrorWhenAsked() {
    FortrasReader strict = READER.failOnError(true);
    List<String> lines = List.of("HDR00112112026", "END0000");

    assertThatThrownBy(() -> strict.read(lines))
        .isInstanceOf(FortrasException.class)
        .hasMessageContaining("MISSING");
  }

  @Test
  void rejectsFilesWithoutMatchingLayout() {
    List<String> lines = List.of("@@PHBORD100");

    assertThatThrownBy(() -> READER.read(lines))
        .isInstanceOf(FortrasException.class)
        .satisfies(e ->
            assertThat(((FortrasException) e).problems())
                .extracting(Problem::code)
                .containsExactly(ProblemCode.NO_SCHEMA));
  }

  @Test
  void readsDatesWithPattern() {
    Record header = READER.read(List.of("HDR00131122026", "MSGx", "END0001")).records().getFirst();

    assertThat(header.getDate("created")).isEqualTo(LocalDate.of(2026, 12, 31));
  }
}

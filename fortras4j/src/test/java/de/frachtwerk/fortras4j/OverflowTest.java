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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import de.frachtwerk.fortras4j.schema.FortrasSchema;
import de.frachtwerk.fortras4j.schema.FortrasSchemas;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class OverflowTest {

  private static final String LAYOUT =
      """
      id: overflow
      format: OVERFLOW
      release: "1"
      detect: "^TXT"
      codelists:
        hub:
          strict: false
          codes: { ABC: a }
      records:
        TXT:
          fields:
            - { name: plain, start: 4, end: 6 }
            - { name: cut, start: 7, end: 9, overflow: truncate }
            - { name: strict, start: 10, end: 12, overflow: error }
            - { name: limited, start: 13, end: 16, maxLength: 2, overflow: truncate }
            - { name: hub, start: 17, end: 19, type: code, codelist: hub, overflow: truncate }
      structure:
        - { record: TXT }
      """;

  private static final FortrasSchema SCHEMA =
      FortrasSchemas.builder()
          .add("overflow.yaml", new ByteArrayInputStream(LAYOUT.getBytes(UTF_8)))
          .build()
          .get("overflow");

  private static WriteResult write(String field, String value) {
    Record rec = Record.create(SCHEMA, "TXT").set(field, value);
    return FortrasWriter.create().failOnError(false).write(SCHEMA, List.of(rec));
  }

  @Test
  void truncateCutsTextToTheFieldLength() {
    assertThat(write("plain", "ABCD").problems()).isEmpty();
    assertThat(write("cut", "ABCD").lines().getFirst()).startsWith("TXT   ABC");
    assertThat(write("cut", "ABCD").problems()).isEmpty();
    assertThat(write("hub", "BRLX").lines().getFirst()).endsWith("BRL");
    assertThat(write("hub", "BRLX").problems()).isEmpty();
  }

  @Test
  void errorReportsTooLongText() {
    assertThat(write("strict", "ABCD").problems())
        .extracting(Problem::code, Problem::field, Problem::message)
        .containsExactly(
            tuple(
                ProblemCode.VALUE_TOO_LONG,
                "strict",
                "value 'ABCD' exceeds the allowed length of 3"));
  }

  @Test
  void ownMaxLengthStaysAHardLimit() {
    assertThat(write("limited", "ABC").problems())
        .extracting(Problem::code)
        .containsExactly(ProblemCode.VALUE_TOO_LONG);
    assertThat(write("limited", "AB").problems()).isEmpty();
  }
}

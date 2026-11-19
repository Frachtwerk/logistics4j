/*
 * Copyright (C) 2026-7613 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

class ControlTest {

  private static final String LAYOUT =
      """
      id: control
      format: CONTROL
      release: "1"
      detect: "^TXT"
      records:
        TXT:
          fields:
            - { name: replaced, start: 4, end: 15 }
            - { name: strict, start: 16, end: 27, control: error }
            - { name: kept, start: 28, end: 39, control: keep }
      structure:
        - { record: TXT }
      """;

  private static final FortrasSchema SCHEMA =
      FortrasSchemas.builder()
          .add("control.yaml", new ByteArrayInputStream(LAYOUT.getBytes(UTF_8)))
          .build()
          .get("control");

  private static WriteResult write(String field, String value) {
    Record rec = Record.create(SCHEMA, "TXT").set(field, value);
    return FortrasWriter.create().failOnError(false).write(SCHEMA, List.of(rec));
  }

  private static String slice(WriteResult result, int start, int end) {
    return result.lines().getFirst().substring(start - 1, end);
  }

  @Test
  void replaceWritesASpaceForEachLineBreakAndControlCharacter() {
    assertThat(slice(write("replaced", "foo\nbar baz"), 4, 15)).isEqualTo("foo bar baz ");
    assertThat(slice(write("replaced", "a\r\nb\tc\u2028d"), 4, 15)).isEqualTo("a b c d     ");
    assertThat(write("replaced", "foo\nbar").problems()).isEmpty();
  }

  @Test
  void errorReportsTheFirstControlCharacter() {
    assertThat(write("strict", "foo\tbar").problems())
        .extracting(Problem::code, Problem::field, Problem::message)
        .containsExactly(
            tuple(
                ProblemCode.INVALID_VALUE,
                "strict",
                "value contains the control character U+0009 at position 4"));
    assertThat(write("strict", "foo bar").problems()).isEmpty();
  }

  @Test
  void keepWritesTheValueUnchanged() {
    assertThat(slice(write("kept", "a\tb"), 28, 39)).isEqualTo("a\tb         ");
  }
}

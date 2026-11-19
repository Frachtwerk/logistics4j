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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import de.frachtwerk.fortras4j.schema.FortrasSchemas;
import de.frachtwerk.fortras4j.schema.SchemaException;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class LineLengthTest {

  private static final String LAYOUT =
      """
      id: lengths
      format: LENGTHS
      release: "1"
      detect: "^TXT"
      minLineLength: 6
      maxLineLength: 9
      records:
        TXT:
          fields:
            - { name: text, start: 4, end: 9 }
      structure:
        - { record: TXT, occurs: "1..*" }
      """;

  private static final FortrasReader READER = FortrasReader.of(schemas(LAYOUT));

  private static FortrasSchemas schemas(String layout) {
    return FortrasSchemas.builder()
        .add("lengths.yaml", new ByteArrayInputStream(layout.getBytes(UTF_8)))
        .build();
  }

  @Test
  void reportsLinesShorterThanMinLineLength() {
    assertThat(READER.read(List.of("TXTabc", "TXTa")).problems())
        .extracting(Problem::code, Problem::lineNumber, Problem::message)
        .containsExactly(tuple(ProblemCode.LINE_TOO_SHORT, 2, "4 characters, at least 6 required"));
  }

  @Test
  void removesLineTerminatorsAtTheEndOfALine() {
    FortrasDocument document = READER.read(List.of("TXTabcdef\r", "TXTabc\r\n", "\r"));

    assertThat(document.problems()).isEmpty();
    assertThat(document.records("TXT"))
        .extracting(Record::raw)
        .containsExactly("TXTabcdef", "TXTabc");
  }

  @Test
  void rejectsMinLineLengthAboveMaxLineLength() {
    assertThatThrownBy(() -> schemas(LAYOUT.replace("minLineLength: 6", "minLineLength: 10")))
        .isInstanceOf(SchemaException.class)
        .satisfies(e ->
            assertThat(((SchemaException) e).errors())
                .anyMatch(m -> m.contains("must be between 0 and maxLineLength 9")));
  }
}

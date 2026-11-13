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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import de.frachtwerk.fortras4j.schema.FortrasSchema;
import de.frachtwerk.fortras4j.schema.FortrasSchemas;
import de.frachtwerk.fortras4j.type.Width;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WidthTest {

  private static final FortrasSchemas SCHEMAS =
      FortrasSchemas.builder().addClasspath("layouts/widths.yaml").build();
  private static final FortrasSchema SCHEMA = SCHEMAS.get("widths");
  private static final FortrasReader CHARS = FortrasReader.of(SCHEMAS);
  private static final FortrasReader UTF8 = CHARS.widthsIn(UTF_8);
  private static final FortrasWriter WRITER = FortrasWriter.create().validate(false);

  /** 23 characters, 28 bytes in UTF-8: every field holds a multibyte character. */
  private static final String UTF8_LINE = "NAM" + "Müll " + " Köln" + "€ " + "042" + "äb" + "xyz";

  private static Record first(FortrasDocument document) {
    return document.records("NAM").getFirst();
  }

  private static Record sample() {
    return Record.create(SCHEMA, "NAM")
        .set("name", "Müll")
        .set("city", "Köln")
        .set("exact", "€")
        .set("count", 42)
        .set("notes", List.of(Map.of("text", "äb"), Map.of("text", "xyz")));
  }

  @Test
  void readsFieldsByBytes() {
    FortrasDocument document = UTF8.read(List.of(UTF8_LINE));

    assertThat(document.problems()).isEmpty();
    Record rec = first(document);
    assertThat(rec.getString("name")).isEqualTo("Müll");
    assertThat(rec.getString("city")).isEqualTo("Köln");
    assertThat(rec.getString("exact")).isEqualTo("€");
    assertThat(rec.getInt("count")).isEqualTo(42);
    assertThat(rec.values())
        .containsEntry("notes", List.of(Map.of("text", "äb"), Map.of("text", "xyz")));
  }

  @Test
  void charactersMisreadTheSameLine() {
    Record rec = first(CHARS.read(List.of(UTF8_LINE)));

    assertThat(rec.getString("city")).isNotEqualTo("Köln");
  }

  @Test
  void writesFieldsByBytes() {
    List<String> lines = WRITER.widthsIn(UTF_8).write(SCHEMA, List.of(sample())).lines();

    assertThat(lines).containsExactly(UTF8_LINE);
    assertThat(lines.getFirst().getBytes(UTF_8)).hasSize(28);
  }

  @Test
  void truncatesWithoutSplittingACharacter() {
    Record rec = sample().set("name", "Abcdeü").set("city", "äbcdef");

    String line = WRITER.widthsIn(UTF_8).write(SCHEMA, List.of(rec)).lines().getFirst();

    // ü no longer fits behind "Abcde", ä no longer fits before "bcdef": both are padded instead
    assertThat(line).startsWith("NAM" + "Abcde " + " bcdef");
    assertThat(line.getBytes(UTF_8)).hasSize(28);
  }

  @Test
  void reportsOverflowInBytes() {
    Record rec = sample().set("exact", "Käse");

    WriteResult result = WRITER.widthsIn(UTF_8).failOnError(false).write(SCHEMA, List.of(rec));

    assertThat(result.problems())
        .extracting(Problem::code, Problem::field, Problem::message)
        .containsExactly(
            tuple(ProblemCode.VALUE_TOO_LONG, "exact", "value 'Käse' does not fit into 4 bytes"));
    assertThat(WRITER.write(SCHEMA, List.of(rec)).problems()).isEmpty();
  }

  @Test
  void reportsFieldBoundariesInsideACharacter() {
    // bytes 9 and 10 hold ü, so the boundary between name (4-9) and city (10-15) splits it
    String line = "NAM" + "Abcdeü" + "Köln" + "ABCD" + "042" + "abcxyz";

    FortrasDocument document = UTF8.read(List.of(line));

    assertThat(document.problems())
        .extracting(Problem::code, Problem::field, Problem::message)
        .containsExactly(
            tuple(
                ProblemCode.INVALID_VALUE,
                "name",
                "the field boundaries split a multibyte character at 4-9"),
            tuple(
                ProblemCode.INVALID_VALUE,
                "city",
                "the field boundaries split a multibyte character at 10-15"));
    Record rec = first(document);
    assertThat(rec.getString("name")).isNull();
    assertThat(rec.getString("exact")).isEqualTo("ABCD");
    assertThat(WRITER.widthsIn(UTF_8).failOnError(false).write(document).lines())
        .containsExactly(line);
  }

  @Test
  void countsTheLineLengthInBytes() {
    String line = UTF8_LINE + "x";

    assertThat(UTF8.read(List.of(line)).problems())
        .extracting(Problem::code, Problem::message)
        .containsExactly(tuple(ProblemCode.LINE_TOO_LONG, "29 bytes, at most 28 allowed"));
    assertThat(CHARS.read(List.of(line)).problems())
        .extracting(Problem::code)
        .doesNotContain(ProblemCode.LINE_TOO_LONG);
  }

  @Test
  void keepsUnchangedRecordsAndOverlaysChangedFields() {
    FortrasDocument document = UTF8.read(List.of(UTF8_LINE));
    FortrasWriter writer = WRITER.widthsIn(UTF_8);

    assertThat(writer.write(document).lines()).containsExactly(UTF8_LINE);

    first(document).set("count", 7);
    assertThat(writer.write(document).lines())
        .containsExactly("NAM" + "Müll " + " Köln" + "€ " + "007" + "äb" + "xyz");
  }

  @ParameterizedTest
  @ValueSource(strings = {"windows-1252", "IBM850", "ISO-8859-1"})
  void singleByteCharsetsCountLikeCharacters(String name) {
    Charset charset = Charset.forName(name);
    String line = "NAM" + "Müller" + "  Köln" + "ABCD" + "042" + "äbc" + "xyz";

    FortrasDocument chars = CHARS.read(List.of(line));
    FortrasDocument bytes = CHARS.widthsIn(charset).read(List.of(line));

    assertThat(bytes.problems()).isEqualTo(chars.problems()).isEmpty();
    assertThat(first(bytes).values()).isEqualTo(first(chars).values());
    Record rec = sample().set("name", "Müller").set("exact", "ABCD").set("notes", List.of());
    assertThat(WRITER.widthsIn(charset).write(SCHEMA, List.of(rec)).lines())
        .isEqualTo(WRITER.write(SCHEMA, List.of(rec)).lines());
  }

  @Test
  void rejectsCharsetsWithoutSingleByteAscii() {
    assertThatThrownBy(() -> CHARS.widthsIn(StandardCharsets.UTF_16))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not encode ASCII as single bytes");
  }

  @Test
  void bytePaddingFillsUpWithBlanks() {
    Width utf8 = Width.bytes(UTF_8);

    assertThat(utf8.padding('ä', 5)).isEqualTo("ää ");
    assertThat(utf8.head("aäb", 2)).isEqualTo("a");
    assertThat(utf8.tail("aäb", 2)).isEqualTo("b");
    assertThat(Width.chars().head("aäb", 2)).isEqualTo("aä");
  }

  @Test
  void charactersAreTheDefault() {
    assertThat(CHARS.width()).isEqualTo(Width.chars());
    assertThat(FortrasWriter.create().width()).isEqualTo(Width.chars());
    assertThat(UTF8.widthsInCharacters().width()).isEqualTo(Width.chars());
  }
}

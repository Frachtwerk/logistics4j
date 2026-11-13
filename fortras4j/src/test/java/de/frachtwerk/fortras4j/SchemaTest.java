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

import de.frachtwerk.fortras4j.schema.FortrasSchemas;
import de.frachtwerk.fortras4j.schema.ReloadableSchemas;
import de.frachtwerk.fortras4j.schema.SchemaException;
import de.frachtwerk.fortras4j.type.FieldCodec;
import de.frachtwerk.fortras4j.type.FieldContext;
import de.frachtwerk.fortras4j.type.FieldOptions;
import de.frachtwerk.fortras4j.type.FieldType;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SchemaTest {

  @Test
  void reportsAllLayoutErrorsAtOnce() {
    String yaml =
        Demo.layout(
            """
            A:
              fields:
                - { name: a, start: 2, end: 5 }
                - { name: b, start: 4, end: 6 }
                - { name: c, start: 7, end: 8, type: strng }
                - { name: d, start: 9, end: 10, type: integer, overflw: max }
                - { name: e, start: 11, end: 12, type: code, codelist: missing }
            """,
            """
            - { record: A }
            - { record: B, occurs: "1-2" }
            """);

    FortrasSchemas.Builder builder = Demo.withLayout(yaml);

    assertThatThrownBy(builder::build)
        .isInstanceOf(SchemaException.class)
        .satisfies(e ->
            assertThat(((SchemaException) e).errors())
                .anyMatch(m -> m.contains("b (4-6) overlaps a (2-5)"))
                .anyMatch(m -> m.contains("unknown type 'strng'"))
                .anyMatch(m -> m.contains("unknown option(s) [overflw]"))
                .anyMatch(m -> m.contains("unknown codelist 'missing'"))
                .anyMatch(m -> m.contains("unknown record 'B'"))
                .anyMatch(m -> m.contains("invalid '1-2'")));
  }

  @Test
  void rejectsFieldsOverlappingThePrefix() {
    String yaml =
        Demo.layout("ABC:\n  fields:\n    - { name: x, start: 3, end: 4 }", "- { record: ABC }");

    FortrasSchemas.Builder builder = Demo.withLayout(yaml);

    assertThatThrownBy(builder::build).hasMessageContaining("x (3-4) overlaps prefix 'ABC' (1-3)");
  }

  @Test
  void requiresQuotedCodesSoLeadingZerosSurvive() {
    String yaml =
        """
        id: t
        detect: "^A"
        codelists:
          numbers: [100, "002"]
        records:
          A: {}
        structure:
          - { record: A }
        """;

    FortrasSchemas.Builder builder = Demo.withLayout(yaml);

    assertThatThrownBy(builder::build)
        .hasMessageContaining("code 100 is a number; write it as a quoted string");
  }

  @Test
  void validatesStructureReferences() {
    String yaml =
        Demo.layout(
            "A:\n  fields:\n    - { name: x, start: 2, end: 3 }",
            """
            - group: g
              key: nope
              constraints:
                - { unique: y, record: A }
              children:
                - { record: A }
                - { record: A }
            """);

    FortrasSchemas.Builder builder = Demo.withLayout(yaml);

    assertThatThrownBy(builder::build)
        .isInstanceOf(SchemaException.class)
        .satisfies(e ->
            assertThat(((SchemaException) e).errors())
                .anyMatch(m -> m.contains("key 'nope' is no field"))
                .anyMatch(m -> m.contains("unknown field A.y"))
                .anyMatch(m -> m.contains("'A' is used twice")));
  }

  @Test
  void extendsAnotherLayout() {
    String child =
        """
        id: demo-v2
        extends: demo
        detect: "^HDR002"
        codelists:
          colour:
            codes: { RED: Rot, GRN: Grün, BLU: Blau }
        """;

    FortrasSchemas schemas =
        FortrasSchemas.builder()
            .addClasspath("layouts/demo.yaml")
            .add("child.yaml", new ByteArrayInputStream(child.getBytes(StandardCharsets.UTF_8)))
            .build();

    assertThat(schemas.get("demo-v2").records())
        .containsOnlyKeys("HDR", "ORD", "ITM", "MSG", "END");
    assertThat(schemas.get("demo-v2").codeList("colour").contains("BLU")).isTrue();
    assertThat(schemas.get("demo").codeList("colour").contains("BLU")).isFalse();
    assertThat(schemas.detect("HDR00212112026")).map(s -> s.id()).contains("demo-v2");
  }

  @Test
  void supportsCustomFieldTypes() {
    FieldType upper =
        new FieldType() {
          @Override
          public String name() {
            return "upper";
          }

          @Override
          public FieldCodec create(FieldOptions options, FieldContext context) {
            return new FieldCodec() {
              @Override
              public Object parse(String slice) {
                return slice.isBlank() ? null : slice.strip().toUpperCase(Locale.ROOT);
              }

              @Override
              public String format(Object value, int length) {
                String text = value == null ? "" : value.toString();
                return text + " ".repeat(Math.max(0, length - text.length()));
              }

              @Override
              public Object coerce(Object value) {
                return value;
              }
            };
          }
        };
    String yaml =
        Demo.layout(
            "A:\n  fields:\n    - { name: x, start: 2, end: 4, type: upper }", "- { record: A }");

    FortrasSchemas schemas = Demo.withLayout(yaml).type(upper).build();

    assertThat(FortrasReader.of(schemas).read(List.of("Aabc")).records().getFirst().get("x"))
        .isEqualTo("ABC");
  }

  @Test
  void reloadsLayoutsAndKeepsTheOldOnesOnError(@TempDir Path dir) throws Exception {
    Path file = dir.resolve("layout.yaml");
    Files.writeString(
        file, Demo.layout("A:\n  fields:\n    - { name: x, start: 2, end: 3 }", "- { record: A }"));
    ReloadableSchemas schemas =
        new ReloadableSchemas(() -> FortrasSchemas.builder().addDirectory(dir).build());
    FortrasReader reader = FortrasReader.of(schemas);
    assertThat(reader.read(List.of("Aabcd")).records().getFirst().get("x")).isEqualTo("ab");

    Files.writeString(
        file, Demo.layout("A:\n  fields:\n    - { name: x, start: 2, end: 5 }", "- { record: A }"));
    schemas.reload();
    assertThat(reader.read(List.of("Aabcd")).records().getFirst().get("x")).isEqualTo("abcd");

    Files.writeString(file, "id: t\nrecords: {}");
    assertThatThrownBy(schemas::reload).isInstanceOf(SchemaException.class);
    assertThat(reader.read(List.of("Aabcd")).records().getFirst().get("x")).isEqualTo("abcd");
  }
}

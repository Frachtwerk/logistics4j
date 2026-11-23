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
import de.frachtwerk.fortras4j.schema.SchemaException;
import de.frachtwerk.fortras4j.type.FieldCodec;
import de.frachtwerk.fortras4j.type.FieldContext;
import de.frachtwerk.fortras4j.type.FieldOptions;
import de.frachtwerk.fortras4j.type.FieldType;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Times, value prefixes, typed constraint values, codec failures and ambiguous layouts. */
class FindingsTest {

  @Test
  void readsMidnightUnlessZerosMeanEmpty() {
    String yaml =
        Demo.layout(
            """
            A:
              fields:
                - { name: time, start: 2, end: 5, type: time }
                - { name: zeros, start: 6, end: 9, type: time, null: zeros }
                - { name: date, start: 10, end: 17, type: date }
            """,
            "- { record: A }");
    FortrasReader reader = FortrasReader.of(Demo.withLayout(yaml).build());

    Record a = reader.read(List.of("A00000000" + "00000000")).records().getFirst();

    assertThat(a.getTime("time")).isEqualTo(LocalTime.MIDNIGHT);
    assertThat(a.getTime("zeros")).isNull();
    assertThat(a.getDate("date")).isNull();
  }

  @Test
  void countsAValuePrefixOnlyWhereTheLineCarriesIt() {
    String yaml =
        Demo.layout(
                "A:\n  fields:\n    - { name: depot, start: 2, end: 7, type: code, codelist: d, valuePrefix: \"910\" }",
                "- { record: A }")
            + "codelists:\n  d: { strict: false, codes: [\"123456\", \"123\"] }\n";
    FortrasReader reader = FortrasReader.of(Demo.withLayout(yaml).build());

    FortrasDocument withoutPrefix = reader.read(List.of("A123456"));
    FortrasDocument withPrefix = reader.read(List.of("A910123"));

    assertThat(withoutPrefix.problems()).isEmpty();
    assertThat(withoutPrefix.records().getFirst().get("depot")).isEqualTo("123456");
    assertThat(withPrefix.problems()).isEmpty();
    assertThat(withPrefix.records().getFirst().get("depot")).isEqualTo("123");
  }

  @Test
  void comparesConstraintValuesAsTheFieldHoldsThem() {
    String yaml =
        Demo.layout(
            "A:\n  fields:\n    - { name: n, start: 2, end: 4, type: integer }",
            """
            - group: g
              occurs: "1"
              constraints:
                - { first: n, record: A, values: ["001"] }
                - { required: n, record: A, values: ["002"] }
              children:
                - { record: A, occurs: "1..*" }
            """);
    FortrasReader reader = FortrasReader.of(Demo.withLayout(yaml).build());

    assertThat(reader.read(List.of("A001", "A002")).problems()).isEmpty();
    assertThat(reader.read(List.of("A002", "A003")).problems())
        .extracting(Problem::code)
        .containsExactlyInAnyOrder(ProblemCode.UNEXPECTED_VALUE);
    assertThatThrownBy(
            () -> Demo.withLayout(yaml.replace("values: [\"002\"]", "values: [\"x\"]")).build())
        .isInstanceOf(SchemaException.class)
        .hasMessageContaining("constraint value 'x' does not fit A.n");
    assertThatThrownBy(() ->
            Demo.withLayout(yaml.replace("values: [\"002\"]", "values: [\"99999999999999999999\"]"))
                .build())
        .isInstanceOf(SchemaException.class)
        .hasMessageContaining("constraint value '99999999999999999999' does not fit A.n");
    assertThatThrownBy(
            () -> Demo.withLayout(yaml.replace("values: [\"002\"]", "values: [\"\"]")).build())
        .isInstanceOf(SchemaException.class)
        .hasMessageContaining("constraint values of A.n contain an empty value");
  }

  @Test
  void reportsAValueTheCodecCannotWrite() {
    FieldType failing =
        new FieldType() {
          @Override
          public String name() {
            return "failing";
          }

          @Override
          public FieldCodec create(FieldOptions options, FieldContext context) {
            return new FieldCodec() {
              @Override
              public Object parse(String slice) {
                return slice;
              }

              @Override
              public String format(Object value, int length) {
                throw new IllegalArgumentException("cannot write " + value);
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
            "A:\n  fields:\n    - { name: x, start: 2, end: 4, type: failing }", "- { record: A }");
    FortrasSchemas schemas = Demo.withLayout(yaml).type(failing).build();
    Record a = Record.create(schemas.get("t"), "A").set("x", "abc");

    WriteResult result =
        FortrasWriter.create().failOnError(false).write(schemas.get("t"), List.of(a));

    assertThat(result.problems())
        .singleElement()
        .satisfies(p -> {
          assertThat(p.code()).isEqualTo(ProblemCode.INVALID_VALUE);
          assertThat(p.message()).contains("cannot write abc");
        });
  }

  @Test
  void refusesToGuessBetweenLayoutsThatBothMatch() {
    FortrasSchemas schemas =
        FortrasSchemas.builder()
            .add(
                "one.yaml",
                new java.io.ByteArrayInputStream(
                    Demo.layout("A:\n  fields: []", "- { record: A }")
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            .add(
                "two.yaml",
                new java.io.ByteArrayInputStream(
                    Demo.layout("A:\n  fields: []", "- { record: A }")
                        .replace("id: t", "id: u")
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            .build();

    assertThatThrownBy(() -> FortrasReader.of(schemas).read(List.of("A")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("several layouts match");
  }
}

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

import de.frachtwerk.fortras4j.schema.FortrasSchema;
import de.frachtwerk.fortras4j.schema.FortrasSchemas;
import de.frachtwerk.fortras4j.schema.SchemaException;
import de.frachtwerk.fortras4j.schema.ValueRule;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RuleTest {

  private static final FortrasSchemas SCHEMAS =
      FortrasSchemas.builder().addClasspath("layouts/rules.yaml").build();
  private static final Fortras4j FORTRAS = Fortras4j.of(SCHEMAS);
  private static final String STAMP = "1311202614300045";

  private static List<Problem> problems(String line) {
    return FORTRAS.read(List.of(line)).problems();
  }

  @Test
  void acceptsValuesOfTheMatchingCase() {
    assertThat(problems("SCNAAX1100" + STAMP)).isEmpty();
    assertThat(problems("SCNBBX2200" + STAMP)).isEmpty();
  }

  @Test
  void reportsValuesOutsideTheMatchingCase() {
    assertThat(problems("SCNAAX1200" + STAMP))
        .singleElement()
        .satisfies(p -> {
          assertThat(p.severity()).isEqualTo(Severity.WARNING);
          assertThat(p.code()).isEqualTo(ProblemCode.UNEXPECTED_VALUE);
          assertThat(p.recordId()).isEqualTo("SCN");
          assertThat(p.field()).isEqualTo("status");
          assertThat(p.message())
              .isEqualTo("200 is not allowed for kind AA (rule 'status'); allowed: [100, 101]");
        });
  }

  @Test
  void appliesOtherwiseWhenNoCaseMatches() {
    // kind BB matches only with hub X1 or X2
    assertThat(problems("SCNBBX3001" + STAMP)).isEmpty();
    assertThat(problems("SCNBBX3200" + STAMP))
        .singleElement()
        .extracting(Problem::message)
        .isEqualTo("200 is not allowed (rule 'status'); allowed: [1, 2]");
    assertThat(problems("SCNCCX1003" + STAMP)).hasSize(1);
  }

  @Test
  void skipsTheRuleForEmptyOrInvalidFields() {
    assertThat(problems("SCNAAX1   " + STAMP)).isEmpty();
    assertThat(problems("SCN  X1200" + STAMP)).isEmpty();
    // an unknown code of a strict code list is an error, so the rule is not checked
    assertThat(problems("SCNZZX1200" + STAMP))
        .singleElement()
        .extracting(Problem::code)
        .isEqualTo(ProblemCode.UNKNOWN_CODE);
    assertThat(problems("SCNAAX1abc" + STAMP))
        .singleElement()
        .extracting(Problem::code)
        .isEqualTo(ProblemCode.INVALID_VALUE);
  }

  @Test
  void answersTheAllowedValues() {
    ValueRule rule = SCHEMAS.get("rules").rule("status");
    Map<String, Object> values = new HashMap<>();
    values.put("kind", "BB");
    values.put("hub", "X1");

    assertThat(rule.allowed(values)).hasValueSatisfying(v -> assertThat(v).containsExactly(200L));
    values.put("hub", "X9");
    assertThat(rule.allowed(values)).hasValueSatisfying(v -> assertThat(v).containsExactly(1L, 2L));
    values.put("hub", null);
    assertThat(rule.allowed(values)).isEmpty();
    assertThat(SCHEMAS.get("rules").rules("SCN")).containsExactly(rule);
  }

  @Test
  void validatesRules() {
    String yaml =
        Demo.layout(
                """
                A:
                  fields:
                    - { name: kind, start: 2, end: 3 }
                    - { name: code, start: 4, end: 6, type: integer }
                    - repeat: blocks
                      start: 7
                      count: 2
                      stride: 2
                      fields:
                        - { name: x, start: 1, end: 2 }
                """,
                "- { record: A }")
            + """
            rules:
              noRecord: { field: code, otherwise: [1] }
              unknownRecord: { record: B, field: code, otherwise: [1] }
              unknownField: { record: A, field: missing, otherwise: [1] }
              repeatField: { record: A, field: x, otherwise: [a] }
              wrongType: { record: A, field: code, otherwise: [abc] }
              ownField: { record: A, field: code, cases: [{ when: { code: [1] }, allowed: [1] }] }
              nothing: { record: A, field: code }
              badSeverity: { record: A, field: code, severity: fatal, otherwise: [1] }
              unknownKey: { record: A, field: code, otherwise: [1], allow: [2] }
              noAllowed: { record: A, field: code, cases: [{ when: { kind: X } }] }
            """;

    assertThatThrownBy(() -> Demo.withLayout(yaml).build())
        .isInstanceOf(SchemaException.class)
        .satisfies(e ->
            assertThat(((SchemaException) e).errors())
                .anyMatch(m -> m.contains("rules.noRecord.record: is required"))
                .anyMatch(m -> m.contains("rules.unknownRecord.record: unknown record 'B'"))
                .anyMatch(m -> m.contains("'missing' is no field of record A"))
                .anyMatch(m -> m.contains("'x' is no field of record A outside of repeat"))
                .anyMatch(m -> m.startsWith("t: rules.wrongType.otherwise: "))
                .anyMatch(m -> m.contains("a rule cannot depend on its own field"))
                .anyMatch(m -> m.contains("rules.nothing: needs 'cases' or 'otherwise'"))
                .anyMatch(m -> m.contains("must be 'error' or 'warning'"))
                .anyMatch(m -> m.contains("unknown key 'allow'"))
                .anyMatch(m -> m.contains("needs 'when' and 'allowed'")));
  }

  @Test
  void extendingLayoutsReplaceOrDropRulesByName() {
    String replacing =
        """
        id: rules-v2
        extends: rules
        rules:
          status:
            record: SCN
            field: status
            otherwise: [7]
        """;
    String dropping =
        """
        id: rules-v3
        extends: rules
        rules:
          status: null
        """;
    FortrasSchemas schemas =
        FortrasSchemas.builder()
            .addClasspath("layouts/rules.yaml")
            .add("v2.yaml", stream(replacing))
            .add("v3.yaml", stream(dropping))
            .build();

    FortrasSchema v2 = schemas.get("rules-v2");
    assertThat(v2.rule("status").severity()).isEqualTo(Severity.ERROR);
    assertThat(v2.rule("status").allowed(Map.of("kind", "AA")))
        .hasValueSatisfying(v -> assertThat(v).containsExactly(7L));
    assertThat(schemas.get("rules-v3").rules()).isEmpty();
    assertThat(schemas.get("rules").rules()).containsOnlyKeys("status");
  }

  private static ByteArrayInputStream stream(String yaml) {
    return new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
  }
}

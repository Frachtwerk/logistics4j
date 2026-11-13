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

import de.frachtwerk.fortras4j.schema.FieldDefinition;
import de.frachtwerk.fortras4j.schema.FieldElement;
import de.frachtwerk.fortras4j.schema.FortrasSchema;
import de.frachtwerk.fortras4j.schema.RecordDefinition;
import de.frachtwerk.fortras4j.schema.RepeatDefinition;
import de.frachtwerk.fortras4j.schema.ValueRule;
import de.frachtwerk.fortras4j.type.FieldValueException;
import de.frachtwerk.fortras4j.type.Width;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reads one line into a {@link Record}; problems are collected, never thrown. */
final class LineParser {

  private LineParser() {}

  static Record parse(FortrasSchema schema, String line, int lineNumber, List<Problem> problems) {
    return parse(schema, line, lineNumber, problems, Width.chars());
  }

  /** Positions, lengths and {@code maxLineLength} are counted by {@code width}. */
  static Record parse(
      FortrasSchema schema, String line, int lineNumber, List<Problem> problems, Width width) {
    LineBuffer columns = LineBuffer.of(line, width);
    if (columns.length() > schema.maxLineLength()) {
      problems.add(
          new Problem(
              Severity.ERROR,
              ProblemCode.LINE_TOO_LONG,
              lineNumber,
              null,
              null,
              columns.length()
                  + " "
                  + width.unit()
                  + ", at most "
                  + schema.maxLineLength()
                  + " allowed"));
    }
    RecordDefinition definition = schema.recordFor(line);
    if (definition == null) {
      if (!schema.isPassthrough(line)) {
        problems.add(
            new Problem(
                Severity.ERROR,
                ProblemCode.UNKNOWN_RECORD,
                lineNumber,
                line.substring(0, Math.min(3, line.length())),
                null,
                "no record type of layout '" + schema.id() + "' matches this line"));
      }
      return Record.unknown(line, lineNumber);
    }

    int problemsBefore = problems.size();
    Map<String, Object> values = new LinkedHashMap<>();
    for (FieldElement element : definition.elements()) {
      switch (element) {
        case FieldDefinition field ->
            values.put(
                field.name(),
                parseField(
                    field, field.start(), columns, lineNumber, definition, field.name(), problems));
        case RepeatDefinition repeat ->
            values.put(
                repeat.name(), parseBlocks(repeat, columns, lineNumber, definition, problems));
      }
    }
    checkRules(schema, definition, values, lineNumber, problems, problemsBefore);
    return Record.parsed(definition, line, lineNumber, values);
  }

  /** Skips a rule if its field or a condition field is empty or has an error on this line. */
  private static void checkRules(
      FortrasSchema schema,
      RecordDefinition definition,
      Map<String, Object> values,
      int lineNumber,
      List<Problem> problems,
      int problemsBefore) {
    List<Problem> lineProblems = problems.subList(problemsBefore, problems.size());
    List<Problem> found = new ArrayList<>();
    for (ValueRule rule : schema.rules(definition.id())) {
      Set<String> fields = new HashSet<>(rule.conditionFields());
      fields.add(rule.field());
      boolean hasError =
          lineProblems.stream().anyMatch(p -> p.isError() && fields.contains(p.field()));
      if (!hasError) {
        rule.violation(values.get(rule.field()), values)
            .ifPresent(message ->
                found.add(
                    new Problem(
                        rule.severity(),
                        ProblemCode.UNEXPECTED_VALUE,
                        lineNumber,
                        definition.id(),
                        rule.field(),
                        message)));
      }
    }
    problems.addAll(found);
  }

  /** Reads all blocks of a repeat; empty blocks are dropped unless {@code skipEmpty} is off. */
  private static List<Map<String, Object>> parseBlocks(
      RepeatDefinition repeat,
      LineBuffer columns,
      int lineNumber,
      RecordDefinition definition,
      List<Problem> problems) {
    List<Map<String, Object>> blocks = new ArrayList<>();
    for (int i = 0; i < repeat.count(); i++) {
      Map<String, Object> block = new LinkedHashMap<>();
      for (FieldDefinition field : repeat.fields()) {
        String label = repeat.name() + "[" + i + "]." + field.name();
        block.put(
            field.name(),
            parseField(
                field,
                repeat.absoluteStart(i, field),
                columns,
                lineNumber,
                definition,
                label,
                problems));
      }
      boolean present =
          repeat.presentIf() != null
              ? block.get(repeat.presentIf()) != null
              : block.values().stream().anyMatch(v -> v != null);
      if (!repeat.skipEmpty() || present) {
        blocks.add(Collections.unmodifiableMap(block));
      }
    }
    return List.copyOf(blocks);
  }

  private static Object parseField(
      FieldDefinition field,
      int start,
      LineBuffer columns,
      int lineNumber,
      RecordDefinition definition,
      String label,
      List<Problem> problems) {
    try {
      Object value = field.codec().parse(columns.slice(start, start + field.length() - 1));
      if (value == null && field.required()) {
        problems.add(
            new Problem(
                Severity.ERROR,
                ProblemCode.MISSING_VALUE,
                lineNumber,
                definition.id(),
                label,
                "required field is empty at " + start + "-" + (start + field.length() - 1)));
      }
      return value;
    } catch (FieldValueException e) {
      problems.add(
          new Problem(
              e.severity(),
              e.code(),
              lineNumber,
              definition.id(),
              label,
              e.getMessage() + " at " + start + "-" + (start + field.length() - 1)));
      return e.retainedValue();
    }
  }
}

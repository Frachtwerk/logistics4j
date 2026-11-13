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
import de.frachtwerk.fortras4j.type.FieldValueException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads one line into a {@link Record}; problems are collected, never thrown. */
final class LineParser {

  private LineParser() {}

  static Record parse(FortrasSchema schema, String line, int lineNumber, List<Problem> problems) {
    if (line.length() > schema.maxLineLength()) {
      problems.add(
          new Problem(
              Severity.ERROR,
              ProblemCode.LINE_TOO_LONG,
              lineNumber,
              null,
              null,
              line.length() + " characters, at most " + schema.maxLineLength() + " allowed"));
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

    Map<String, Object> values = new LinkedHashMap<>();
    for (FieldElement element : definition.elements()) {
      switch (element) {
        case FieldDefinition field ->
            values.put(
                field.name(),
                parseField(
                    field, field.start(), line, lineNumber, definition, field.name(), problems));
        case RepeatDefinition repeat -> {
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
                      line,
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
          values.put(repeat.name(), List.copyOf(blocks));
        }
      }
    }
    return Record.parsed(definition, line, lineNumber, values);
  }

  private static Object parseField(
      FieldDefinition field,
      int start,
      String line,
      int lineNumber,
      RecordDefinition definition,
      String label,
      List<Problem> problems) {
    String slice = slice(line, start, start + field.length() - 1);
    try {
      Object value = field.codec().parse(slice);
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

  /** Lines may end early: FORTRAS allows omitting trailing blank fields. */
  static String slice(String line, int start, int end) {
    if (start > line.length()) {
      return "";
    }
    return line.substring(start - 1, Math.min(end, line.length()));
  }
}

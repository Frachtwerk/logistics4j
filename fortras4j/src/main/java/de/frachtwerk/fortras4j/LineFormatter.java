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
import java.util.List;
import java.util.Map;

/**
 * Turns a {@link Record} back into a line. Unchanged records keep their original line; changed
 * fields are overlaid onto it, so unmapped areas survive a round trip byte for byte.
 */
final class LineFormatter {

  /** The line being written and where its problems go. */
  private record Target(
      StringBuilder line, RecordDefinition definition, int lineNumber, List<Problem> problems) {}

  private LineFormatter() {}

  static String format(FortrasSchema schema, Record rec, int lineNumber, List<Problem> problems) {
    RecordDefinition definition = rec.definition();
    String raw = rec.raw();
    if (definition == null || (raw != null && !rec.isModified())) {
      return raw;
    }

    StringBuilder line = new StringBuilder(raw != null ? raw : "");
    if (raw == null) {
      ensureLength(line, definition.length());
      line.replace(0, definition.prefix().length(), definition.prefix());
    }
    Target target = new Target(line, definition, lineNumber, problems);
    for (FieldElement element : definition.elements()) {
      if (raw != null && !rec.isModified(element.name())) {
        continue;
      }
      Object value = rec.values().get(element.name());
      switch (element) {
        case FieldDefinition field -> put(target, field, field.start(), value, field.name());
        case RepeatDefinition repeat -> {
          @SuppressWarnings("unchecked")
          List<Map<String, Object>> blocks = (List<Map<String, Object>>) value;
          putBlocks(target, repeat, blocks);
        }
      }
    }
    String result = line.toString();
    return raw == null && schema.trimTrailingSpaces() ? result.stripTrailing() : result;
  }

  /** Writes all blocks of a repeat; positions without a block are blanked. */
  private static void putBlocks(
      Target target, RepeatDefinition repeat, List<Map<String, Object>> blocks) {
    StringBuilder line = target.line();
    for (int i = 0; i < repeat.count(); i++) {
      Map<String, Object> block = i < blocks.size() ? blocks.get(i) : null;
      for (FieldDefinition field : repeat.fields()) {
        int start = repeat.absoluteStart(i, field);
        if (block == null) {
          ensureLength(line, start + field.length() - 1);
          line.replace(start - 1, start - 1 + field.length(), " ".repeat(field.length()));
        } else {
          String label = repeat.name() + "[" + i + "]." + field.name();
          put(target, field, start, block.get(field.name()), label);
        }
      }
    }
  }

  private static void put(
      Target target, FieldDefinition field, int start, Object value, String label) {
    RecordDefinition definition = target.definition();
    if (value == null && field.required()) {
      target
          .problems()
          .add(
              new Problem(
                  Severity.ERROR,
                  ProblemCode.MISSING_VALUE,
                  target.lineNumber(),
                  definition.id(),
                  label,
                  "required field has no value"));
      return;
    }
    String text;
    try {
      text = field.codec().format(value, field.length());
    } catch (FieldValueException e) {
      target
          .problems()
          .add(
              new Problem(
                  e.severity(),
                  e.code(),
                  target.lineNumber(),
                  definition.id(),
                  label,
                  e.getMessage()));
      return;
    }
    if (text.length() != field.length()) {
      throw new IllegalStateException(
          "codec of "
              + definition.id()
              + "."
              + label
              + " returned "
              + text.length()
              + " instead of "
              + field.length()
              + " characters");
    }
    ensureLength(target.line(), start + field.length() - 1);
    target.line().replace(start - 1, start - 1 + field.length(), text);
  }

  private static void ensureLength(StringBuilder line, int length) {
    while (line.length() < length) {
      line.append(' ');
    }
  }
}

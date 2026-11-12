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

  private LineFormatter() {}

  static String format(
      FortrasSchema schema, Record record, int lineNumber, List<Problem> problems) {
    RecordDefinition definition = record.definition();
    String raw = record.raw();
    if (definition == null || (raw != null && !record.isModified())) {
      return raw;
    }

    StringBuilder line = new StringBuilder(raw != null ? raw : "");
    if (raw == null) {
      ensureLength(line, definition.length());
      line.replace(0, definition.prefix().length(), definition.prefix());
    }
    for (FieldElement element : definition.elements()) {
      if (raw != null && !record.isModified(element.name())) {
        continue;
      }
      Object value = record.values().get(element.name());
      switch (element) {
        case FieldDefinition field ->
            put(line, field, field.start(), value, definition, field.name(), lineNumber, problems);
        case RepeatDefinition repeat -> {
          @SuppressWarnings("unchecked")
          List<Map<String, Object>> blocks = (List<Map<String, Object>>) value;
          for (int i = 0; i < repeat.count(); i++) {
            Map<String, Object> block = i < blocks.size() ? blocks.get(i) : null;
            for (FieldDefinition field : repeat.fields()) {
              int start = repeat.absoluteStart(i, field);
              if (block == null) {
                ensureLength(line, start + field.length() - 1);
                line.replace(start - 1, start - 1 + field.length(), " ".repeat(field.length()));
              } else {
                String label = repeat.name() + "[" + i + "]." + field.name();
                put(
                    line,
                    field,
                    start,
                    block.get(field.name()),
                    definition,
                    label,
                    lineNumber,
                    problems);
              }
            }
          }
        }
      }
    }
    String result = line.toString();
    return raw == null && schema.trimTrailingSpaces() ? result.stripTrailing() : result;
  }

  private static void put(
      StringBuilder line,
      FieldDefinition field,
      int start,
      Object value,
      RecordDefinition definition,
      String label,
      int lineNumber,
      List<Problem> problems) {
    if (value == null && field.required()) {
      problems.add(
          new Problem(
              Severity.ERROR,
              ProblemCode.MISSING_VALUE,
              lineNumber,
              definition.id(),
              label,
              "required field has no value"));
      return;
    }
    String text;
    try {
      text = field.codec().format(value, field.length());
    } catch (FieldValueException e) {
      problems.add(
          new Problem(e.severity(), e.code(), lineNumber, definition.id(), label, e.getMessage()));
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
    ensureLength(line, start + field.length() - 1);
    line.replace(start - 1, start - 1 + field.length(), text);
  }

  private static void ensureLength(StringBuilder line, int length) {
    while (line.length() < length) {
      line.append(' ');
    }
  }
}

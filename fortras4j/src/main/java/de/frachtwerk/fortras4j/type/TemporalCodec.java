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

package de.frachtwerk.fortras4j.type;

import de.frachtwerk.fortras4j.ProblemCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.List;

/**
 * Types {@code date} (default pattern {@code ddMMyyyy}, i.e. TTMMJJJJ), {@code time} (default
 * {@code HHmm}) and {@code datetime} (default {@code ddMMyyyyHHmm}). Empty fields read as {@code
 * null}; all-zero fields too for {@code date} and {@code time}, for {@code datetime} only with
 * {@code null: zeros}.
 */
final class TemporalCodec implements FieldCodec {

  enum Kind {
    DATE("date", "ddMMyyyy"),
    TIME("time", "HHmm"),
    DATETIME("datetime", "ddMMyyyyHHmm");

    private final String typeName;
    private final String defaultPattern;

    Kind(String typeName, String defaultPattern) {
      this.typeName = typeName;
      this.defaultPattern = defaultPattern;
    }
  }

  private final Kind kind;
  private final DateTimeFormatter formatter;
  private final Layout layout;

  private TemporalCodec(Kind kind, DateTimeFormatter formatter, Layout layout) {
    this.kind = kind;
    this.formatter = formatter;
    this.layout = layout;
  }

  static FieldType type(Kind kind) {
    return new FieldType() {
      @Override
      public String name() {
        return kind.typeName;
      }

      @Override
      public FieldCodec create(FieldOptions options, FieldContext context) {
        String pattern = options.string("pattern", kind.defaultPattern);
        DateTimeFormatter dateTimeFormatter;
        try {
          dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
        } catch (IllegalArgumentException e) {
          throw new IllegalArgumentException("invalid pattern '" + pattern + "'");
        }
        Layout temporalLayout =
            Layout.from(
                options, Layout.Align.RIGHT, ' ', Layout.NullAs.BLANKS, Layout.Overflow.ERROR);
        return new TemporalCodec(kind, dateTimeFormatter, temporalLayout);
      }
    };
  }

  @Override
  public Object parse(String slice) {
    String text = slice.strip();
    if (isEmpty(text)) {
      return null;
    }
    try {
      return switch (kind) {
        case DATE -> LocalDate.parse(text, formatter);
        case TIME -> LocalTime.parse(text, formatter);
        case DATETIME -> LocalDateTime.parse(text, formatter);
      };
    } catch (DateTimeParseException e) {
      throw new FieldValueException(
          ProblemCode.INVALID_VALUE, "'" + text + "' is not a valid " + kind.typeName);
    }
  }

  private boolean isEmpty(String text) {
    if (kind == Kind.DATETIME) {
      return layout.isEmpty(text);
    }
    return text.isEmpty() || text.chars().allMatch(c -> c == '0');
  }

  @Override
  public String format(Object value, int length) {
    if (value == null) {
      return layout.empty(length);
    }
    return layout.fit(formatter.format((TemporalAccessor) coerce(value)), length);
  }

  @Override
  public Object coerce(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return switch (value) {
        case LocalDate d when kind == Kind.DATE -> d;
        case LocalTime t when kind == Kind.TIME -> t;
        case LocalDateTime dt when kind == Kind.DATETIME -> dt;
        case String s when s.isBlank() -> null;
        case String s -> parseIso(s.strip());
        case List<?> parts -> fromParts(parts);
        default ->
            throw new IllegalArgumentException(
                "cannot convert " + value.getClass().getSimpleName() + " to a " + kind.typeName);
      };
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("'" + value + "' is not an ISO " + kind.typeName);
    }
  }

  private Object parseIso(String text) {
    return switch (kind) {
      case DATE -> LocalDate.parse(text);
      case TIME -> LocalTime.parse(text);
      case DATETIME -> LocalDateTime.parse(text);
    };
  }

  // Jackson may serialize java.time values as arrays, e.g. [2024, 1, 31]
  private Object fromParts(List<?> parts) {
    int[] p = parts.stream().mapToInt(o -> ((Number) o).intValue()).toArray();
    return switch (kind) {
      case DATE -> LocalDate.of(p[0], p[1], p[2]);
      case TIME -> LocalTime.of(p[0], part(p, 1), part(p, 2));
      case DATETIME ->
          LocalDateTime.of(p[0], p[1], p[2], part(p, 3), part(p, 4), part(p, 5), part(p, 6));
    };
  }

  private static int part(int[] parts, int index) {
    return parts.length > index ? parts[index] : 0;
  }
}

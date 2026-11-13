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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.List;

/**
 * Types {@code date} (default pattern {@code ddMMyyyy}, i.e. TTMMJJJJ) and {@code time} (default
 * {@code HHmm}). Empty and all-zero fields read as {@code null}.
 */
final class TemporalCodec implements FieldCodec {

  private final boolean date;
  private final DateTimeFormatter formatter;
  private final Layout layout;

  private TemporalCodec(boolean date, DateTimeFormatter formatter, Layout layout) {
    this.date = date;
    this.formatter = formatter;
    this.layout = layout;
  }

  static FieldType type(boolean date) {
    return new FieldType() {
      @Override
      public String name() {
        return date ? "date" : "time";
      }

      @Override
      public FieldCodec create(FieldOptions options, FieldContext context) {
        String pattern = options.string("pattern", date ? "ddMMyyyy" : "HHmm");
        DateTimeFormatter dateTimeFormatter;
        try {
          dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
        } catch (IllegalArgumentException e) {
          throw new IllegalArgumentException("invalid pattern '" + pattern + "'");
        }
        Layout temporalLayout =
            Layout.from(
                options, Layout.Align.RIGHT, ' ', Layout.NullAs.BLANKS, Layout.Overflow.ERROR);
        return new TemporalCodec(date, dateTimeFormatter, temporalLayout);
      }
    };
  }

  @Override
  public Object parse(String slice) {
    String text = slice.strip();
    if (text.isEmpty() || text.chars().allMatch(c -> c == '0')) {
      return null;
    }
    try {
      return date ? LocalDate.parse(text, formatter) : LocalTime.parse(text, formatter);
    } catch (DateTimeParseException e) {
      throw new FieldValueException(
          ProblemCode.INVALID_VALUE, "'" + text + "' is not a valid " + (date ? "date" : "time"));
    }
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
        case LocalDate d when date -> d;
        case LocalTime t when !date -> t;
        case String s when s.isBlank() -> null;
        case String s -> date ? LocalDate.parse(s.strip()) : LocalTime.parse(s.strip());
        case List<?> parts -> fromParts(parts);
        default ->
            throw new IllegalArgumentException(
                "cannot convert " + value.getClass().getSimpleName() + " to a " + name());
      };
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("'" + value + "' is not an ISO " + name());
    }
  }

  // Jackson may serialize java.time values as arrays, e.g. [2024, 1, 31]
  private Object fromParts(List<?> parts) {
    int[] p = parts.stream().mapToInt(o -> ((Number) o).intValue()).toArray();
    if (date) {
      return LocalDate.of(p[0], p[1], p[2]);
    }
    return LocalTime.of(p[0], p.length > 1 ? p[1] : 0, p.length > 2 ? p[2] : 0);
  }

  private String name() {
    return date ? "date" : "time";
  }
}

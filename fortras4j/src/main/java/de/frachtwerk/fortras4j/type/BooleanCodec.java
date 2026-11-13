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
import java.util.List;
import java.util.Locale;

/**
 * Type {@code boolean}. Options {@code true} and {@code false} list the accepted codes; the first
 * one is written. Defaults: {@code Y} / {@code N}.
 */
final class BooleanCodec implements FieldCodec {

  private final List<String> trueCodes;
  private final List<String> falseCodes;
  private final Layout layout;

  private BooleanCodec(List<String> trueCodes, List<String> falseCodes, Layout layout) {
    this.trueCodes = trueCodes;
    this.falseCodes = falseCodes;
    this.layout = layout;
  }

  static FieldType type() {
    return new FieldType() {
      @Override
      public String name() {
        return "boolean";
      }

      @Override
      public FieldCodec create(FieldOptions options, FieldContext context) {
        List<String> trueValues = options.strings("true", List.of("Y"));
        List<String> falseValues = options.strings("false", List.of("N"));
        if (trueValues.isEmpty() || falseValues.isEmpty()) {
          throw new IllegalArgumentException("options 'true' and 'false' must not be empty");
        }
        Layout booleanLayout =
            Layout.from(
                options, Layout.Align.LEFT, ' ', Layout.NullAs.BLANKS, Layout.Overflow.ERROR);
        return new BooleanCodec(trueValues, falseValues, booleanLayout);
      }
    };
  }

  @Override
  public Object parse(String slice) {
    String text = slice.strip();
    if (text.isEmpty()) {
      return null;
    }
    if (trueCodes.contains(text)) {
      return Boolean.TRUE;
    }
    if (falseCodes.contains(text)) {
      return Boolean.FALSE;
    }
    throw new FieldValueException(
        ProblemCode.INVALID_VALUE, "'" + text + "' is neither " + trueCodes + " nor " + falseCodes);
  }

  @Override
  public String format(Object value, int length) {
    return format(value, length, Width.chars());
  }

  @Override
  public String format(Object value, int length, Width width) {
    // coerce maps null and blank text to null, which is written as an empty field
    if (!(coerce(value) instanceof Boolean b)) {
      return layout.empty(length);
    }
    return layout.fit(
        b.booleanValue() ? trueCodes.getFirst() : falseCodes.getFirst(), length, width);
  }

  @Override
  public Object coerce(Object value) {
    return switch (value) {
      case null -> null;
      case Boolean b -> b;
      case String s when s.isBlank() -> null;
      case String s when trueCodes.contains(s.strip()) -> Boolean.TRUE;
      case String s when falseCodes.contains(s.strip()) -> Boolean.FALSE;
      case String s when s.strip().toLowerCase(Locale.ROOT).equals("true") -> Boolean.TRUE;
      case String s when s.strip().toLowerCase(Locale.ROOT).equals("false") -> Boolean.FALSE;
      default -> throw new IllegalArgumentException("cannot convert '" + value + "' to a boolean");
    };
  }
}

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

/**
 * Padding, alignment and empty-value handling shared by all built-in types.
 *
 * <ul>
 *   <li>{@code align}: {@code left} or {@code right}
 *   <li>{@code pad}: fill character
 *   <li>{@code null}: how an empty value is written, {@code blanks} or {@code zeros}
 *   <li>{@code overflow}: value too long; {@code error}, {@code truncate} (text) or {@code max}
 *       (numbers, writes all nines)
 * </ul>
 */
public record Layout(Align align, char pad, NullAs nullAs, Overflow overflow) {

  public enum Align {
    LEFT,
    RIGHT
  }

  public enum NullAs {
    BLANKS,
    ZEROS
  }

  public enum Overflow {
    ERROR,
    TRUNCATE,
    MAX
  }

  public static Layout from(
      FieldOptions options, Align align, char pad, NullAs nullAs, Overflow overflow) {
    return new Layout(
        options.enumValue("align", Align.class, align),
        options.character("pad", pad),
        options.enumValue("null", NullAs.class, nullAs),
        options.enumValue("overflow", Overflow.class, overflow));
  }

  public String empty(int length) {
    return String.valueOf(nullAs == NullAs.ZEROS ? '0' : ' ').repeat(length);
  }

  /** True if the slice represents no value: blank, or only zeros for {@code null: zeros}. */
  public boolean isEmpty(String slice) {
    if (slice.isBlank()) {
      return true;
    }
    return nullAs == NullAs.ZEROS && slice.chars().allMatch(c -> c == '0' || c == ' ');
  }

  /** Pads {@code text} to {@code length}; handles overflow except {@link Overflow#MAX}. */
  public String fit(String text, int length) {
    if (text.length() > length) {
      if (overflow == Overflow.TRUNCATE) {
        return align == Align.LEFT
            ? text.substring(0, length)
            : text.substring(text.length() - length);
      }
      throw new FieldValueException(
          ProblemCode.VALUE_TOO_LONG,
          "value '" + text + "' does not fit into " + length + " characters");
    }
    String padding = String.valueOf(pad).repeat(length - text.length());
    return align == Align.LEFT ? text + padding : padding + text;
  }
}

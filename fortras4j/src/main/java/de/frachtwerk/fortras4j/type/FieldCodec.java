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

/** Converts between the characters of one fixed-width field and a Java value. */
public interface FieldCodec {

  /**
   * @param slice the field's characters; shorter than the field (or empty) if the line ends early
   * @return the value, or {@code null} if the field is empty
   * @throws FieldValueException if the content is invalid; it may carry a value to retain
   */
  Object parse(String slice);

  /**
   * @param value a value as returned by {@link #coerce(Object)}, or {@code null}
   * @return exactly {@code length} characters
   * @throws FieldValueException if the value cannot be represented
   */
  String format(Object value, int length);

  /**
   * Normalizes a value set by the caller or produced by the binder (e.g. an ISO date string) to the
   * type returned by {@link #parse(String)}.
   *
   * @throws IllegalArgumentException if the value cannot be converted
   */
  Object coerce(Object value);
}

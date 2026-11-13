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

import java.nio.charset.Charset;

/**
 * How positions and lengths of fixed-width fields are counted: in characters (the default) or in
 * the bytes a charset encodes them to. Byte widths matter for multibyte charsets such as UTF-8,
 * where an umlaut takes two bytes; for single-byte charsets both count the same.
 */
public sealed interface Width permits CharacterWidth, ByteWidth {

  /** Counts characters ({@code char}s), as all versions before 0.2.0 did. */
  static Width chars() {
    return CharacterWidth.INSTANCE;
  }

  /**
   * Counts the bytes of {@code charset}.
   *
   * @throws IllegalArgumentException if the charset does not encode printable ASCII as one byte per
   *     character (e.g. UTF-16), because padding, digits and prefixes rely on that
   */
  static Width bytes(Charset charset) {
    return new ByteWidth(charset);
  }

  /** Length of {@code text} in this width. */
  int of(String text);

  /** Longest start of {@code text} that is at most {@code max} long. */
  String head(String text, int max);

  /** Longest end of {@code text} that is at most {@code max} long. */
  String tail(String text, int max);

  /** Padding of exactly {@code length} made of {@code pad}, filled up with blanks if necessary. */
  String padding(char pad, int length);

  /** Unit for messages, e.g. {@code characters}. */
  String unit();
}

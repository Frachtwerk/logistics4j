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
import java.util.Objects;

/**
 * Bytes of a charset that encodes printable ASCII as single bytes. {@link #head} and {@link #tail}
 * never split a character, so they may return less than the maximum.
 */
public record ByteWidth(Charset charset) implements Width {

  private static final String PRINTABLE_ASCII = printableAscii();

  public ByteWidth {
    Objects.requireNonNull(charset, "charset");
    if (!charset.canEncode()
        || PRINTABLE_ASCII.getBytes(charset).length != PRINTABLE_ASCII.length()) {
      throw new IllegalArgumentException(
          "charset " + charset.name() + " does not encode ASCII as single bytes");
    }
  }

  private static String printableAscii() {
    StringBuilder ascii = new StringBuilder();
    for (char c = ' '; c <= '~'; c++) {
      ascii.append(c);
    }
    return ascii.toString();
  }

  @Override
  public int of(String text) {
    return text.getBytes(charset).length;
  }

  @Override
  public String head(String text, int max) {
    int used = 0;
    int end = 0;
    while (end < text.length()) {
      int next = end + Character.charCount(text.codePointAt(end));
      used += of(text.substring(end, next));
      if (used > max) {
        break;
      }
      end = next;
    }
    return text.substring(0, end);
  }

  @Override
  public String tail(String text, int max) {
    int used = 0;
    int start = text.length();
    while (start > 0) {
      int previous = start - Character.charCount(text.codePointBefore(start));
      used += of(text.substring(previous, start));
      if (used > max) {
        break;
      }
      start = previous;
    }
    return text.substring(start);
  }

  @Override
  public String padding(char pad, int length) {
    int padWidth = of(String.valueOf(pad));
    int count = length / padWidth;
    return String.valueOf(pad).repeat(count) + " ".repeat(length - count * padWidth);
  }

  @Override
  public String unit() {
    return "bytes";
  }
}

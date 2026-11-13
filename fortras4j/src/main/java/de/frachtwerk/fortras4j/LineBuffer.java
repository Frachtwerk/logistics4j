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

import de.frachtwerk.fortras4j.type.FieldValueException;
import de.frachtwerk.fortras4j.type.Width;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;

/** A line addressed by 1-based positions, counted in characters or in bytes of a charset. */
abstract sealed class LineBuffer permits LineBuffer.Chars, LineBuffer.Bytes {

  static LineBuffer of(String line, Width width) {
    return switch (width) {
      case Width.Chars c -> new Chars(line);
      case Width.Bytes b -> new Bytes(line, b.charset());
    };
  }

  /** Length in positions. */
  abstract int length();

  /**
   * Content from {@code start} to {@code end} (inclusive); shorter or empty if the line ends early.
   *
   * @throws FieldValueException if the range splits a multibyte character
   */
  abstract String slice(int start, int end);

  /** Appends blanks up to {@code length} positions. */
  abstract void ensureLength(int length);

  /** Overwrites the positions from {@code start} with {@code text}, which must fit exactly. */
  abstract void replace(int start, String text);

  static final class Chars extends LineBuffer {

    private final StringBuilder line;

    Chars(String line) {
      this.line = new StringBuilder(line);
    }

    @Override
    int length() {
      return line.length();
    }

    @Override
    String slice(int start, int end) {
      if (start > line.length()) {
        return "";
      }
      return line.substring(start - 1, Math.min(end, line.length()));
    }

    @Override
    void ensureLength(int length) {
      while (line.length() < length) {
        line.append(' ');
      }
    }

    @Override
    void replace(int start, String text) {
      ensureLength(start - 1 + text.length());
      line.replace(start - 1, start - 1 + text.length(), text);
    }

    @Override
    public String toString() {
      return line.toString();
    }
  }

  static final class Bytes extends LineBuffer {

    private final Charset charset;
    private byte[] bytes;
    private int size;

    Bytes(String line, Charset charset) {
      this.charset = charset;
      this.bytes = line.getBytes(charset);
      this.size = bytes.length;
    }

    @Override
    int length() {
      return size;
    }

    @Override
    String slice(int start, int end) {
      if (start > size) {
        return "";
      }
      ByteBuffer range = ByteBuffer.wrap(bytes, start - 1, Math.min(end, size) - start + 1);
      try {
        return charset
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(range)
            .toString();
      } catch (CharacterCodingException e) {
        throw new FieldValueException(
            ProblemCode.INVALID_VALUE, "the field boundaries split a multibyte character");
      }
    }

    @Override
    void ensureLength(int length) {
      if (length > bytes.length) {
        bytes = Arrays.copyOf(bytes, Math.max(length, bytes.length * 2));
      }
      if (length > size) {
        Arrays.fill(bytes, size, length, (byte) ' ');
        size = length;
      }
    }

    @Override
    void replace(int start, String text) {
      byte[] encoded = text.getBytes(charset);
      ensureLength(start - 1 + encoded.length);
      System.arraycopy(encoded, 0, bytes, start - 1, encoded.length);
    }

    /** Decodes the bytes; a multibyte character split by an earlier overlay becomes U+FFFD. */
    @Override
    public String toString() {
      return new String(bytes, 0, size, charset);
    }
  }
}

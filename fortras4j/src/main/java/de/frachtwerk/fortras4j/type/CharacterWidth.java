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

/** Characters; {@link #head} and {@link #tail} cut exactly at the length, as before 0.2.0. */
public final class CharacterWidth implements Width {

  static final CharacterWidth INSTANCE = new CharacterWidth();

  private CharacterWidth() {}

  @Override
  public int of(String text) {
    return text.length();
  }

  @Override
  public String head(String text, int max) {
    return text.length() <= max ? text : text.substring(0, max);
  }

  @Override
  public String tail(String text, int max) {
    return text.length() <= max ? text : text.substring(text.length() - max);
  }

  @Override
  public String padding(char pad, int length) {
    return String.valueOf(pad).repeat(length);
  }

  @Override
  public String unit() {
    return "characters";
  }

  @Override
  public String toString() {
    return "chars";
  }
}

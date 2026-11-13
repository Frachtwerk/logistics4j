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

package de.frachtwerk.fortras4j.schema;

/**
 * How often a structure element may occur: {@code "1"}, {@code "0..1"}, {@code "1..*"}, {@code "*"}
 * or {@code "0..9"}.
 */
public record Occurs(int min, int max) {

  public static final int UNBOUNDED = Integer.MAX_VALUE;
  public static final Occurs ONCE = new Occurs(1, 1);

  public Occurs {
    if (min < 0 || max < 1 || max < min) {
      throw new IllegalArgumentException("invalid occurrence " + min + ".." + max);
    }
  }

  public static Occurs parse(String value) {
    String text = value.trim();
    if (text.equals("*")) {
      return new Occurs(0, UNBOUNDED);
    }
    int separator = text.indexOf("..");
    if (separator < 0) {
      int exact = Integer.parseInt(text);
      return new Occurs(exact, exact);
    }
    int min = Integer.parseInt(text.substring(0, separator).trim());
    String upper = text.substring(separator + 2).trim();
    int max = upper.equals("*") ? UNBOUNDED : Integer.parseInt(upper);
    return new Occurs(min, max);
  }

  public boolean repeatable() {
    return max > 1;
  }

  @Override
  public String toString() {
    if (min == max) {
      return String.valueOf(min);
    }
    return min + ".." + (max == UNBOUNDED ? "*" : String.valueOf(max));
  }
}

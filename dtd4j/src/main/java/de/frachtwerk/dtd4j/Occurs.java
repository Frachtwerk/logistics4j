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

package de.frachtwerk.dtd4j;

/** How often a particle of a content model may occur: none, {@code ?}, {@code *} or {@code +}. */
public enum Occurs {
  ONE(""),
  OPTIONAL("?"),
  ZERO_OR_MORE("*"),
  ONE_OR_MORE("+");

  private final String symbol;

  Occurs(String symbol) {
    this.symbol = symbol;
  }

  /** The suffix in the DTD, empty for {@link #ONE}. */
  public String symbol() {
    return symbol;
  }

  public boolean optional() {
    return this == OPTIONAL || this == ZERO_OR_MORE;
  }

  public boolean repeatable() {
    return this == ZERO_OR_MORE || this == ONE_OR_MORE;
  }
}

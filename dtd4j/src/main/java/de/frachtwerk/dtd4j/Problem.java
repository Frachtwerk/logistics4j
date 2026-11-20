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

/**
 * A deviation from the DTD or the layout, reported instead of thrown.
 *
 * @param path the element, e.g. {@code FAR_DATA/FAHRT[1]/SDG[2]/SDG_HEAD}
 */
public record Problem(Severity severity, ProblemCode code, String path, String message) {

  public boolean isError() {
    return severity == Severity.ERROR;
  }

  @Override
  public String toString() {
    return severity + " " + code + " at " + path + ": " + message;
  }
}

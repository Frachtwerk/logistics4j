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

/**
 * A finding while reading or writing.
 *
 * @param lineNumber 1-based line, or 0 if not related to a line
 * @param recordId record type, if known
 * @param field field or structure element concerned, if any
 */
public record Problem(
    Severity severity,
    ProblemCode code,
    int lineNumber,
    String recordId,
    String field,
    String message) {

  public boolean isError() {
    return severity == Severity.ERROR;
  }

  @Override
  public String toString() {
    StringBuilder text = new StringBuilder().append(severity).append(' ').append(code);
    if (lineNumber > 0) {
      text.append(" line ").append(lineNumber);
    }
    if (recordId != null) {
      text.append(' ').append(recordId);
    }
    if (field != null) {
      text.append('.').append(field);
    }
    return text.append(": ").append(message).toString();
  }
}

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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.List;

/** Lines produced by {@link FortrasWriter} together with the problems found while writing. */
public record WriteResult(List<String> lines, List<Problem> problems) {

  public WriteResult {
    lines = List.copyOf(lines);
    problems = List.copyOf(problems);
  }

  public boolean isValid() {
    return problems.stream().noneMatch(Problem::isError);
  }

  /** Writes the lines, each terminated by {@code lineSeparator}; FORTRAS prescribes CRLF. */
  public void writeTo(Writer out, String lineSeparator) {
    try {
      for (String line : lines) {
        out.write(line);
        out.write(lineSeparator);
      }
      out.flush();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public String asText(String lineSeparator) {
    return String.join(lineSeparator, lines) + lineSeparator;
  }
}

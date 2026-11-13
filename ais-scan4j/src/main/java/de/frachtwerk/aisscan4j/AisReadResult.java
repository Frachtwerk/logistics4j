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

package de.frachtwerk.aisscan4j;

import de.frachtwerk.fortras4j.Problem;
import java.util.List;
import java.util.Objects;

/**
 * A scan and the problems found while reading it. Fields with errors are {@code null} in the scan;
 * values with warnings (e.g. an unknown hub) are kept.
 *
 * @param line the line as read, for messages that quote the input
 */
public record AisReadResult(AisScan scan, List<Problem> problems, String line) {

  public AisReadResult {
    problems = List.copyOf(problems);
  }

  public List<Problem> errors() {
    return problems.stream().filter(Problem::isError).toList();
  }

  public boolean isValid() {
    return errors().isEmpty();
  }

  /** Problems of one field, by the property name of {@link AisScan}. */
  public List<Problem> problems(String property) {
    String field = AisScans.FIELDS.get(property);
    return problems.stream().filter(p -> Objects.equals(p.field(), field)).toList();
  }
}

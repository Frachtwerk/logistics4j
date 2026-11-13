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

import java.util.List;

/** A document could not be read or written; carries all problems found. */
public class FortrasException extends RuntimeException {

  private final List<Problem> problems;

  public FortrasException(String message, List<Problem> problems) {
    super(message + describe(problems));
    this.problems = List.copyOf(problems);
  }

  public List<Problem> problems() {
    return problems;
  }

  private static String describe(List<Problem> problems) {
    if (problems.isEmpty()) {
      return "";
    }
    StringBuilder text = new StringBuilder();
    problems.stream().limit(20).forEach(p -> text.append("\n  - ").append(p));
    if (problems.size() > 20) {
      text.append("\n  ... ").append(problems.size() - 20).append(" more");
    }
    return text.toString();
  }
}

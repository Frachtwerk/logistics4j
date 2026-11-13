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

import de.frachtwerk.fortras4j.schema.FortrasSchema;
import java.util.List;

/** A FORTRAS file read into records grouped by the layout's structure. */
public final class FortrasDocument {

  private final FortrasSchema schema;
  private final Group root;
  private final List<Problem> problems;

  public FortrasDocument(FortrasSchema schema, Group root, List<Problem> problems) {
    this.schema = schema;
    this.root = root;
    this.problems = List.copyOf(problems);
  }

  public FortrasSchema schema() {
    return schema;
  }

  public Group root() {
    return root;
  }

  public List<Record> records() {
    return root.flatten();
  }

  public List<Record> records(String recordId) {
    return records().stream().filter(r -> recordId.equals(r.id())).toList();
  }

  public List<Problem> problems() {
    return problems;
  }

  public List<Problem> errors() {
    return problems.stream().filter(Problem::isError).toList();
  }

  /** True if no {@link Severity#ERROR} was found; warnings are allowed. */
  public boolean isValid() {
    return problems.stream().noneMatch(Problem::isError);
  }
}

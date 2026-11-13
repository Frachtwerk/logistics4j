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
import java.util.ArrayList;
import java.util.List;

/**
 * Writes documents or record lists as FORTRAS lines. By default the records are checked against the
 * layout's structure and an invalid result throws a {@link FortrasException}.
 */
public final class FortrasWriter {

  private final boolean validate;
  private final boolean failOnError;

  private FortrasWriter(boolean validate, boolean failOnError) {
    this.validate = validate;
    this.failOnError = failOnError;
  }

  public static FortrasWriter create() {
    return new FortrasWriter(true, true);
  }

  /** Check the structure (groups, occurrences, constraints) before writing. */
  public FortrasWriter validate(boolean validate) {
    return new FortrasWriter(validate, failOnError);
  }

  public FortrasWriter failOnError(boolean failOnError) {
    return new FortrasWriter(validate, failOnError);
  }

  public WriteResult write(FortrasDocument document) {
    return write(document.schema(), document.records());
  }

  /** Writes records in the given order; they need not come from a parsed document. */
  public WriteResult write(FortrasSchema schema, List<Record> records) {
    List<Problem> problems = new ArrayList<>();
    if (validate) {
      List<Record> copy = new ArrayList<>(records);
      new StructureMatcher(schema, copy, problems).match();
    }
    List<String> lines = new ArrayList<>(records.size());
    for (int i = 0; i < records.size(); i++) {
      String line = LineFormatter.format(schema, records.get(i), i + 1, problems);
      if (line != null) {
        lines.add(line);
      }
    }
    problems.sort(java.util.Comparator.comparingInt(Problem::lineNumber));
    WriteResult result = new WriteResult(lines, problems);
    if (failOnError && !result.isValid()) {
      throw new FortrasException("cannot write a valid " + schema.id() + " file", problems);
    }
    return result;
  }
}

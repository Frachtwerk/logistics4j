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
import de.frachtwerk.fortras4j.type.Width;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Writes documents or record lists as FORTRAS lines. By default the records are checked against the
 * layout's structure and an invalid result throws a {@link FortrasException}.
 */
public final class FortrasWriter {

  private final boolean validate;
  private final boolean failOnError;
  private final Width width;

  private FortrasWriter(boolean validate, boolean failOnError, Width width) {
    this.validate = validate;
    this.failOnError = failOnError;
    this.width = Objects.requireNonNull(width);
  }

  public static FortrasWriter create() {
    return new FortrasWriter(true, true, Width.chars());
  }

  /** Check the structure (groups, occurrences, constraints) before writing. */
  public FortrasWriter validate(boolean validate) {
    return new FortrasWriter(validate, failOnError, width);
  }

  public FortrasWriter failOnError(boolean failOnError) {
    return new FortrasWriter(validate, failOnError, width);
  }

  /**
   * Count field positions and lengths in bytes of {@code charset} instead of characters: values are
   * padded and truncated by their encoded length, and a multibyte character is never split. The
   * result is still a list of lines; encoding them stays with the caller.
   *
   * @throws IllegalArgumentException if the charset does not encode ASCII as single bytes
   */
  public FortrasWriter widthsIn(Charset charset) {
    return new FortrasWriter(validate, failOnError, Width.bytes(charset));
  }

  /** Count field positions and lengths in characters; the default. */
  public FortrasWriter widthsInCharacters() {
    return new FortrasWriter(validate, failOnError, Width.chars());
  }

  /** How field positions and lengths are counted. */
  public Width width() {
    return width;
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
      String line = LineFormatter.format(schema, records.get(i), i + 1, problems, width);
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

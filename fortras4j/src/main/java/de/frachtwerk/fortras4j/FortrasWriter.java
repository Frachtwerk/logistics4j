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

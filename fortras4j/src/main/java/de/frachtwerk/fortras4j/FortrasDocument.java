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

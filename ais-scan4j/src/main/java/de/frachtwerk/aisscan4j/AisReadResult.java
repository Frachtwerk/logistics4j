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

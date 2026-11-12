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

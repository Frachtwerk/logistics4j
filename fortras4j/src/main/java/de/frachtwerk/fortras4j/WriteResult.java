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

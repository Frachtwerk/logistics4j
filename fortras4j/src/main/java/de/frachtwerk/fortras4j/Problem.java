package de.frachtwerk.fortras4j;

/**
 * A finding while reading or writing.
 *
 * @param lineNumber 1-based line, or 0 if not related to a line
 * @param recordId record type, if known
 * @param field field or structure element concerned, if any
 */
public record Problem(
    Severity severity,
    ProblemCode code,
    int lineNumber,
    String recordId,
    String field,
    String message) {

  public boolean isError() {
    return severity == Severity.ERROR;
  }

  @Override
  public String toString() {
    StringBuilder text = new StringBuilder().append(severity).append(' ').append(code);
    if (lineNumber > 0) {
      text.append(" line ").append(lineNumber);
    }
    if (recordId != null) {
      text.append(' ').append(recordId);
    }
    if (field != null) {
      text.append('.').append(field);
    }
    return text.append(": ").append(message).toString();
  }
}

package de.frachtwerk.fortras4j.type;

import de.frachtwerk.fortras4j.ProblemCode;
import de.frachtwerk.fortras4j.Severity;

/** A field value could not be read or written. */
public class FieldValueException extends RuntimeException {

  private final ProblemCode code;
  private final Severity severity;
  private final transient Object retainedValue;

  public FieldValueException(ProblemCode code, String message) {
    this(code, Severity.ERROR, message, null);
  }

  /**
   * @param retainedValue value to keep in the record despite the problem (e.g. an unknown code)
   */
  public FieldValueException(
      ProblemCode code, Severity severity, String message, Object retainedValue) {
    super(message);
    this.code = code;
    this.severity = severity;
    this.retainedValue = retainedValue;
  }

  public ProblemCode code() {
    return code;
  }

  public Severity severity() {
    return severity;
  }

  public Object retainedValue() {
    return retainedValue;
  }
}

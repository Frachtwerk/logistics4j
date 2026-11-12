package de.frachtwerk.fortras4j.type;

/** Converts between the characters of one fixed-width field and a Java value. */
public interface FieldCodec {

  /**
   * @param slice the field's characters; shorter than the field (or empty) if the line ends early
   * @return the value, or {@code null} if the field is empty
   * @throws FieldValueException if the content is invalid; it may carry a value to retain
   */
  Object parse(String slice);

  /**
   * @param value a value as returned by {@link #coerce(Object)}, or {@code null}
   * @return exactly {@code length} characters
   * @throws FieldValueException if the value cannot be represented
   */
  String format(Object value, int length);

  /**
   * Normalizes a value set by the caller or produced by the binder (e.g. an ISO date string) to the
   * type returned by {@link #parse(String)}.
   *
   * @throws IllegalArgumentException if the value cannot be converted
   */
  Object coerce(Object value);
}

package de.frachtwerk.fortras4j;

/** Stable identifiers for problems; meant for translation and handling in applications. */
public enum ProblemCode {
  /** No layout matches the file. */
  NO_SCHEMA,
  LINE_TOO_LONG,
  /** The line starts with no known record prefix. */
  UNKNOWN_RECORD,
  INVALID_VALUE,
  UNKNOWN_CODE,
  VALUE_TOO_LONG,
  /** A mandatory record or group is missing. */
  MISSING,
  /** A record or group occurs more often than allowed. */
  TOO_MANY,
  /** A known record at a position the structure does not allow. */
  UNEXPECTED_RECORD,
  /** A record inside a group carries a different key value (tolerated with keyMismatch: warn). */
  KEY_MISMATCH,
  DUPLICATE_VALUE,
  MISSING_VALUE,
  UNEXPECTED_VALUE
}

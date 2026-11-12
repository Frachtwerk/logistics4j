package de.frachtwerk.fortras4j.schema;

import java.util.List;

/** A layout file could not be loaded; lists every problem found, not just the first. */
public class SchemaException extends RuntimeException {

  private final List<String> errors;

  public SchemaException(List<String> errors) {
    super("Invalid FORTRAS layout:\n  - " + String.join("\n  - ", errors));
    this.errors = List.copyOf(errors);
  }

  public SchemaException(String error) {
    this(List.of(error));
  }

  public List<String> errors() {
    return errors;
  }
}

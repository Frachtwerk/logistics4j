package de.frachtwerk.fortras4j.schema;

import java.util.List;

/**
 * Check across the records of one group iteration.
 *
 * <ul>
 *   <li>{@code UNIQUE}: no two records carry the same value in {@code field}
 *   <li>{@code REQUIRED}: every value in {@code values} occurs at least once
 *   <li>{@code FIRST}: the first such record carries one of {@code values}
 * </ul>
 */
public record Constraint(Kind kind, String recordId, String field, List<String> values) {

  public enum Kind {
    UNIQUE,
    REQUIRED,
    FIRST
  }

  public Constraint {
    values = List.copyOf(values);
  }
}

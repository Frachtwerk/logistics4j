/*
 * Copyright (C) 2026 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
 *
 * This file is part of logistics4j.
 *
 * logistics4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * logistics4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with logistics4j. If not, see <http://www.gnu.org/licenses/>.
 */

package de.frachtwerk.fortras4j.schema;

import de.frachtwerk.fortras4j.Severity;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Restricts the values of one field of a record depending on other fields of the same record, e.g.
 * the status codes allowed per scan type. The first case whose {@code when} matches decides;
 * without a match {@code otherwise} applies, and without {@code otherwise} any value is allowed.
 * Rules are checked when reading.
 *
 * <p>Values are compared after coercion by the field type: integers as {@link Long}, codes and
 * strings as text.
 *
 * @param name key of the rule in the layout
 * @param otherwise values allowed when no case matches; {@code null} if then any value is allowed
 */
public record ValueRule(
    String name,
    String recordId,
    String field,
    Severity severity,
    List<Case> cases,
    Set<Object> otherwise) {

  /**
   * @param when allowed values per condition field; all of them must match
   * @param allowed values allowed for the rule's field when this case matches
   */
  public record Case(Map<String, Set<Object>> when, Set<Object> allowed) {

    public Case {
      Map<String, Set<Object>> copy = new LinkedHashMap<>();
      when.forEach((f, values) -> copy.put(f, unmodifiable(values)));
      when = Collections.unmodifiableMap(copy);
      allowed = unmodifiable(allowed);
    }

    boolean matches(Map<String, ?> values) {
      return when.entrySet().stream().allMatch(e -> e.getValue().contains(values.get(e.getKey())));
    }
  }

  public ValueRule {
    cases = List.copyOf(cases);
    otherwise = otherwise == null ? null : unmodifiable(otherwise);
  }

  /** The fields the cases depend on. */
  public Set<String> conditionFields() {
    Set<String> fields = new LinkedHashSet<>();
    cases.forEach(c -> fields.addAll(c.when().keySet()));
    return Collections.unmodifiableSet(fields);
  }

  /**
   * The values allowed for {@link #field()}, given the coerced values of the condition fields.
   *
   * @return empty if any value is allowed, or if a condition field has no value
   */
  public Optional<Set<Object>> allowed(Map<String, ?> values) {
    if (conditionFields().stream().anyMatch(f -> values.get(f) == null)) {
      return Optional.empty();
    }
    return matchingCase(values).map(Case::allowed).or(() -> Optional.ofNullable(otherwise));
  }

  /** Describes why {@code value} violates this rule, or empty if it is allowed. */
  public Optional<String> violation(Object value, Map<String, ?> values) {
    Optional<Set<Object>> allowed = allowed(values);
    if (value == null || allowed.isEmpty() || allowed.get().contains(value)) {
      return Optional.empty();
    }
    String condition =
        matchingCase(values).map(c -> " for " + describe(c.when().keySet(), values)).orElse("");
    return Optional.of(
        value
            + " is not allowed"
            + condition
            + " (rule '"
            + name
            + "'); allowed: "
            + allowed.get());
  }

  private Optional<Case> matchingCase(Map<String, ?> values) {
    return cases.stream().filter(c -> c.matches(values)).findFirst();
  }

  private static String describe(Set<String> fields, Map<String, ?> values) {
    StringBuilder text = new StringBuilder();
    for (String f : fields) {
      if (!text.isEmpty()) {
        text.append(", ");
      }
      text.append(f).append(' ').append(values.get(f));
    }
    return text.toString();
  }

  private static Set<Object> unmodifiable(Set<Object> values) {
    return Collections.unmodifiableSet(new LinkedHashSet<>(values));
  }
}

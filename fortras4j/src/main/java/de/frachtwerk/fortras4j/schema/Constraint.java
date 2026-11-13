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

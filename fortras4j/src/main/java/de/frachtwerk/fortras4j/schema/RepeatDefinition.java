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
 * A block of fields that repeats {@code count} times, every {@code stride} characters, starting at
 * {@code start}; e.g. the six text keys of H00. Its value is a list of maps.
 *
 * <p>With {@code skipEmpty} blocks without any value are left out of the list when reading. If
 * {@code presentIf} names a field, a block counts as present only if that field has a value; useful
 * when unused blocks still carry zeros in numeric fields.
 */
public record RepeatDefinition(
    String name,
    int start,
    int count,
    int stride,
    List<FieldDefinition> fields,
    boolean skipEmpty,
    String presentIf,
    String description)
    implements FieldElement {

  public RepeatDefinition {
    fields = List.copyOf(fields);
  }

  @Override
  public int end() {
    return start + count * stride - 1;
  }

  /** Absolute start position of {@code field} in block {@code index} (0-based). */
  public int absoluteStart(int index, FieldDefinition field) {
    return start + index * stride + field.start() - 1;
  }

  public FieldDefinition field(String fieldName) {
    return fields.stream().filter(f -> f.name().equals(fieldName)).findFirst().orElse(null);
  }
}

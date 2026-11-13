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

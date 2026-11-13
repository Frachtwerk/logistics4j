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

import de.frachtwerk.fortras4j.type.FieldCodec;

/**
 * A fixed-width field. A {@code required} field without a value is an error when reading and
 * writing.
 *
 * <p>Positions are 1-based and inclusive, exactly as printed in the FORTRAS record descriptions.
 * Inside a {@link RepeatDefinition} they are relative to the block.
 */
public record FieldDefinition(
    String name,
    int start,
    int end,
    String type,
    FieldCodec codec,
    Object defaultValue,
    boolean required,
    String description)
    implements FieldElement {

  public int length() {
    return end - start + 1;
  }
}

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

package de.frachtwerk.fortras4j.type;

/**
 * A named field type usable in layout files ({@code type: date}). Custom types can be registered
 * via {@link de.frachtwerk.fortras4j.schema.FortrasSchemas.Builder#type(FieldType)}.
 */
public interface FieldType {

  String name();

  /**
   * Creates the codec for one field. Called once while loading the layout, so invalid options are
   * reported at startup and not while parsing.
   *
   * @throws IllegalArgumentException if the options are invalid
   */
  FieldCodec create(FieldOptions options, FieldContext context);
}

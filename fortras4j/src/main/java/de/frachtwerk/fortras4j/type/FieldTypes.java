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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry of field types. Built in: {@code string}, {@code code}, {@code currency}, {@code
 * integer}, {@code decimal}, {@code date}, {@code time}, {@code datetime}, {@code boolean}.
 */
public final class FieldTypes {

  private final Map<String, FieldType> types = new LinkedHashMap<>();

  private FieldTypes() {}

  public static FieldTypes builtIn() {
    FieldTypes registry = new FieldTypes();
    registry.register(TextCodec.type("string", TextCodec.Kind.STRING));
    registry.register(TextCodec.type("code", TextCodec.Kind.CODE));
    registry.register(TextCodec.type("currency", TextCodec.Kind.CURRENCY));
    registry.register(NumberCodec.integerType());
    registry.register(NumberCodec.decimalType());
    registry.register(TemporalCodec.type(TemporalCodec.Kind.DATE));
    registry.register(TemporalCodec.type(TemporalCodec.Kind.TIME));
    registry.register(TemporalCodec.type(TemporalCodec.Kind.DATETIME));
    registry.register(BooleanCodec.type());
    return registry;
  }

  /** Adds a type; a type with the same name replaces the existing one. */
  public FieldTypes register(FieldType type) {
    types.put(type.name(), type);
    return this;
  }

  public FieldType get(String name) {
    return types.get(name);
  }

  public Set<String> names() {
    return types.keySet();
  }
}

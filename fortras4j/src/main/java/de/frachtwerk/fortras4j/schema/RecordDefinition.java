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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Layout of one record type ("Satzart"), identified by the prefix its lines start with.
 *
 * @param length length of a newly written line; at least the end of the last field
 */
public record RecordDefinition(
    String id, String prefix, String description, List<FieldElement> elements, int length) {

  public RecordDefinition {
    elements = List.copyOf(elements);
  }

  public FieldElement element(String name) {
    for (FieldElement element : elements) {
      if (element.name().equals(name)) {
        return element;
      }
    }
    return null;
  }

  public boolean hasField(String name) {
    return element(name) instanceof FieldDefinition;
  }

  public Map<String, FieldElement> elementsByName() {
    Map<String, FieldElement> result = new LinkedHashMap<>();
    elements.forEach(element -> result.put(element.name(), element));
    return result;
  }
}

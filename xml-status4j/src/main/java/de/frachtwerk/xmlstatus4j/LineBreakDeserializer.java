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

package de.frachtwerk.xmlstatus4j;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Text values may be wrapped over several lines; they are joined and trimmed, or with {@code
 * replaceBySpace} joined by one space.
 */
final class LineBreakDeserializer extends ValueDeserializer<String> {

  private final boolean replaceBySpace;

  LineBreakDeserializer() {
    this(false);
  }

  LineBreakDeserializer(boolean replaceBySpace) {
    this.replaceBySpace = replaceBySpace;
  }

  @Override
  public String deserialize(JsonParser parser, DeserializationContext context) {
    String text = parser.getString();
    if (replaceBySpace) {
      return text.replaceAll("[ \\t]*[\\n\\r]+[ \\t]*", " ").trim();
    }
    return text.replaceAll("[\\n\\r]", "").trim();
  }
}

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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Allowed codes of a field together with their descriptions. A strict code list turns unknown codes
 * into errors, a lenient one into warnings; the value is retained either way.
 */
public record CodeList(String name, Map<String, String> codes, boolean strict) {

  public CodeList {
    // descriptions are optional, so null values must be allowed
    codes = Collections.unmodifiableMap(new LinkedHashMap<>(codes));
  }

  public boolean contains(String code) {
    return codes.containsKey(code);
  }

  public String description(String code) {
    return codes.get(code);
  }
}

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

package de.frachtwerk.fortras4j;

import de.frachtwerk.fortras4j.schema.FortrasSchemas;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

final class Demo {

  static final FortrasSchemas SCHEMAS =
      FortrasSchemas.builder().addClasspath("layouts/demo.yaml").build();

  private Demo() {}

  static FortrasSchemas.Builder withLayout(String yaml) {
    return FortrasSchemas.builder()
        .add("test.yaml", new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
  }

  /** A minimal valid layout to which a test appends or replaces parts. */
  static String layout(String records, String structure) {
    return """
    id: t
    detect: "^A"
    records:
    %s
    structure:
    %s
    """
        .formatted(records.indent(2), structure.indent(2));
  }
}

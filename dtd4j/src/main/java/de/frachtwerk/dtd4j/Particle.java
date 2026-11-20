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

package de.frachtwerk.dtd4j;

import java.util.List;
import java.util.stream.Collectors;

/** A part of an element content model: an element name, a sequence or a choice. */
public sealed interface Particle {

  Occurs occurs();

  /** An element, e.g. {@code SDG+}. */
  record Name(String name, Occurs occurs) implements Particle {
    @Override
    public String toString() {
      return name + occurs.symbol();
    }
  }

  /** Particles in this order, e.g. {@code (FAR?, SDG+)}. */
  record Sequence(List<Particle> items, Occurs occurs) implements Particle {
    public Sequence {
      items = List.copyOf(items);
    }

    @Override
    public String toString() {
      return items.stream().map(Object::toString).collect(Collectors.joining(", ", "(", ")"))
          + occurs.symbol();
    }
  }

  /** Exactly one of the particles, e.g. {@code (A | B)}. */
  record Choice(List<Particle> options, Occurs occurs) implements Particle {
    public Choice {
      options = List.copyOf(options);
    }

    @Override
    public String toString() {
      return options.stream().map(Object::toString).collect(Collectors.joining(" | ", "(", ")"))
          + occurs.symbol();
    }
  }
}

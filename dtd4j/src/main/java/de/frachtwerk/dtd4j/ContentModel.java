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

import java.util.Set;
import java.util.TreeSet;

/** What an element may contain, as declared by {@code <!ELEMENT name content>}. */
public sealed interface ContentModel {

  /** {@code EMPTY}: no content. */
  record Empty() implements ContentModel {
    @Override
    public String toString() {
      return "EMPTY";
    }
  }

  /** {@code ANY}: any declared elements and text. */
  record Any() implements ContentModel {
    @Override
    public String toString() {
      return "ANY";
    }
  }

  /** {@code (#PCDATA)}: text only. */
  record Text() implements ContentModel {
    @Override
    public String toString() {
      return "(#PCDATA)";
    }
  }

  /** {@code (#PCDATA | a | b)*}: text mixed with the given elements in any order. */
  record Mixed(Set<String> names) implements ContentModel {
    public Mixed {
      names = Set.copyOf(names);
    }

    @Override
    public String toString() {
      return "(#PCDATA | " + String.join(" | ", new TreeSet<>(names)) + ")*";
    }
  }

  /** Element content, e.g. {@code (FAR?, SDG+)+}. */
  record Children(Particle particle) implements ContentModel {
    @Override
    public String toString() {
      return particle.toString();
    }
  }
}

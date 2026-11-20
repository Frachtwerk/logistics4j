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

import de.frachtwerk.dtd4j.ContentModel.Children;
import de.frachtwerk.dtd4j.Particle.Choice;
import de.frachtwerk.dtd4j.Particle.Name;
import de.frachtwerk.dtd4j.Particle.Sequence;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * How the children of an element are bound: which child elements become lists, and the repeated
 * group whose occurrences become objects of their own.
 *
 * @param group the repeated group named in the layout, or {@code null}
 * @param lists for each child element outside the group, whether it may occur more than once
 * @param groupLists the same for child elements inside the group
 * @param groupOrder the child elements inside the group in the order of the content model
 */
record Shape(
    Particle group,
    Map<String, Boolean> lists,
    Map<String, Boolean> groupLists,
    List<String> groupOrder) {

  static final Shape NONE = new Shape(null, Map.of(), Map.of(), List.of());

  /** The shapes of all elements with element content. */
  static Map<String, Shape> of(XmlLayout layout) {
    Map<String, Shape> shapes = new HashMap<>();
    for (ElementDeclaration declaration : layout.dtd().elements().values()) {
      if (declaration.content() instanceof Children children) {
        Particle group = layout.groupParticle(declaration.name()).orElse(null);
        Map<String, Boolean> lists = new HashMap<>();
        Map<String, Boolean> groupLists = new HashMap<>();
        collect(children.particle(), false, group, lists, groupLists);
        List<String> groupOrder = new ArrayList<>();
        if (group != null) {
          names(group, groupOrder);
        }
        shapes.put(
            declaration.name(),
            new Shape(group, Map.copyOf(lists), Map.copyOf(groupLists), List.copyOf(groupOrder)));
      }
    }
    return Map.copyOf(shapes);
  }

  private static void collect(
      Particle particle,
      boolean repeated,
      Particle group,
      Map<String, Boolean> lists,
      Map<String, Boolean> groupLists) {
    if (particle == group) {
      // the group's own occurrences become objects; its content is a scope of its own
      for (Particle part : parts(particle)) {
        collect(part, false, null, groupLists, groupLists);
      }
      return;
    }
    boolean many = repeated || particle.occurs().repeatable();
    switch (particle) {
      case Name name -> lists.merge(name.name(), many, (a, b) -> true);
      case Sequence sequence -> {
        for (Particle item : sequence.items()) {
          collect(item, many, group, lists, groupLists);
        }
      }
      case Choice choice -> {
        for (Particle option : choice.options()) {
          collect(option, many, group, lists, groupLists);
        }
      }
    }
  }

  private static void names(Particle particle, List<String> into) {
    if (particle instanceof Name name) {
      if (!into.contains(name.name())) {
        into.add(name.name());
      }
    } else {
      parts(particle).forEach(part -> names(part, into));
    }
  }

  static List<Particle> parts(Particle particle) {
    return switch (particle) {
      case Sequence sequence -> sequence.items();
      case Choice choice -> choice.options();
      case Name name -> List.of(name);
    };
  }

  boolean isList(String element, boolean inGroup) {
    return (inGroup ? groupLists : lists).getOrDefault(element, false);
  }
}

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

import java.util.List;

/**
 * Element of the file structure: a record, a group (sequence of children) or a choice between
 * alternatives.
 *
 * <p>Nodes are compared by identity wherever the parser or binder tracks them, because two
 * structurally equal references (e.g. the same record at two places) are different positions.
 */
public sealed interface StructureNode
    permits StructureNode.RecordRef, StructureNode.GroupNode, StructureNode.ChoiceNode {

  Occurs occurs();

  /** Property name of this element when a group is bound to a map or POJO. */
  String bindingName();

  /**
   * A child as seen by the binder.
   *
   * @param repeatable whether it binds to a list; true for options of a repeatable choice
   */
  record Binding(StructureNode node, boolean repeatable) {
    public String name() {
      return node.bindingName();
    }
  }

  /** Children as seen by the binder: options of choices are flattened into the parent. */
  static List<Binding> bindings(List<StructureNode> children) {
    List<Binding> result = new java.util.ArrayList<>();
    collectBindings(children, false, result);
    return result;
  }

  private static void collectBindings(
      List<StructureNode> children, boolean repeatedChoice, List<Binding> into) {
    for (StructureNode child : children) {
      if (child instanceof ChoiceNode choice) {
        collectBindings(choice.options(), repeatedChoice || choice.occurs().repeatable(), into);
      } else {
        into.add(new Binding(child, repeatedChoice || child.occurs().repeatable()));
      }
    }
  }

  /**
   * @param as property name used by the binder; defaults to the record id
   */
  record RecordRef(String recordId, Occurs occurs, String as) implements StructureNode {
    @Override
    public String bindingName() {
      return as != null ? as : recordId;
    }
  }

  /**
   * @param key optional field shared by the records of one iteration (e.g. the bordero position); a
   *     record carrying a different value ends the current iteration
   * @param keyTolerant if true, only a record that can start the group ends the iteration on a
   *     different key; other records stay in the group and the difference is a warning. Suits files
   *     whose position numbers in subordinate records are unreliable.
   */
  record GroupNode(
      String name,
      Occurs occurs,
      String key,
      boolean keyTolerant,
      List<StructureNode> children,
      List<Constraint> constraints)
      implements StructureNode {

    public GroupNode {
      children = List.copyOf(children);
      constraints = List.copyOf(constraints);
    }

    @Override
    public String bindingName() {
      return name;
    }
  }

  /** Exactly one of the options per occurrence. Options are flattened into the parent binding. */
  record ChoiceNode(String name, Occurs occurs, List<StructureNode> options)
      implements StructureNode {

    public ChoiceNode {
      options = List.copyOf(options);
    }

    @Override
    public String bindingName() {
      return name;
    }
  }
}

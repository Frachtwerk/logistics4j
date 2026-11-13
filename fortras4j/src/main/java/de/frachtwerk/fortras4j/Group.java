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

import de.frachtwerk.fortras4j.schema.StructureNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** One iteration of a structure group, e.g. one shipment of a bordero. */
public final class Group implements Node {

  private final StructureNode.GroupNode definition;
  private final List<Node> children = new ArrayList<>();

  public Group(StructureNode.GroupNode definition) {
    this.definition = definition;
  }

  public String name() {
    return definition.name();
  }

  public StructureNode.GroupNode definition() {
    return definition;
  }

  /** Direct children in file order, including records that did not fit the structure. */
  public List<Node> children() {
    return Collections.unmodifiableList(children);
  }

  public Group add(Node child) {
    children.add(child);
    return this;
  }

  /** Direct child records of the given type. */
  public List<Record> records(String recordId) {
    return children.stream()
        .filter(Record.class::isInstance)
        .map(Record.class::cast)
        .filter(r -> recordId.equals(r.id()))
        .toList();
  }

  public Optional<Record> record(String recordId) {
    return records(recordId).stream().findFirst();
  }

  /** Direct child groups with the given name. */
  public List<Group> groups(String name) {
    return children.stream()
        .filter(Group.class::isInstance)
        .map(Group.class::cast)
        .filter(g -> g.name().equals(name))
        .toList();
  }

  public Optional<Group> group(String name) {
    return groups(name).stream().findFirst();
  }

  @Override
  public List<Record> flatten() {
    List<Record> result = new ArrayList<>();
    children.forEach(child -> result.addAll(child.flatten()));
    return result;
  }

  @Override
  public String toString() {
    return name() + children;
  }
}

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

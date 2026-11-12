package de.frachtwerk.fortras4j;

import de.frachtwerk.fortras4j.schema.Constraint;
import de.frachtwerk.fortras4j.schema.FortrasSchema;
import de.frachtwerk.fortras4j.schema.Occurs;
import de.frachtwerk.fortras4j.schema.StructureNode;
import de.frachtwerk.fortras4j.schema.StructureNode.ChoiceNode;
import de.frachtwerk.fortras4j.schema.StructureNode.GroupNode;
import de.frachtwerk.fortras4j.schema.StructureNode.RecordRef;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Assigns a flat list of records to the structure of a layout.
 *
 * <p>Matching is greedy with one record of look-ahead, which suits FORTRAS: each record type opens
 * or continues a well-defined level. Rules:
 *
 * <ul>
 *   <li>A group iteration starts with a record that can begin the group and ends when the next
 *       record fits none of its remaining children.
 *   <li>An element with {@code max = 1} delimits: a second occurrence starts a new iteration of the
 *       enclosing group. Higher limits are counted, and excess records are reported as {@code
 *       TOO_MANY} but kept.
 *   <li>A group with a {@code key} ends when a record carries a different key value.
 *   <li>A known record that belongs to the current group but arrives out of order is kept in the
 *       group and reported as {@code UNEXPECTED_RECORD}; unknown lines are kept where they appear.
 * </ul>
 */
final class StructureMatcher {

  private final FortrasSchema schema;
  private final List<Record> records;
  private final List<Problem> problems;
  private final Map<StructureNode, Set<String>> firstSets = new IdentityHashMap<>();
  private final Map<StructureNode, Set<String>> descendantSets = new IdentityHashMap<>();
  private final Deque<KeyScope> keyScopes = new ArrayDeque<>();

  /** Record types the enclosing levels can still accept; such records are never "misplaced". */
  private final Deque<Set<String>> follow = new ArrayDeque<>();

  private int position;

  private static final class KeyScope {
    private final GroupNode group;
    private Object value;

    private KeyScope(GroupNode group) {
      this.group = group;
    }

    private String field() {
      return group.key();
    }
  }

  StructureMatcher(FortrasSchema schema, List<Record> records, List<Problem> problems) {
    this.schema = schema;
    this.records = records;
    this.problems = problems;
  }

  Group match() {
    GroupNode rootNode = schema.root();
    Group root = new Group(rootNode);
    matchBody(rootNode, root);
    while (position < records.size()) {
      Record record = records.get(position++);
      root.add(record);
      if (record.isKnown()) {
        record.structure(null);
        unexpected(record);
      }
    }
    return root;
  }

  // ---- matching ---------------------------------------------------------------------------------

  private void matchRepeated(StructureNode node, Group parent) {
    Occurs occurs = node.occurs();
    int count = 0;
    int firstExcessLine = 0;
    Record next;
    while ((next = peek(parent)) != null && canStart(node, next)) {
      if (count == 1 && occurs.max() == 1) {
        break;
      }
      if (count == occurs.max()) {
        firstExcessLine = lineOf(position);
      }
      int before = position;
      matchOnce(node, parent);
      if (position == before) {
        break;
      }
      count++;
    }
    if (count < occurs.min()) {
      problems.add(
          new Problem(
              Severity.ERROR,
              ProblemCode.MISSING,
              lineOf(position),
              node instanceof RecordRef ref ? ref.recordId() : null,
              node instanceof RecordRef ? null : node.bindingName(),
              describe(node)
                  + " expected "
                  + occurs
                  + " time(s), found "
                  + count
                  + (parent.definition() == schema.root()
                      ? ""
                      : " in group '" + parent.name() + "'")));
    } else if (count > occurs.max()) {
      problems.add(
          new Problem(
              Severity.ERROR,
              ProblemCode.TOO_MANY,
              firstExcessLine,
              node instanceof RecordRef ref ? ref.recordId() : null,
              node instanceof RecordRef ? null : node.bindingName(),
              describe(node) + " allowed " + occurs + " time(s), found " + count));
    }
  }

  private void matchOnce(StructureNode node, Group parent) {
    switch (node) {
      case RecordRef ref -> {
        Record record = records.get(position++);
        record.structure(ref);
        bindKeys(record);
        parent.add(record);
      }
      case GroupNode groupNode -> {
        Group group = new Group(groupNode);
        parent.add(group);
        if (groupNode.key() != null) {
          keyScopes.push(new KeyScope(groupNode));
        }
        matchBody(groupNode, group);
        if (groupNode.key() != null) {
          keyScopes.pop();
        }
        checkConstraints(groupNode, group);
      }
      case ChoiceNode choice -> {
        Record next = peek(parent);
        for (StructureNode option : choice.options()) {
          if (next != null && canStart(option, next)) {
            matchOnce(option, parent);
            return;
          }
        }
      }
    }
  }

  private void matchBody(GroupNode node, Group group) {
    List<StructureNode> children = node.children();
    Set<String> outer = follow.isEmpty() ? Set.of() : follow.peek();
    for (int i = 0; i < children.size(); i++) {
      keepMisplaced(node, group, i);
      Set<String> next = new HashSet<>(outer);
      for (int j = i; j < children.size(); j++) {
        next.addAll(first(children.get(j)));
      }
      follow.push(next);
      matchRepeated(children.get(i), group);
      follow.pop();
    }
    keepMisplaced(node, group, children.size());
  }

  /**
   * Keeps records that belong somewhere into this group but fit none of the remaining children, so
   * that one misplaced line does not cascade into errors for the rest of the file.
   */
  private void keepMisplaced(GroupNode node, Group group, int nextChild) {
    Record next;
    while ((next = peek(group)) != null) {
      String id = next.id();
      if (!descendants(node).contains(id)
          || first(node).contains(id)
          || (!follow.isEmpty() && follow.peek().contains(id))
          || !keyCompatible(next)) {
        return;
      }
      for (int j = nextChild; j < node.children().size(); j++) {
        if (first(node.children().get(j)).contains(id)) {
          return;
        }
      }
      position++;
      next.structure(null);
      group.add(next);
      unexpected(next);
    }
  }

  /** Next known record; unknown lines on the way are attached to {@code group} as they are. */
  private Record peek(Group group) {
    while (position < records.size() && !records.get(position).isKnown()) {
      group.add(records.get(position++));
    }
    return position < records.size() ? records.get(position) : null;
  }

  private boolean canStart(StructureNode node, Record record) {
    return first(node).contains(record.id()) && keyCompatible(record);
  }

  private boolean keyCompatible(Record record) {
    for (KeyScope scope : keyScopes) {
      if (mismatches(scope, record)
          && !(scope.group.keyTolerant() && !first(scope.group).contains(record.id()))) {
        return false;
      }
    }
    return true;
  }

  private boolean mismatches(KeyScope scope, Record record) {
    if (scope.value == null || !record.definition().hasField(scope.field())) {
      return false;
    }
    Object value = record.values().get(scope.field());
    return value != null && !value.equals(scope.value);
  }

  private void bindKeys(Record record) {
    for (KeyScope scope : keyScopes) {
      if (!record.definition().hasField(scope.field())) {
        continue;
      }
      if (scope.value == null) {
        scope.value = record.values().get(scope.field());
      } else if (mismatches(scope, record)) {
        problems.add(
            new Problem(
                Severity.WARNING,
                ProblemCode.KEY_MISMATCH,
                lineOf(position - 1),
                record.id(),
                scope.field(),
                "'"
                    + record.values().get(scope.field())
                    + "' differs from '"
                    + scope.value
                    + "' of group '"
                    + scope.group.name()
                    + "'"));
      }
    }
  }

  // ---- constraints ------------------------------------------------------------------------------

  private void checkConstraints(GroupNode node, Group group) {
    if (node.constraints().isEmpty()) {
      return;
    }
    List<Record> members =
        group.flatten().stream().filter(r -> r.isKnown() && r.structure() != null).toList();
    for (Constraint constraint : node.constraints()) {
      List<Record> concerned =
          members.stream().filter(r -> constraint.recordId().equals(r.id())).toList();
      switch (constraint.kind()) {
        case UNIQUE -> {
          Map<Object, Record> seen = new HashMap<>();
          for (Record record : concerned) {
            Object value = record.values().get(constraint.field());
            if (value != null && seen.putIfAbsent(value, record) != null) {
              constraintProblem(
                  ProblemCode.DUPLICATE_VALUE,
                  record,
                  constraint,
                  "value '" + value + "' occurs more than once in group '" + node.name() + "'");
            }
          }
        }
        case REQUIRED -> {
          Set<String> present = new HashSet<>();
          concerned.forEach(
              r -> present.add(Objects.toString(r.values().get(constraint.field()), null)));
          Set<String> missing = new LinkedHashSet<>(constraint.values());
          missing.removeAll(present);
          if (!missing.isEmpty()) {
            Record anchor = members.isEmpty() ? null : members.getFirst();
            constraintProblem(
                ProblemCode.MISSING_VALUE,
                anchor,
                constraint,
                "group '"
                    + node.name()
                    + "' lacks "
                    + constraint.recordId()
                    + " with "
                    + constraint.field()
                    + " "
                    + missing);
          }
        }
        case FIRST -> {
          if (!concerned.isEmpty()) {
            Record first = concerned.getFirst();
            Object value = first.values().get(constraint.field());
            if (!constraint.values().contains(Objects.toString(value, null))) {
              constraintProblem(
                  ProblemCode.UNEXPECTED_VALUE,
                  first,
                  constraint,
                  "group '"
                      + node.name()
                      + "' must start with "
                      + constraint.recordId()
                      + " "
                      + constraint.field()
                      + " "
                      + constraint.values()
                      + ", found '"
                      + value
                      + "'");
            }
          }
        }
      }
    }
  }

  private void constraintProblem(
      ProblemCode code, Record record, Constraint constraint, String message) {
    problems.add(
        new Problem(
            Severity.ERROR,
            code,
            record == null ? 0 : lineOf(records.indexOf(record)),
            constraint.recordId(),
            constraint.field(),
            message));
  }

  // ---- helpers ----------------------------------------------------------------------------------

  private void unexpected(Record record) {
    problems.add(
        new Problem(
            Severity.ERROR,
            ProblemCode.UNEXPECTED_RECORD,
            lineOf(records.indexOf(record)),
            record.id(),
            null,
            "record " + record.id() + " is not allowed at this position"));
  }

  /** Line numbers follow list positions, so they also work for records created in code. */
  private int lineOf(int index) {
    return Math.min(index, records.size()) + 1;
  }

  private Set<String> first(StructureNode node) {
    Set<String> cached = firstSets.get(node);
    if (cached != null) {
      return cached;
    }
    Set<String> result = new HashSet<>();
    switch (node) {
      case RecordRef ref -> result.add(ref.recordId());
      case ChoiceNode choice -> choice.options().forEach(o -> result.addAll(first(o)));
      case GroupNode group -> {
        for (StructureNode child : group.children()) {
          result.addAll(first(child));
          if (!nullable(child)) {
            break;
          }
        }
      }
    }
    firstSets.put(node, result);
    return result;
  }

  private boolean nullable(StructureNode node) {
    if (node.occurs().min() == 0) {
      return true;
    }
    return switch (node) {
      case RecordRef ref -> false;
      case ChoiceNode choice -> choice.options().stream().anyMatch(this::nullable);
      case GroupNode group -> group.children().stream().allMatch(this::nullable);
    };
  }

  private Set<String> descendants(GroupNode node) {
    return descendantSets.computeIfAbsent(
        node,
        n -> {
          Set<String> ids = new HashSet<>();
          collect(n, ids);
          return ids;
        });
  }

  private static void collect(StructureNode node, Set<String> into) {
    switch (node) {
      case RecordRef ref -> into.add(ref.recordId());
      case GroupNode group -> group.children().forEach(c -> collect(c, into));
      case ChoiceNode choice -> choice.options().forEach(o -> collect(o, into));
    }
  }

  private static String describe(StructureNode node) {
    return switch (node) {
      case RecordRef ref -> "record " + ref.recordId();
      case GroupNode group -> "group '" + group.name() + "'";
      case ChoiceNode choice -> "one of '" + choice.name() + "'";
    };
  }
}

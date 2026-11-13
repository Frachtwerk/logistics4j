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

package de.frachtwerk.fortras4j.bind;

import de.frachtwerk.fortras4j.FortrasDocument;
import de.frachtwerk.fortras4j.Group;
import de.frachtwerk.fortras4j.Node;
import de.frachtwerk.fortras4j.Record;
import de.frachtwerk.fortras4j.schema.FortrasSchema;
import de.frachtwerk.fortras4j.schema.StructureNode;
import de.frachtwerk.fortras4j.schema.StructureNode.GroupNode;
import de.frachtwerk.fortras4j.schema.StructureNode.RecordRef;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Binds documents to your own classes and back, using the names from the layout.
 *
 * <p>A group becomes an object whose properties are named after its children: a record by its
 * {@code as} name (default: record id), a group by its name. Children that may occur more than once
 * become lists. Record fields map to properties of the same name, repeated blocks to lists.
 *
 * <pre>{@code
 * record Bordero(Kopf kopf, List<Sendung> sendungen) {}
 * Bordero bordero = binder.toObject(document, Bordero.class);
 * FortrasDocument again = binder.fromObject(bordero, schema);
 * }</pre>
 *
 * <p>Properties of your classes that the layout does not know are ignored when reading; when
 * writing they are rejected unless {@link #ignoreUnknownProperties(boolean)} is set, so typos in
 * property names do not silently drop data.
 */
public final class FortrasBinder {

  private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

  private final JsonMapper mapper;
  private final boolean ignoreUnknownProperties;

  private FortrasBinder(JsonMapper mapper, boolean ignoreUnknownProperties) {
    this.mapper = Objects.requireNonNull(mapper);
    this.ignoreUnknownProperties = ignoreUnknownProperties;
  }

  public static FortrasBinder create() {
    return withMapper(
        JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build());
  }

  /**
   * Uses your mapper, e.g. with custom modules. It should not fail on unknown properties, because
   * the binder produces every field of the layout.
   */
  public static FortrasBinder withMapper(JsonMapper mapper) {
    return new FortrasBinder(mapper, false);
  }

  public FortrasBinder ignoreUnknownProperties(boolean ignore) {
    return new FortrasBinder(mapper, ignore);
  }

  // ---- document -> object -----------------------------------------------------------------------

  public <T> T toObject(FortrasDocument document, Class<T> type) {
    return mapper.convertValue(toMap(document.root()), type);
  }

  public <T> T toObject(Group group, Class<T> type) {
    return mapper.convertValue(toMap(group), type);
  }

  public <T> T toObject(Record record, Class<T> type) {
    return mapper.convertValue(record.values(), type);
  }

  /** The generic form of {@link #toObject}: nested maps and lists. */
  public Map<String, Object> toMap(Group group) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (StructureNode.Binding binding : StructureNode.bindings(group.definition().children())) {
      StructureNode child = binding.node();
      List<Object> values = new ArrayList<>();
      for (Node node : group.children()) {
        if (node instanceof Record record && record.structure() == child) {
          values.add(new LinkedHashMap<>(record.values()));
        } else if (node instanceof Group sub && sub.definition() == child) {
          values.add(toMap(sub));
        }
      }
      if (binding.repeatable()) {
        result.put(binding.name(), values);
      } else {
        result.put(binding.name(), values.isEmpty() ? null : values.getFirst());
      }
    }
    return result;
  }

  // ---- object -> document -----------------------------------------------------------------------

  /**
   * Builds a document from an object shaped like the layout. Records are created in structure
   * order; fields not set keep the layout's {@code default}. Structure problems surface when the
   * document is written.
   */
  public FortrasDocument fromObject(Object object, FortrasSchema schema) {
    Map<String, Object> map = mapper.convertValue(object, MAP);
    Group root = new Group(schema.root());
    fill(root, schema.root(), map, schema, "");
    return new FortrasDocument(schema, root, List.of());
  }

  private void fill(
      Group group,
      GroupNode definition,
      Map<String, Object> map,
      FortrasSchema schema,
      String path) {
    Set<String> known = new HashSet<>();
    for (StructureNode.Binding binding : StructureNode.bindings(definition.children())) {
      StructureNode child = binding.node();
      String name = binding.name();
      known.add(name);
      String childPath = path.isEmpty() ? name : path + "." + name;
      List<?> items = items(map.get(name), binding.repeatable(), childPath);
      for (int i = 0; i < items.size(); i++) {
        String itemPath = binding.repeatable() ? childPath + "[" + i + "]" : childPath;
        Map<String, Object> item = asMap(items.get(i), itemPath);
        switch (child) {
          case RecordRef ref -> group.add(record(schema, ref.recordId(), item, itemPath));
          case GroupNode groupNode -> {
            Group sub = new Group(groupNode);
            fill(sub, groupNode, item, schema, itemPath);
            group.add(sub);
          }
          case StructureNode.ChoiceNode choice ->
              throw new IllegalStateException("choices are flattened: " + choice.name());
        }
      }
    }
    rejectUnknown(map, known, path);
  }

  private Record record(
      FortrasSchema schema, String recordId, Map<String, Object> item, String path) {
    Record record = Record.create(schema, recordId);
    for (Map.Entry<String, Object> entry : item.entrySet()) {
      if (entry.getValue() == null) {
        continue;
      }
      if (record.definition().element(entry.getKey()) == null) {
        if (ignoreUnknownProperties) {
          continue;
        }
        throw new IllegalArgumentException(
            path + ": record " + recordId + " has no field '" + entry.getKey() + "'");
      }
      try {
        record.set(entry.getKey(), entry.getValue());
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(path + "." + entry.getKey() + ": " + e.getMessage(), e);
      }
    }
    return record;
  }

  private static List<?> items(Object value, boolean repeatable, String path) {
    if (value == null) {
      return List.of();
    }
    if (value instanceof List<?> list) {
      if (!repeatable && list.size() > 1) {
        throw new IllegalArgumentException(path + ": at most one element allowed");
      }
      return list;
    }
    return List.of(value);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> asMap(Object value, String path) {
    if (value instanceof Map<?, ?> map) {
      return (Map<String, Object>) map;
    }
    throw new IllegalArgumentException(path + ": expected an object, got " + value);
  }

  private void rejectUnknown(Map<String, Object> map, Set<String> known, String path) {
    if (ignoreUnknownProperties) {
      return;
    }
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      if (!known.contains(entry.getKey()) && entry.getValue() != null) {
        throw new IllegalArgumentException(
            (path.isEmpty() ? "" : path + ": ")
                + "the layout has no element '"
                + entry.getKey()
                + "'; known: "
                + known);
      }
    }
  }
}

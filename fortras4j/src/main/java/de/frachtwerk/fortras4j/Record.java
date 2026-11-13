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

import de.frachtwerk.fortras4j.schema.FieldDefinition;
import de.frachtwerk.fortras4j.schema.FieldElement;
import de.frachtwerk.fortras4j.schema.FortrasSchema;
import de.frachtwerk.fortras4j.schema.RecordDefinition;
import de.frachtwerk.fortras4j.schema.RepeatDefinition;
import de.frachtwerk.fortras4j.schema.StructureNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One line of a FORTRAS file with its field values.
 *
 * <p>A record read from a file remembers its original line. When written again unchanged, the
 * original line is used verbatim; changed fields are overlaid onto it. Records of an unknown type
 * keep only their raw line.
 */
public final class Record implements Node {

  private final RecordDefinition definition;
  private final String raw;
  private final int lineNumber;
  private final Map<String, Object> values;
  private final Map<String, Object> original;
  private StructureNode.RecordRef structure;

  private Record(
      RecordDefinition definition, String raw, int lineNumber, Map<String, Object> values) {
    this.definition = definition;
    this.raw = raw;
    this.lineNumber = lineNumber;
    this.values = values;
    this.original = raw == null ? Map.of() : snapshot(values);
  }

  static Record parsed(
      RecordDefinition definition, String raw, int lineNumber, Map<String, Object> values) {
    return new Record(definition, raw, lineNumber, values);
  }

  static Record unknown(String raw, int lineNumber) {
    return new Record(null, raw, lineNumber, new LinkedHashMap<>());
  }

  /** A new, empty record; fields with a {@code default} in the layout are prefilled. */
  public static Record create(FortrasSchema schema, String recordId) {
    RecordDefinition definition = schema.record(recordId);
    if (definition == null) {
      throw new IllegalArgumentException(
          "layout '" + schema.id() + "' has no record '" + recordId + "'");
    }
    Map<String, Object> values = new LinkedHashMap<>();
    for (FieldElement element : definition.elements()) {
      values.put(
          element.name(),
          element instanceof FieldDefinition field ? field.defaultValue() : List.of());
    }
    return new Record(definition, null, 0, values);
  }

  /** Record type, e.g. {@code "A00"}; {@code null} for an unknown line. */
  public String id() {
    return definition == null ? null : definition.id();
  }

  public boolean isKnown() {
    return definition != null;
  }

  public RecordDefinition definition() {
    return definition;
  }

  /** The line as read; {@code null} for a record created in code. */
  public String raw() {
    return raw;
  }

  /** 1-based line number in the source file; 0 for a record created in code. */
  public int lineNumber() {
    return lineNumber;
  }

  /** The structure position this record was matched to; {@code null} if it did not fit. */
  public StructureNode.RecordRef structure() {
    return structure;
  }

  void structure(StructureNode.RecordRef structure) {
    this.structure = structure;
  }

  public Map<String, Object> values() {
    return Collections.unmodifiableMap(values);
  }

  public Object get(String field) {
    requireElement(field);
    return values.get(field);
  }

  public String getString(String field) {
    Object value = get(field);
    return value == null ? null : value.toString();
  }

  public Long getLong(String field) {
    return (Long) get(field);
  }

  public Integer getInt(String field) {
    Long value = getLong(field);
    return value == null ? null : Math.toIntExact(value);
  }

  public BigDecimal getDecimal(String field) {
    return (BigDecimal) get(field);
  }

  public LocalDate getDate(String field) {
    return (LocalDate) get(field);
  }

  public LocalTime getTime(String field) {
    return (LocalTime) get(field);
  }

  public Boolean getBoolean(String field) {
    return (Boolean) get(field);
  }

  /** Blocks of a repeated field group, e.g. the text keys of H00. */
  @SuppressWarnings("unchecked")
  public List<Map<String, Object>> getBlocks(String repeat) {
    if (!(requireElement(repeat) instanceof RepeatDefinition)) {
      throw new IllegalArgumentException("'" + repeat + "' is not a repeated block");
    }
    return (List<Map<String, Object>>) values.get(repeat);
  }

  /**
   * Sets a field or, for a repeated block, a list of maps. The value is converted to the field
   * type: an ISO string becomes a {@link LocalDate}, an {@code int} a {@link Long} and so on.
   */
  public Record set(String field, Object value) {
    FieldElement element = requireElement(field);
    try {
      values.put(field, coerce(element, value));
    } catch (IllegalArgumentException | ClassCastException e) {
      throw new IllegalArgumentException(definition.id() + "." + field + ": " + e.getMessage(), e);
    }
    return this;
  }

  /** True if a field differs from the value read from the file. */
  public boolean isModified(String field) {
    return raw == null || !Objects.equals(values.get(field), original.get(field));
  }

  public boolean isModified() {
    return raw == null || !values.equals(original);
  }

  @Override
  public List<Record> flatten() {
    return List.of(this);
  }

  @Override
  public String toString() {
    return (definition == null ? "?" : definition.id())
        + (lineNumber > 0 ? "@" + lineNumber : "")
        + values;
  }

  private FieldElement requireElement(String field) {
    if (definition == null) {
      throw new IllegalStateException("line " + lineNumber + " has no known record type");
    }
    FieldElement element = definition.element(field);
    if (element == null) {
      throw new IllegalArgumentException(
          "record " + definition.id() + " has no field '" + field + "'");
    }
    return element;
  }

  private static Object coerce(FieldElement element, Object value) {
    if (element instanceof FieldDefinition field) {
      return field.codec().coerce(value);
    }
    RepeatDefinition repeat = (RepeatDefinition) element;
    if (value == null) {
      return List.of();
    }
    if (!(value instanceof List<?> blocks)) {
      throw new IllegalArgumentException("expected a list of blocks for '" + repeat.name() + "'");
    }
    if (blocks.size() > repeat.count()) {
      throw new IllegalArgumentException(
          blocks.size() + " blocks given, at most " + repeat.count() + " allowed");
    }
    List<Map<String, Object>> result = new ArrayList<>();
    for (Object block : blocks) {
      if (!(block instanceof Map<?, ?> map)) {
        throw new IllegalArgumentException("a block of '" + repeat.name() + "' must be a map");
      }
      Map<String, Object> converted = new LinkedHashMap<>();
      for (FieldDefinition sub : repeat.fields()) {
        converted.put(sub.name(), sub.codec().coerce(map.get(sub.name())));
      }
      for (Object key : map.keySet()) {
        if (repeat.field(String.valueOf(key)) == null) {
          throw new IllegalArgumentException(
              "block '" + repeat.name() + "' has no field '" + key + "'");
        }
      }
      result.add(Collections.unmodifiableMap(converted));
    }
    return List.copyOf(result);
  }

  private static Map<String, Object> snapshot(Map<String, Object> values) {
    // values are immutable (String, Long, LocalDate, unmodifiable lists of unmodifiable maps)
    return Collections.unmodifiableMap(new LinkedHashMap<>(values));
  }
}

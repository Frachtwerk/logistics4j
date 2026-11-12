package de.frachtwerk.fortras4j.type;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry of field types. Built in: {@code string}, {@code code}, {@code currency}, {@code
 * integer}, {@code decimal}, {@code date}, {@code time}, {@code boolean}.
 */
public final class FieldTypes {

  private final Map<String, FieldType> types = new LinkedHashMap<>();

  private FieldTypes() {}

  public static FieldTypes builtIn() {
    FieldTypes registry = new FieldTypes();
    registry.register(TextCodec.type("string", TextCodec.Kind.STRING));
    registry.register(TextCodec.type("code", TextCodec.Kind.CODE));
    registry.register(TextCodec.type("currency", TextCodec.Kind.CURRENCY));
    registry.register(NumberCodec.integerType());
    registry.register(NumberCodec.decimalType());
    registry.register(TemporalCodec.type(true));
    registry.register(TemporalCodec.type(false));
    registry.register(BooleanCodec.type());
    return registry;
  }

  /** Adds a type; a type with the same name replaces the existing one. */
  public FieldTypes register(FieldType type) {
    types.put(type.name(), type);
    return this;
  }

  public FieldType get(String name) {
    return types.get(name);
  }

  public Set<String> names() {
    return types.keySet();
  }
}

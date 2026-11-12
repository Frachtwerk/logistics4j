package de.frachtwerk.fortras4j.schema;

/** Supplies the currently active layouts; allows swapping them at runtime. */
@FunctionalInterface
public interface SchemaSource {

  FortrasSchemas schemas();
}

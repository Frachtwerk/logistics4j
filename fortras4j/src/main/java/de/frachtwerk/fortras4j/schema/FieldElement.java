package de.frachtwerk.fortras4j.schema;

/** A single field or a block of fields repeated at a fixed stride within one record. */
public sealed interface FieldElement permits FieldDefinition, RepeatDefinition {

  String name();

  /** First position, 1-based and inclusive. */
  int start();

  /** Last position, 1-based and inclusive. */
  int end();
}

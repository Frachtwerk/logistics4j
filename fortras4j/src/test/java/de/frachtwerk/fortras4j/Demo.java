package de.frachtwerk.fortras4j;

import de.frachtwerk.fortras4j.schema.FortrasSchemas;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

final class Demo {

  static final FortrasSchemas SCHEMAS =
      FortrasSchemas.builder().addClasspath("layouts/demo.yaml").build();

  private Demo() {}

  static FortrasSchemas.Builder withLayout(String yaml) {
    return FortrasSchemas.builder()
        .add("test.yaml", new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
  }

  /** A minimal valid layout to which a test appends or replaces parts. */
  static String layout(String records, String structure) {
    return """
    id: t
    detect: "^A"
    records:
    %s
    structure:
    %s
    """
        .formatted(records.indent(2), structure.indent(2));
  }
}

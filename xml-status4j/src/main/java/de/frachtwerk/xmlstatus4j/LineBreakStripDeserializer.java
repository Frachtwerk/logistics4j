package de.frachtwerk.xmlstatus4j;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/** Text values may be wrapped over several lines; they are joined and trimmed. */
final class LineBreakStripDeserializer extends ValueDeserializer<String> {

  @Override
  public String deserialize(JsonParser parser, DeserializationContext context) {
    return parser.getString().replaceAll("[\\n\\r]", "").trim();
  }
}

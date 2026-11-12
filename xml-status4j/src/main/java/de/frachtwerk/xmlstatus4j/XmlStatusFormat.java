package de.frachtwerk.xmlstatus4j;

import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 * Reads and writes XML status files.
 *
 * <p>Reading ignores a {@code DOCTYPE} declaration and removes line breaks inside text values.
 * Writing produces an indented document with an ISO-8859-1 declaration; the caller encodes the
 * lines accordingly.
 */
public final class XmlStatusFormat {

  public static final String DECLARATION = "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>";

  private static final XmlMapper READER =
      XmlMapper.builder()
          .addModule(
              new SimpleModule().addDeserializer(String.class, new LineBreakStripDeserializer()))
          .build();

  private static final XmlMapper WRITER =
      XmlMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

  private XmlStatusFormat() {}

  /**
   * @throws XmlStatusException if the lines are not a well-formed status document
   */
  public static FahrDaten read(List<String> lines) {
    String xml = String.join("\n", lines).replaceAll("(?i)<!DOCTYPE.*?>\n?", "");
    try {
      return READER.readValue(xml, FahrDaten.class);
    } catch (JacksonException e) {
      throw new XmlStatusException("not a valid XML status file", e);
    }
  }

  /**
   * @throws XmlStatusException if the object cannot be serialized
   */
  public static List<String> write(FahrDaten fahrDaten) {
    try {
      String xml = DECLARATION + "\r\n" + WRITER.writeValueAsString(fahrDaten);
      return List.of(xml.split("\\r?\\n"));
    } catch (JacksonException e) {
      throw new XmlStatusException("cannot write XML status file", e);
    }
  }
}

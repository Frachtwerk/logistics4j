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

package de.frachtwerk.xmlstatus4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * How {@link XmlStatusCodec} reads and writes XML status files. Immutable and thread-safe; build it
 * with {@link #builder()} or load it from YAML:
 *
 * <pre>{@code
 * encoding: UTF-8           # charset of the file, default ISO-8859-1
 * declaration: auto         # auto (from the encoding), none, or the declaration itself
 * indent: true              # indented (default) or compact output
 * lineBreaks: strip         # line breaks in text values: strip (default), keep or space
 * doctype: ignore           # DOCTYPE on reading: ignore (default) or reject
 * rootElement: FAR_DATA     # name of the root element
 * elements:                 # element names, keyed by class and property of the model
 *   Fahrt.sendungsdaten: SDG
 * }</pre>
 *
 * <p>All keys are optional; unknown keys and invalid values are errors.
 */
public final class XmlStatusConfig {

  /** How line breaks inside text values are read. */
  public enum LineBreaks {
    /** Remove them and trim the value, as wrapped values are joined. */
    STRIP,
    /** Keep the value as it is. */
    KEEP,
    /** Replace each line break and the blanks around it by one space and trim the value. */
    SPACE
  }

  /** What reading does with a {@code DOCTYPE} declaration. */
  public enum Doctype {
    IGNORE,
    REJECT
  }

  /** The model classes whose properties can be renamed with {@link Builder#element}. */
  static final Map<String, Class<?>> MODEL =
      Stream.of(
              Barcode.class,
              EmpfaengerDaten.class,
              FahrDaten.class,
              FahrerDaten.class,
              Fahrt.class,
              FahrtHeadDaten.class,
              FahrzeugDaten.class,
              HofDaten.class,
              Lademittel.class,
              Nachnahme.class,
              PositionsDaten.class,
              SendungSummen.class,
              SendungsStatus.class,
              Sendungsdaten.class,
              SendungsdatenKopf.class,
              VersenderDaten.class,
              Wechselbruecke.class)
          .collect(
              Collectors.toMap(
                  Class::getSimpleName, Function.identity(), (a, b) -> a, LinkedHashMap::new));

  private static final String DEFAULT_ROOT_ELEMENT = "FAR_DATA";
  private static final Pattern XML_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9._-]*");
  // keys of the YAML form, also used in error messages
  private static final String ENCODING = "encoding";
  private static final String DECLARATION = "declaration";
  private static final String INDENT = "indent";
  private static final String LINE_BREAKS = "lineBreaks";
  private static final String DOCTYPE = "doctype";
  private static final String ROOT_ELEMENT = "rootElement";
  private static final String ELEMENTS = "elements";
  private static final Set<String> KEYS =
      Set.of(ENCODING, DECLARATION, INDENT, LINE_BREAKS, DOCTYPE, ROOT_ELEMENT, ELEMENTS);
  private static final YAMLMapper YAML = YAMLMapper.builder().build();
  private static final XmlStatusConfig DEFAULTS = builder().build();

  private final Charset encoding;
  private final String declaration;
  private final boolean indent;
  private final LineBreaks lineBreaks;
  private final Doctype doctype;
  private final String rootElement;
  private final Map<String, String> elements;

  private XmlStatusConfig(Builder builder) {
    this.encoding = builder.encoding;
    this.declaration = builder.resolveDeclaration();
    this.indent = builder.indent;
    this.lineBreaks = builder.lineBreaks;
    this.doctype = builder.doctype;
    this.rootElement = builder.rootElement;
    this.elements = Map.copyOf(builder.elements);
  }

  /** The behaviour of 0.1.0: ISO-8859-1, indented, line breaks stripped, DOCTYPE ignored. */
  public static XmlStatusConfig defaults() {
    return DEFAULTS;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** A builder starting from this configuration. */
  public Builder toBuilder() {
    Builder builder = new Builder();
    builder.encoding = encoding;
    builder.declaration = declaration;
    builder.declarationMode = DeclarationMode.EXPLICIT;
    if (declaration == null) {
      builder.declarationMode = DeclarationMode.NONE;
    } else if (declaration.equals(automaticDeclaration(encoding))) {
      builder.declarationMode = DeclarationMode.AUTO;
    }
    builder.indent = indent;
    builder.lineBreaks = lineBreaks;
    builder.doctype = doctype;
    builder.rootElement = rootElement;
    builder.elements.putAll(elements);
    return builder;
  }

  /**
   * @throws XmlStatusConfigException if the file is not a valid configuration
   */
  public static XmlStatusConfig load(Path file) {
    try (InputStream in = Files.newInputStream(file)) {
      return load(file.toString(), in);
    } catch (IOException e) {
      throw new UncheckedIOException("cannot read " + file, e);
    }
  }

  /**
   * @throws XmlStatusConfigException if the resource is missing or not a valid configuration
   */
  public static XmlStatusConfig loadClasspath(String resource, ClassLoader loader) {
    try (InputStream in = loader.getResourceAsStream(resource)) {
      if (in == null) {
        throw new XmlStatusConfigException(List.of(resource + ": not found on the classpath"));
      }
      return load(resource, in);
    } catch (IOException e) {
      throw new UncheckedIOException("cannot read " + resource, e);
    }
  }

  /**
   * @param sourceName name used in error messages, e.g. the file name
   * @throws XmlStatusConfigException if the YAML is not a valid configuration
   */
  public static XmlStatusConfig load(String sourceName, InputStream yaml) {
    JsonNode root;
    try {
      root = YAML.readTree(yaml);
    } catch (JacksonException e) {
      throw new XmlStatusConfigException(
          List.of(sourceName + ": not valid YAML: " + e.getOriginalMessage()));
    }
    if (root == null || root.isMissingNode() || root.isNull()) {
      return defaults();
    }
    if (!root.isObject()) {
      throw new XmlStatusConfigException(
          List.of(sourceName + ": a configuration must be a YAML mapping"));
    }
    return new YamlReader(sourceName).read(root);
  }

  /** Charset of the file; {@link XmlStatusCodec} uses it for streams. */
  public Charset encoding() {
    return encoding;
  }

  /** The XML declaration written as first line, or empty if none is written. */
  public Optional<String> declaration() {
    return Optional.ofNullable(declaration);
  }

  public boolean indent() {
    return indent;
  }

  public LineBreaks lineBreaks() {
    return lineBreaks;
  }

  public Doctype doctype() {
    return doctype;
  }

  public String rootElement() {
    return rootElement;
  }

  /** Renamed elements, keyed by {@code <class>.<property>}, e.g. {@code Fahrt.sendungsdaten}. */
  public Map<String, String> elements() {
    return elements;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof XmlStatusConfig other
        && encoding.equals(other.encoding)
        && Objects.equals(declaration, other.declaration)
        && indent == other.indent
        && lineBreaks == other.lineBreaks
        && doctype == other.doctype
        && rootElement.equals(other.rootElement)
        && elements.equals(other.elements);
  }

  @Override
  public int hashCode() {
    return Objects.hash(encoding, declaration, indent, lineBreaks, doctype, rootElement, elements);
  }

  @Override
  public String toString() {
    return "XmlStatusConfig[encoding="
        + encoding
        + ", declaration="
        + declaration
        + ", indent="
        + indent
        + ", lineBreaks="
        + lineBreaks
        + ", doctype="
        + doctype
        + ", rootElement="
        + rootElement
        + ", elements="
        + elements
        + "]";
  }

  /**
   * The declaration for a charset. It names the charset by its canonical Java name, which for
   * common charsets is the IANA name ({@code UTF-8}, {@code IBM850}, {@code windows-1252}). For
   * ISO-8859-1 it keeps the spelling of 0.1.0, {@value XmlStatusFormat#DECLARATION}.
   */
  static String automaticDeclaration(Charset encoding) {
    if (encoding.equals(StandardCharsets.ISO_8859_1)) {
      return XmlStatusFormat.DECLARATION;
    }
    return "<?xml version=\"1.0\" encoding=\"" + encoding.name() + "\"?>";
  }

  private enum DeclarationMode {
    AUTO,
    EXPLICIT,
    NONE
  }

  /** Not thread-safe; the built configuration is. */
  public static final class Builder {
    private Charset encoding = StandardCharsets.ISO_8859_1;
    private DeclarationMode declarationMode = DeclarationMode.AUTO;
    private String declaration;
    private boolean indent = true;
    private LineBreaks lineBreaks = LineBreaks.STRIP;
    private Doctype doctype = Doctype.IGNORE;
    private String rootElement = DEFAULT_ROOT_ELEMENT;
    private final Map<String, String> elements = new LinkedHashMap<>();

    private Builder() {}

    /**
     * Charset of the file. Unless {@link #declaration(String)} or {@link #noDeclaration()} is used,
     * the declaration names this charset, see {@link #automaticDeclaration()}.
     */
    public Builder encoding(Charset encoding) {
      this.encoding = Objects.requireNonNull(encoding, ENCODING);
      return this;
    }

    /** Writes the given declaration, e.g. {@code <?xml version="1.0" encoding="utf-8"?>}. */
    public Builder declaration(String declaration) {
      this.declaration = Objects.requireNonNull(declaration, DECLARATION);
      this.declarationMode = DeclarationMode.EXPLICIT;
      return this;
    }

    /**
     * Derives the declaration from the encoding (default). The encoding is named by its canonical
     * Java name, e.g. {@code UTF-8}, {@code IBM850} or {@code windows-1252}; ISO-8859-1 keeps the
     * declaration of 0.1.0, {@value XmlStatusFormat#DECLARATION}.
     */
    public Builder automaticDeclaration() {
      this.declaration = null;
      this.declarationMode = DeclarationMode.AUTO;
      return this;
    }

    /** Writes no declaration. */
    public Builder noDeclaration() {
      this.declaration = null;
      this.declarationMode = DeclarationMode.NONE;
      return this;
    }

    /** Indented output (default) or the document in one line after the declaration. */
    public Builder indent(boolean indent) {
      this.indent = indent;
      return this;
    }

    public Builder lineBreaks(LineBreaks lineBreaks) {
      this.lineBreaks = Objects.requireNonNull(lineBreaks, LINE_BREAKS);
      return this;
    }

    public Builder doctype(Doctype doctype) {
      this.doctype = Objects.requireNonNull(doctype, DOCTYPE);
      return this;
    }

    public Builder rootElement(String name) {
      this.rootElement = Objects.requireNonNull(name, ROOT_ELEMENT);
      return this;
    }

    /**
     * Renames the element of a property, e.g. {@code element(Fahrt.class, "sendungsdaten", "SDG")}.
     */
    public Builder element(Class<?> type, String property, String name) {
      return element(type.getSimpleName() + "." + property, name);
    }

    /** Renames the element of a property, keyed by {@code <class>.<property>}. */
    public Builder element(String key, String name) {
      elements.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(name, "name"));
      return this;
    }

    /**
     * @throws XmlStatusConfigException if a value is invalid, e.g. an unknown property
     */
    public XmlStatusConfig build() {
      List<String> errors = new ArrayList<>();
      validate(errors, "");
      if (!errors.isEmpty()) {
        throw new XmlStatusConfigException(errors);
      }
      return new XmlStatusConfig(this);
    }

    private void validate(List<String> errors, String prefix) {
      if (declarationMode == DeclarationMode.EXPLICIT && !declaration.startsWith("<?xml")) {
        errors.add(prefix + DECLARATION + ": must start with '<?xml': " + declaration);
      }
      if (!XML_NAME.matcher(rootElement).matches()) {
        errors.add(prefix + ROOT_ELEMENT + ": not an XML name: '" + rootElement + "'");
      }
      elements.forEach(
          (key, name) -> validateElement(errors, prefix + ELEMENTS + "." + key, key, name));
    }

    private static void validateElement(List<String> errors, String path, String key, String name) {
      int dot = key.indexOf('.');
      if (dot < 0) {
        errors.add(path + ": key must be <class>.<property>");
      } else {
        Class<?> type = MODEL.get(key.substring(0, dot));
        String property = key.substring(dot + 1);
        if (type == null) {
          errors.add(path + ": unknown class; known: " + MODEL.keySet());
        } else if (!properties(type).contains(property)) {
          errors.add(path + ": unknown property; known: " + properties(type));
        }
      }
      if (!XML_NAME.matcher(name).matches()) {
        errors.add(path + ": not an XML name: '" + name + "'");
      }
    }

    private String resolveDeclaration() {
      return switch (declarationMode) {
        case AUTO -> XmlStatusConfig.automaticDeclaration(encoding);
        case EXPLICIT -> declaration;
        case NONE -> null;
      };
    }
  }

  /** The properties of a model class that are written as elements, in declaration order. */
  static List<String> properties(Class<?> type) {
    List<String> names = new ArrayList<>();
    for (Field field : type.getDeclaredFields()) {
      if (!Modifier.isStatic(field.getModifiers())
          && field.isAnnotationPresent(JacksonXmlProperty.class)) {
        names.add(field.getName());
      }
    }
    return names;
  }

  /** Reads the YAML form, collecting every error. */
  private static final class YamlReader {
    private final String source;
    private final List<String> errors = new ArrayList<>();
    private final Builder builder = builder();

    private YamlReader(String source) {
      this.source = source;
    }

    private XmlStatusConfig read(JsonNode root) {
      for (String key : root.propertyNames()) {
        if (!KEYS.contains(key)) {
          error(key, "unknown key; allowed: " + KEYS);
        }
      }
      text(root, ENCODING).ifPresent(this::encoding);
      text(root, DECLARATION).ifPresent(this::declaration);
      bool(root, INDENT).ifPresent(builder::indent);
      enumValue(root, LINE_BREAKS, LineBreaks.class).ifPresent(builder::lineBreaks);
      enumValue(root, DOCTYPE, Doctype.class).ifPresent(builder::doctype);
      text(root, ROOT_ELEMENT).ifPresent(builder::rootElement);
      elements(root.get(ELEMENTS));

      List<String> buildErrors = new ArrayList<>();
      builder.validate(buildErrors, source + ": ");
      errors.addAll(buildErrors);
      if (!errors.isEmpty()) {
        throw new XmlStatusConfigException(errors);
      }
      return new XmlStatusConfig(builder);
    }

    private void encoding(String name) {
      try {
        builder.encoding(Charset.forName(name));
      } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
        error(ENCODING, "unsupported charset '" + name + "'");
      }
    }

    private void declaration(String value) {
      switch (value.toLowerCase(Locale.ROOT)) {
        case "auto" -> builder.automaticDeclaration();
        case "none" -> builder.noDeclaration();
        default -> builder.declaration(value);
      }
    }

    private void elements(JsonNode node) {
      if (node == null || node.isNull()) {
        return;
      }
      if (!node.isObject()) {
        error(ELEMENTS, "must be a mapping of <class>.<property> to an element name");
        return;
      }
      for (Map.Entry<String, JsonNode> entry : node.properties()) {
        if (entry.getValue().isString()) {
          builder.element(entry.getKey(), entry.getValue().asString());
        } else {
          error(ELEMENTS + "." + entry.getKey(), "must be an element name");
        }
      }
    }

    private Optional<String> text(JsonNode root, String key) {
      JsonNode node = root.get(key);
      if (node == null || node.isNull()) {
        return Optional.empty();
      }
      if (!node.isString()) {
        error(key, "must be text");
        return Optional.empty();
      }
      return Optional.of(node.asString());
    }

    private Optional<Boolean> bool(JsonNode root, String key) {
      JsonNode node = root.get(key);
      if (node == null || node.isNull()) {
        return Optional.empty();
      }
      if (!node.isBoolean()) {
        error(key, "must be true or false");
        return Optional.empty();
      }
      return Optional.of(node.asBoolean());
    }

    private <E extends Enum<E>> Optional<E> enumValue(JsonNode root, String key, Class<E> type) {
      return text(root, key)
          .flatMap(value -> {
            for (E constant : type.getEnumConstants()) {
              if (constant.name().equalsIgnoreCase(value)) {
                return Optional.of(constant);
              }
            }
            error(
                key,
                "unknown value '"
                    + value
                    + "'; allowed: "
                    + Stream.of(type.getEnumConstants())
                        .map(c -> c.name().toLowerCase(Locale.ROOT))
                        .toList());
            return Optional.empty();
          });
    }

    private void error(String path, String message) {
      errors.add(source + ": " + path + ": " + message);
    }
  }
}

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

package de.frachtwerk.dtd4j;

import de.frachtwerk.dtd4j.ContentModel.Children;
import de.frachtwerk.dtd4j.Particle.Choice;
import de.frachtwerk.dtd4j.Particle.Name;
import de.frachtwerk.dtd4j.Particle.Sequence;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * How {@link XmlCodec} reads, checks, binds and writes documents of a DTD. Immutable and
 * thread-safe; build it with {@link #builder(Dtd, String)} or load it from YAML:
 *
 * <pre>{@code
 * dtd: status.dtd            # one DTD or a list, relative to this file
 * root: FAR_DATA             # the root element
 * encoding: ISO-8859-1       # charset of the file, default UTF-8
 * declaration: auto          # auto (from the encoding), none, or the declaration itself
 * indent: true               # indented (default) or compact output
 * nulls: empty               # null values: empty element (default), nil (xsi:nil) or omit
 * nullObjects: nulls         # null containers: like nulls (default) or omit
 * order: dtd                 # element order when writing objects: dtd (default) or alphabetical
 * lineBreaks: strip          # line breaks in text values: strip (default), keep or space
 * doctype: ignore            # DOCTYPE on reading: ignore (default) or reject
 * validation: warning        # deviations from the DTD: warning (default), error or off
 * properties:                # property of the bound class per element, default the element name
 *   FAHRT: fahrten
 * groups:                    # list property for the repeated group of an element's content model
 *   FAHRT: abschnitte
 * }</pre>
 *
 * <p>All keys but {@code dtd} and {@code root} are optional; unknown keys and invalid values are
 * errors.
 */
public final class XmlLayout {

  /** How line breaks inside text values are read. */
  public enum LineBreaks {
    /** Remove them and trim the value. */
    STRIP,
    /** Keep the value as it is. */
    KEEP,
    /** Replace each line break and the blanks around it by one space and trim the value. */
    SPACE
  }

  /** How writing renders a null value. */
  public enum Nulls {
    /** An empty element, e.g. {@code <FAR_TSBE/>}. */
    EMPTY,
    /** An empty element with {@code xsi:nil="true"}. */
    NIL,
    /** No element. */
    OMIT
  }

  /** How writing renders a null container, i.e. an element with element content. */
  public enum NullObjects {
    /** Like a null value, see {@link Nulls}. */
    NULLS,
    /** No element, also when {@link Nulls} writes null values. */
    OMIT
  }

  /** The order in which writing puts the elements of an object. */
  public enum Order {
    /** The order of the content model. */
    DTD,
    /** Sorted by element name within each element and each group occurrence. */
    ALPHABETICAL
  }

  /** What reading does with a {@code DOCTYPE} declaration; it is never processed. */
  public enum Doctype {
    IGNORE,
    REJECT
  }

  /** How deviations from the DTD are reported. */
  public enum Validation {
    WARNING,
    ERROR,
    OFF
  }

  // keys of the YAML form, also used in error messages
  private static final String KEY_DTD = "dtd";
  private static final String KEY_ROOT = "root";
  private static final String KEY_ENCODING = "encoding";
  private static final String KEY_DECLARATION = "declaration";
  private static final String KEY_INDENT = "indent";
  private static final String KEY_NULLS = "nulls";
  private static final String KEY_NULL_OBJECTS = "nullObjects";
  private static final String KEY_ORDER = "order";
  private static final String KEY_LINE_BREAKS = "lineBreaks";
  private static final String KEY_DOCTYPE = "doctype";
  private static final String KEY_VALIDATION = "validation";
  private static final String KEY_PROPERTIES = "properties";
  private static final String KEY_GROUPS = "groups";
  private static final Set<String> KEYS =
      Set.of(
          KEY_DTD,
          KEY_ROOT,
          KEY_ENCODING,
          KEY_DECLARATION,
          KEY_INDENT,
          KEY_NULLS,
          KEY_NULL_OBJECTS,
          KEY_ORDER,
          KEY_LINE_BREAKS,
          KEY_DOCTYPE,
          KEY_VALIDATION,
          KEY_PROPERTIES,
          KEY_GROUPS);
  private static final YAMLMapper YAML = YAMLMapper.builder().build();

  private final Dtd dtd;
  private final String root;
  private final Charset encoding;
  private final String declaration;
  private final boolean indent;
  private final Nulls nulls;
  private final NullObjects nullObjects;
  private final Order order;
  private final LineBreaks lineBreaks;
  private final Doctype doctype;
  private final Validation validation;
  private final Map<String, String> properties;
  private final Map<String, String> groups;
  // the repeated group of each element named in groups
  private final Map<String, Particle> groupParticles;

  private XmlLayout(Builder builder, Map<String, Particle> groupParticles) {
    this.dtd = builder.dtd;
    this.root = builder.root;
    this.encoding = builder.encoding;
    this.declaration = builder.resolveDeclaration();
    this.indent = builder.indent;
    this.nulls = builder.nulls;
    this.nullObjects = builder.nullObjects;
    this.order = builder.order;
    this.lineBreaks = builder.lineBreaks;
    this.doctype = builder.doctype;
    this.validation = builder.validation;
    this.properties = Map.copyOf(builder.properties);
    this.groups = Map.copyOf(builder.groups);
    this.groupParticles = Map.copyOf(groupParticles);
  }

  public static Builder builder(Dtd dtd, String root) {
    return new Builder(dtd, root);
  }

  /**
   * @throws XmlLayoutException if the file or a DTD is not valid
   */
  public static XmlLayout load(Path file) {
    try (InputStream in = Files.newInputStream(file)) {
      Path dir = file.toAbsolutePath().getParent();
      return load(file.toString(), in, name -> Dtd.load(dir.resolve(name)));
    } catch (IOException e) {
      throw new UncheckedIOException("cannot read " + file, e);
    }
  }

  /**
   * Loads a layout from the classpath; DTDs are resolved relative to its folder.
   *
   * @throws XmlLayoutException if the resource is missing or not valid
   */
  public static XmlLayout loadClasspath(String resource, ClassLoader loader) {
    int slash = resource.lastIndexOf('/');
    String folder = slash < 0 ? "" : resource.substring(0, slash + 1);
    try (InputStream in = loader.getResourceAsStream(resource)) {
      if (in == null) {
        throw new XmlLayoutException(List.of(resource + ": not found on the classpath"));
      }
      return load(resource, in, name -> Dtd.loadClasspath(folder + name, loader));
    } catch (IOException e) {
      throw new UncheckedIOException("cannot read " + resource, e);
    }
  }

  /**
   * @param sourceName name used in error messages, e.g. the file name
   * @param dtds loads a DTD named in the layout
   * @throws XmlLayoutException if the YAML is not a valid layout
   */
  public static XmlLayout load(String sourceName, InputStream yaml, Function<String, Dtd> dtds) {
    JsonNode node;
    try {
      node = YAML.readTree(yaml);
    } catch (JacksonException e) {
      throw new XmlLayoutException(
          List.of(sourceName + ": not valid YAML: " + e.getOriginalMessage()));
    }
    if (node == null || !node.isObject()) {
      throw new XmlLayoutException(List.of(sourceName + ": a layout must be a YAML mapping"));
    }
    return new YamlReader(sourceName, dtds).read(node);
  }

  public Dtd dtd() {
    return dtd;
  }

  public String root() {
    return root;
  }

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

  public Nulls nulls() {
    return nulls;
  }

  public NullObjects nullObjects() {
    return nullObjects;
  }

  public Order order() {
    return order;
  }

  public LineBreaks lineBreaks() {
    return lineBreaks;
  }

  public Doctype doctype() {
    return doctype;
  }

  public Validation validation() {
    return validation;
  }

  /** Property names by element name; elements not listed use their own name. */
  public Map<String, String> properties() {
    return properties;
  }

  /** Names of the list properties for repeated groups, by element name. */
  public Map<String, String> groups() {
    return groups;
  }

  /** The property an element is bound to. */
  public String property(String element) {
    return properties.getOrDefault(element, element);
  }

  /** The repeated group of the element's content model that {@link #groups()} names. */
  Optional<Particle> groupParticle(String element) {
    return Optional.ofNullable(groupParticles.get(element));
  }

  @Override
  public String toString() {
    return "XmlLayout[root="
        + root
        + ", encoding="
        + encoding
        + ", nulls="
        + nulls
        + ", nullObjects="
        + nullObjects
        + ", order="
        + order
        + ", validation="
        + validation
        + "]";
  }

  /** The declaration for a charset; ISO-8859-1 is written as {@code iso-8859-1}. */
  static String automaticDeclaration(Charset encoding) {
    String name = encoding.equals(StandardCharsets.ISO_8859_1) ? "iso-8859-1" : encoding.name();
    return "<?xml version=\"1.0\" encoding=\"" + name + "\"?>";
  }

  private enum DeclarationMode {
    AUTO,
    EXPLICIT,
    NONE
  }

  /** Not thread-safe; the built layout is. */
  public static final class Builder {
    private final Dtd dtd;
    private String root;
    private Charset encoding = StandardCharsets.UTF_8;
    private DeclarationMode declarationMode = DeclarationMode.AUTO;
    private String declaration;
    private boolean indent = true;
    private Nulls nulls = Nulls.EMPTY;
    private NullObjects nullObjects = NullObjects.NULLS;
    private Order order = Order.DTD;
    private LineBreaks lineBreaks = LineBreaks.STRIP;
    private Doctype doctype = Doctype.IGNORE;
    private Validation validation = Validation.WARNING;
    private final Map<String, String> properties = new LinkedHashMap<>();
    private final Map<String, String> groups = new LinkedHashMap<>();

    private Builder(Dtd dtd, String root) {
      this.dtd = Objects.requireNonNull(dtd, KEY_DTD);
      this.root = Objects.requireNonNull(root, KEY_ROOT);
    }

    public Builder root(String root) {
      this.root = Objects.requireNonNull(root, KEY_ROOT);
      return this;
    }

    /** Charset of the file, named in the declaration unless another one is configured. */
    public Builder encoding(Charset encoding) {
      this.encoding = Objects.requireNonNull(encoding, KEY_ENCODING);
      return this;
    }

    /** Writes the given declaration, e.g. {@code <?xml version="1.0"?>}. */
    public Builder declaration(String declaration) {
      this.declaration = Objects.requireNonNull(declaration, KEY_DECLARATION);
      this.declarationMode = DeclarationMode.EXPLICIT;
      return this;
    }

    /** Derives the declaration from the encoding (default). */
    public Builder automaticDeclaration() {
      this.declaration = null;
      this.declarationMode = DeclarationMode.AUTO;
      return this;
    }

    public Builder noDeclaration() {
      this.declaration = null;
      this.declarationMode = DeclarationMode.NONE;
      return this;
    }

    public Builder indent(boolean indent) {
      this.indent = indent;
      return this;
    }

    public Builder nulls(Nulls nulls) {
      this.nulls = Objects.requireNonNull(nulls, KEY_NULLS);
      return this;
    }

    public Builder nullObjects(NullObjects nullObjects) {
      this.nullObjects = Objects.requireNonNull(nullObjects, KEY_NULL_OBJECTS);
      return this;
    }

    public Builder order(Order order) {
      this.order = Objects.requireNonNull(order, KEY_ORDER);
      return this;
    }

    public Builder lineBreaks(LineBreaks lineBreaks) {
      this.lineBreaks = Objects.requireNonNull(lineBreaks, KEY_LINE_BREAKS);
      return this;
    }

    public Builder doctype(Doctype doctype) {
      this.doctype = Objects.requireNonNull(doctype, KEY_DOCTYPE);
      return this;
    }

    public Builder validation(Validation validation) {
      this.validation = Objects.requireNonNull(validation, KEY_VALIDATION);
      return this;
    }

    /** Binds the element to the property, e.g. {@code property("FAR_FAHR", "rollkartenNr")}. */
    public Builder property(String element, String property) {
      properties.put(
          Objects.requireNonNull(element, "element"), Objects.requireNonNull(property, "property"));
      return this;
    }

    /**
     * Binds each occurrence of the repeated group in the element's content model to an object in
     * the list property, e.g. {@code group("FAHRT", "abschnitte")} for {@code (FAR?, SDG+)+}.
     */
    public Builder group(String element, String property) {
      groups.put(
          Objects.requireNonNull(element, "element"), Objects.requireNonNull(property, "property"));
      return this;
    }

    /**
     * @throws XmlLayoutException if a value is invalid, e.g. an undeclared element
     */
    public XmlLayout build() {
      List<String> errors = new ArrayList<>();
      Map<String, Particle> groupParticles = validate(errors, "");
      if (!errors.isEmpty()) {
        throw new XmlLayoutException(errors);
      }
      return new XmlLayout(this, groupParticles);
    }

    private Map<String, Particle> validate(List<String> errors, String prefix) {
      if (dtd.element(root).isEmpty()) {
        errors.add(prefix + KEY_ROOT + ": element " + root + " is not declared");
      }
      if (declarationMode == DeclarationMode.EXPLICIT && !declaration.startsWith("<?xml")) {
        errors.add(prefix + KEY_DECLARATION + ": must start with '<?xml': " + declaration);
      }
      properties
          .keySet()
          .forEach(element -> {
            if (dtd.element(element).isEmpty()) {
              errors.add(prefix + KEY_PROPERTIES + "." + element + ": undeclared element");
            }
          });
      Map<String, Particle> groupParticles = new HashMap<>();
      groups.forEach((element, property) -> {
        String path = prefix + KEY_GROUPS + "." + element;
        Optional<ElementDeclaration> declaration = dtd.element(element);
        if (declaration.isEmpty()) {
          errors.add(path + ": undeclared element");
        } else if (!(declaration.get().content() instanceof Children children)) {
          errors.add(path + ": element " + element + " has no element content");
        } else {
          Particle group = repeatedGroup(children.particle());
          if (group == null) {
            errors.add(path + ": the content model " + children + " has no repeated group");
          } else {
            groupParticles.put(element, group);
          }
        }
      });
      return groupParticles;
    }

    /** The outermost sequence or choice that may occur more than once. */
    private static Particle repeatedGroup(Particle particle) {
      if (particle instanceof Name) {
        return null;
      }
      if (particle.occurs().repeatable()) {
        return particle;
      }
      List<Particle> parts =
          switch (particle) {
            case Sequence sequence -> sequence.items();
            case Choice choice -> choice.options();
            case Name name -> List.of();
          };
      for (Particle part : parts) {
        Particle found = repeatedGroup(part);
        if (found != null) {
          return found;
        }
      }
      return null;
    }

    private String resolveDeclaration() {
      return switch (declarationMode) {
        case AUTO -> XmlLayout.automaticDeclaration(encoding);
        case EXPLICIT -> declaration;
        case NONE -> null;
      };
    }
  }

  /** Reads the YAML form, collecting every error. */
  private static final class YamlReader {
    private final String source;
    private final Function<String, Dtd> dtds;
    private final List<String> errors = new ArrayList<>();

    private YamlReader(String source, Function<String, Dtd> dtds) {
      this.source = source;
      this.dtds = dtds;
    }

    private XmlLayout read(JsonNode node) {
      for (String key : node.propertyNames()) {
        if (!KEYS.contains(key)) {
          error(key, "unknown key; allowed: " + KEYS);
        }
      }
      Dtd dtd = dtd(node.get(KEY_DTD));
      Optional<String> root = text(node, KEY_ROOT);
      if (root.isEmpty()) {
        error(KEY_ROOT, "the root element is required");
      }
      if (dtd == null || root.isEmpty()) {
        throw new XmlLayoutException(errors);
      }
      Builder builder = builder(dtd, root.get());
      text(node, KEY_ENCODING).ifPresent(name -> encoding(builder, name));
      text(node, KEY_DECLARATION)
          .ifPresent(value -> {
            switch (value.toLowerCase(Locale.ROOT)) {
              case "auto" -> builder.automaticDeclaration();
              case "none" -> builder.noDeclaration();
              default -> builder.declaration(value);
            }
          });
      bool(node, KEY_INDENT).ifPresent(builder::indent);
      enumValue(node, KEY_NULLS, Nulls.class).ifPresent(builder::nulls);
      enumValue(node, KEY_NULL_OBJECTS, NullObjects.class).ifPresent(builder::nullObjects);
      enumValue(node, KEY_ORDER, Order.class).ifPresent(builder::order);
      enumValue(node, KEY_LINE_BREAKS, LineBreaks.class).ifPresent(builder::lineBreaks);
      enumValue(node, KEY_DOCTYPE, Doctype.class).ifPresent(builder::doctype);
      enumValue(node, KEY_VALIDATION, Validation.class).ifPresent(builder::validation);
      names(node.get(KEY_PROPERTIES), KEY_PROPERTIES, builder::property);
      names(node.get(KEY_GROUPS), KEY_GROUPS, builder::group);

      Map<String, Particle> groupParticles = builder.validate(errors, source + ": ");
      if (!errors.isEmpty()) {
        throw new XmlLayoutException(errors);
      }
      return new XmlLayout(builder, groupParticles);
    }

    private Dtd dtd(JsonNode node) {
      List<String> names = new ArrayList<>();
      if (node != null && node.isString()) {
        names.add(node.asString());
      } else if (node != null && node.isArray() && !node.isEmpty()) {
        for (JsonNode element : node) {
          if (element.isString()) {
            names.add(element.asString());
          } else {
            error(KEY_DTD, "must be a file name or a list of file names");
            return null;
          }
        }
      } else {
        error(KEY_DTD, "one DTD or a list of DTDs is required");
        return null;
      }
      try {
        List<Dtd> loaded = names.stream().map(dtds).toList();
        return loaded.size() == 1 ? loaded.getFirst() : Dtd.of(loaded);
      } catch (DtdException e) {
        e.errors().forEach(message -> error(KEY_DTD, message));
        return null;
      } catch (UncheckedIOException e) {
        error(KEY_DTD, e.getMessage());
        return null;
      }
    }

    private void encoding(Builder builder, String name) {
      try {
        builder.encoding(Charset.forName(name));
      } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
        error(KEY_ENCODING, "unsupported charset '" + name + "'");
      }
    }

    private void names(JsonNode node, String key, BiConsumer<String, String> add) {
      if (node == null || node.isNull()) {
        return;
      }
      if (!node.isObject()) {
        error(key, "must be a mapping of element names to property names");
        return;
      }
      for (Map.Entry<String, JsonNode> entry : node.properties()) {
        if (entry.getValue().isString()) {
          add.accept(entry.getKey(), entry.getValue().asString());
        } else {
          error(key + "." + entry.getKey(), "must be a property name");
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

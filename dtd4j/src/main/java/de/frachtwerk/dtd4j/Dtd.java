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
import de.frachtwerk.dtd4j.ContentModel.Mixed;
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The element declarations of one or more DTDs. Immutable and thread-safe.
 *
 * <pre>{@code
 * Dtd dtd = Dtd.load(Path.of("status.dtd"));
 * dtd.element("FAHRT").orElseThrow().content(); // (FAR?, SDG+)+
 * }</pre>
 */
public final class Dtd {

  private static final Pattern TEXT_DECLARATION =
      Pattern.compile("^\\s*<\\?xml[^>]*encoding\\s*=\\s*[\"']([A-Za-z0-9._-]+)[\"']");

  private final Map<String, ElementDeclaration> elements;

  private Dtd(Map<String, ElementDeclaration> elements) {
    this.elements = elements;
  }

  /**
   * @param source name used in error messages, e.g. the file name
   * @throws DtdException if the text is not a valid DTD or uses unsupported declarations
   */
  public static Dtd parse(String source, String text) {
    List<String> errors = new ArrayList<>();
    Map<String, ElementDeclaration> elements = DtdParser.parse(source, text, errors);
    return build(elements, errors);
  }

  /**
   * Reads a DTD in the charset of its text declaration ({@code <?xml ... encoding="..."?>}), UTF-8
   * without one.
   *
   * @throws DtdException if the file is not a valid DTD
   */
  public static Dtd load(Path file) {
    try {
      return parse(file.toString(), decode(file.toString(), Files.readAllBytes(file)));
    } catch (IOException e) {
      throw new UncheckedIOException("cannot read " + file, e);
    }
  }

  /**
   * @throws DtdException if the resource is missing or not a valid DTD
   */
  public static Dtd loadClasspath(String resource, ClassLoader loader) {
    try (InputStream in = loader.getResourceAsStream(resource)) {
      if (in == null) {
        throw new DtdException(List.of(resource + ": not found on the classpath"));
      }
      return parse(resource, decode(resource, in.readAllBytes()));
    } catch (IOException e) {
      throw new UncheckedIOException("cannot read " + resource, e);
    }
  }

  /**
   * The declarations of several DTDs together, e.g. a base DTD and one with additional elements.
   *
   * @throws DtdException if an element is declared more than once
   */
  public static Dtd of(Collection<Dtd> dtds) {
    Map<String, ElementDeclaration> elements = new LinkedHashMap<>();
    List<String> errors = new ArrayList<>();
    for (Dtd dtd : dtds) {
      for (ElementDeclaration declaration : dtd.elements.values()) {
        if (elements.putIfAbsent(declaration.name(), declaration) != null) {
          errors.add("element " + declaration.name() + " is declared in more than one DTD");
        }
      }
    }
    return build(elements, errors);
  }

  private static Dtd build(Map<String, ElementDeclaration> elements, List<String> errors) {
    for (ElementDeclaration declaration : elements.values()) {
      for (String name : referencedNames(declaration.content())) {
        if (!elements.containsKey(name)) {
          errors.add(declaration.name() + ": undeclared element " + name);
        }
      }
    }
    if (!errors.isEmpty()) {
      throw new DtdException(errors);
    }
    return new Dtd(Map.copyOf(elements));
  }

  private static List<String> referencedNames(ContentModel content) {
    List<String> names = new ArrayList<>();
    switch (content) {
      case Children children -> collect(children.particle(), names);
      case Mixed mixed -> names.addAll(mixed.names());
      default -> {
        // no element names
      }
    }
    return names;
  }

  private static void collect(Particle particle, List<String> into) {
    switch (particle) {
      case Name name -> into.add(name.name());
      case Sequence sequence -> sequence.items().forEach(item -> collect(item, into));
      case Choice choice -> choice.options().forEach(option -> collect(option, into));
    }
  }

  private static String decode(String source, byte[] bytes) {
    // the text declaration is ASCII in every charset a DTD may use here
    String head = new String(bytes, 0, Math.min(bytes.length, 200), StandardCharsets.ISO_8859_1);
    Matcher matcher = TEXT_DECLARATION.matcher(head);
    if (!matcher.find()) {
      return new String(bytes, StandardCharsets.UTF_8);
    }
    try {
      return new String(bytes, Charset.forName(matcher.group(1)));
    } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
      throw new DtdException(List.of(source + ": unsupported charset '" + matcher.group(1) + "'"));
    }
  }

  public Optional<ElementDeclaration> element(String name) {
    return Optional.ofNullable(elements.get(name));
  }

  /** All declarations by element name. */
  public Map<String, ElementDeclaration> elements() {
    return elements;
  }

  @Override
  public String toString() {
    return "Dtd" + elements.keySet();
  }
}

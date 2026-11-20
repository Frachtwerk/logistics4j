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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads, checks, binds and writes XML documents of a {@link XmlLayout}. Immutable and thread-safe.
 *
 * <pre>{@code
 * XmlCodec codec = XmlCodec.of(XmlLayout.loadClasspath("xml/status.yaml", loader));
 * XmlDocument document = codec.read(lines);        // elements in document order and problems
 * Status status = codec.read(lines, Status.class); // bound to own classes
 * List<String> out = codec.write(status);          // in the order of the DTD
 * }</pre>
 *
 * <p>Reading never throws for content that does not match the DTD; it is reported as {@link
 * Problem} with the severity of {@link XmlLayout#validation()} and kept.
 */
public final class XmlCodec {

  private final XmlLayout layout;
  private final JsonMapper mapper;
  private final Binder binder;

  private XmlCodec(XmlLayout layout, JsonMapper mapper) {
    this.layout = layout;
    this.mapper = mapper;
    this.binder = new Binder(layout);
  }

  public static XmlCodec of(XmlLayout layout) {
    // a class may hold a list where the DTD allows one element, e.g. for (FAHRT)
    JsonMapper mapper =
        JsonMapper.builder().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY).build();
    return new XmlCodec(Objects.requireNonNull(layout, "layout"), mapper);
  }

  /**
   * A copy that binds with the given mapper, e.g. one with modules for own types. The default
   * mapper accepts a single value for a list.
   */
  public XmlCodec withMapper(JsonMapper mapper) {
    return new XmlCodec(layout, Objects.requireNonNull(mapper, "mapper"));
  }

  public XmlLayout layout() {
    return layout;
  }

  /**
   * @throws Dtd4jException if the lines are not well-formed XML or have a rejected DOCTYPE
   */
  public XmlDocument read(List<String> lines) {
    XmlElement root = XmlParser.parse(String.join("\n", lines), layout);
    return new XmlDocument(root, Validator.validate(root, layout));
  }

  /**
   * Reads a file in the layout's {@linkplain XmlLayout#encoding() encoding}; the stream is not
   * closed.
   *
   * @throws Dtd4jException if the bytes are not valid in the encoding or not well-formed XML
   */
  public XmlDocument read(InputStream in) {
    var decoder =
        layout
            .encoding()
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
    List<String> lines = new ArrayList<>();
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(in, decoder));
      for (String line = reader.readLine(); line != null; line = reader.readLine()) {
        lines.add(line);
      }
    } catch (CharacterCodingException e) {
      throw new Dtd4jException("not valid " + layout.encoding().name(), e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return read(lines);
  }

  /**
   * Reads and binds the document.
   *
   * @throws Dtd4jException if the document cannot be read, has errors or cannot be bound
   */
  public <T> T read(List<String> lines, Class<T> type) {
    XmlDocument document = read(lines);
    failOnErrors(document, "read");
    return bind(document, type);
  }

  /**
   * The root element bound to the type; problems of the document are not checked.
   *
   * @throws Dtd4jException if the tree cannot be bound to the type
   */
  public <T> T bind(XmlDocument document, Class<T> type) {
    JsonNode tree = binder.toTree(document.root());
    try {
      return mapper.treeToValue(tree, type);
    } catch (JacksonException e) {
      throw new Dtd4jException(
          "cannot bind " + document.root().name() + " to " + type.getName(), e);
    }
  }

  /**
   * The document for an object bound to the root element, with the problems of writing and of the
   * DTD.
   *
   * @throws Dtd4jException if the object cannot be converted
   */
  public XmlDocument toDocument(Object value) {
    JsonNode tree;
    try {
      tree = mapper.valueToTree(value);
    } catch (JacksonException e) {
      throw new Dtd4jException("cannot convert " + value.getClass().getName(), e);
    }
    List<Problem> problems = new ArrayList<>();
    XmlElement root = binder.toElement(layout.root(), tree, layout.root(), problems);
    problems.addAll(Validator.validate(root, layout));
    return new XmlDocument(root, problems);
  }

  /** The declaration, if any, and the document as lines, in document order. */
  public List<String> write(XmlDocument document) {
    return XmlFormatter.format(document.root(), layout);
  }

  /**
   * The declaration, if any, and the object as lines; the caller encodes them.
   *
   * @throws Dtd4jException if the object cannot be converted or the document has errors
   */
  public List<String> write(Object value) {
    XmlDocument document = toDocument(value);
    failOnErrors(document, "write");
    return write(document);
  }

  /**
   * Writes the lines of {@link #write(Object)} in the layout's encoding, each ended by CR LF; the
   * stream is not closed.
   *
   * @throws Dtd4jException if the object cannot be written or a character cannot be encoded
   */
  public void write(Object value, OutputStream out) {
    List<String> lines = value instanceof XmlDocument d ? write(d) : write(value);
    String text = String.join("\r\n", lines) + "\r\n";
    try {
      ByteBuffer bytes =
          layout
              .encoding()
              .newEncoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT)
              .encode(CharBuffer.wrap(text));
      out.write(bytes.array(), bytes.arrayOffset() + bytes.position(), bytes.remaining());
    } catch (CharacterCodingException e) {
      throw new Dtd4jException("cannot encode in " + layout.encoding().name(), e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void failOnErrors(XmlDocument document, String action) {
    if (!document.isValid()) {
      throw new Dtd4jException(
          "cannot "
              + action
              + " "
              + document.root().name()
              + ":\n  - "
              + document.errors().stream()
                  .map(Problem::toString)
                  .collect(Collectors.joining("\n  - ")));
    }
  }
}

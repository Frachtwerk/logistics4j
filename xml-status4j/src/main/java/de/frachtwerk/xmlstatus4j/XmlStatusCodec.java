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
import java.util.regex.Pattern;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 * Reads and writes XML status files with a {@link XmlStatusConfig}. Immutable and thread-safe; one
 * instance per configuration can be shared:
 *
 * <pre>{@code
 * XmlStatusCodec utf8 =
 *     XmlStatusCodec.of(XmlStatusConfig.builder().encoding(StandardCharsets.UTF_8).build());
 * List<String> lines = utf8.write(fahrDaten); // first line: <?xml version="1.0" encoding="UTF-8"?>
 * }</pre>
 *
 * <p>{@link XmlStatusFormat} offers the same with the {@linkplain XmlStatusConfig#defaults()
 * defaults}.
 */
public final class XmlStatusCodec {

  private static final Pattern DOCTYPE = Pattern.compile("(?i)<!DOCTYPE.*?>\n?");
  private static final XmlStatusCodec DEFAULT = new XmlStatusCodec(XmlStatusConfig.defaults());

  private final XmlStatusConfig config;
  private final XmlMapper reader;
  private final XmlMapper writer;

  private XmlStatusCodec(XmlStatusConfig config) {
    this.config = config;
    ElementNameIntrospector names =
        new ElementNameIntrospector(config.elements(), config.rootElement());

    XmlMapper.Builder readerBuilder = XmlMapper.builder().annotationIntrospector(names);
    switch (config.lineBreaks()) {
      case STRIP ->
          readerBuilder.addModule(
              new SimpleModule().addDeserializer(String.class, new LineBreakDeserializer()));
      case SPACE ->
          readerBuilder.addModule(
              new SimpleModule().addDeserializer(String.class, new LineBreakDeserializer(true)));
      case KEEP -> {
        // Jackson keeps text values as they are
      }
    }
    this.reader = readerBuilder.build();

    XmlMapper.Builder writerBuilder = XmlMapper.builder().annotationIntrospector(names);
    if (config.indent()) {
      writerBuilder.enable(SerializationFeature.INDENT_OUTPUT);
    }
    this.writer = writerBuilder.build();
  }

  /** With the {@linkplain XmlStatusConfig#defaults() defaults}, the behaviour of 0.1.0. */
  public static XmlStatusCodec create() {
    return DEFAULT;
  }

  public static XmlStatusCodec of(XmlStatusConfig config) {
    Objects.requireNonNull(config, "config");
    return config.equals(XmlStatusConfig.defaults()) ? DEFAULT : new XmlStatusCodec(config);
  }

  public XmlStatusConfig config() {
    return config;
  }

  /**
   * @throws XmlStatusException if the lines are not a well-formed status document, or contain a
   *     {@code DOCTYPE} and the configuration rejects it
   */
  public FahrDaten read(List<String> lines) {
    String xml = String.join("\n", lines);
    if (DOCTYPE.matcher(xml).find()) {
      if (config.doctype() == XmlStatusConfig.Doctype.REJECT) {
        throw new XmlStatusException("DOCTYPE declarations are not allowed");
      }
      xml = DOCTYPE.matcher(xml).replaceAll("");
    }
    try {
      return reader.readValue(xml, FahrDaten.class);
    } catch (JacksonException e) {
      throw new XmlStatusException("not a valid XML status file", e);
    }
  }

  /**
   * Reads a file in the configured {@linkplain XmlStatusConfig#encoding() encoding}; the stream is
   * not closed.
   *
   * @throws XmlStatusException as {@link #read(List)}, or if the bytes are not valid in the
   *     encoding
   */
  public FahrDaten read(InputStream in) {
    var decoder =
        config
            .encoding()
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
    List<String> lines = new ArrayList<>();
    try {
      BufferedReader lineReader = new BufferedReader(new InputStreamReader(in, decoder));
      for (String line = lineReader.readLine(); line != null; line = lineReader.readLine()) {
        lines.add(line);
      }
    } catch (CharacterCodingException e) {
      throw new XmlStatusException("not valid " + config.encoding().name(), e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return read(lines);
  }

  /**
   * The declaration, if any, and the document as lines; the caller encodes them in the configured
   * encoding.
   *
   * @throws XmlStatusException if the object cannot be serialized
   */
  public List<String> write(FahrDaten fahrDaten) {
    try {
      String body = writer.writeValueAsString(fahrDaten);
      String xml = config.declaration().map(d -> d + "\r\n" + body).orElse(body);
      return List.of(xml.split("\\r?\\n"));
    } catch (JacksonException e) {
      throw new XmlStatusException("cannot write XML status file", e);
    }
  }

  /**
   * Writes the lines of {@link #write(FahrDaten)} in the configured {@linkplain
   * XmlStatusConfig#encoding() encoding}, each ended by CR LF; the stream is not closed.
   *
   * @throws XmlStatusException if the object cannot be serialized or a character cannot be encoded
   */
  public void write(FahrDaten fahrDaten, OutputStream out) {
    String text = String.join("\r\n", write(fahrDaten)) + "\r\n";
    try {
      ByteBuffer bytes =
          config
              .encoding()
              .newEncoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT)
              .encode(CharBuffer.wrap(text));
      out.write(bytes.array(), bytes.arrayOffset() + bytes.position(), bytes.remaining());
    } catch (CharacterCodingException e) {
      throw new XmlStatusException("cannot encode in " + config.encoding().name(), e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}

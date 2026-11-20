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

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Reads XML into {@link XmlElement}s with the StAX parser of the JDK. A DOCTYPE is never processed
 * and external entities are never resolved.
 */
final class XmlParser {

  private static final XMLInputFactory FACTORY = factory();
  private static final Pattern DOCTYPE_START = Pattern.compile("(?i)<!DOCTYPE");
  // also over several lines, with an internal subset, and with names XML does not allow
  private static final Pattern DOCTYPE =
      Pattern.compile("(?is)<!DOCTYPE[^\\[>]*(?:\\[.*?]\\s*)?>\n?");

  private XmlParser() {}

  private static XMLInputFactory factory() {
    XMLInputFactory factory = XMLInputFactory.newDefaultFactory();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    factory.setProperty(XMLInputFactory.IS_COALESCING, true);
    factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
    return factory;
  }

  /**
   * @throws Dtd4jException if the text is not well-formed or has a DOCTYPE the layout rejects
   */
  static XmlElement parse(String xml, XmlLayout layout) {
    if (DOCTYPE_START.matcher(xml).find()) {
      if (layout.doctype() == XmlLayout.Doctype.REJECT) {
        throw new Dtd4jException("DOCTYPE declarations are not allowed");
      }
      // removed before parsing, so a malformed DOCTYPE does not fail the document
      xml = DOCTYPE.matcher(xml).replaceAll("");
    }
    XMLStreamReader reader;
    try {
      synchronized (FACTORY) {
        reader = FACTORY.createXMLStreamReader(new StringReader(xml));
      }
    } catch (XMLStreamException e) {
      throw new Dtd4jException("not a well-formed XML document: " + e.getMessage(), e);
    }
    Deque<XmlElement> open = new ArrayDeque<>();
    Deque<StringBuilder> texts = new ArrayDeque<>();
    XmlElement root = null;
    try {
      while (reader.hasNext()) {
        switch (reader.next()) {
          case XMLStreamConstants.START_ELEMENT -> {
            XmlElement element = new XmlElement(name(reader)).nil(isNil(reader));
            if (open.isEmpty()) {
              root = element;
            } else {
              open.peek().add(element);
            }
            open.push(element);
            texts.push(new StringBuilder());
          }
          case XMLStreamConstants.CHARACTERS,
              XMLStreamConstants.CDATA,
              XMLStreamConstants.SPACE -> {
            if (!texts.isEmpty()) {
              texts.peek().append(reader.getText());
            }
          }
          case XMLStreamConstants.END_ELEMENT -> {
            XmlElement element = open.pop();
            String text = texts.pop().toString();
            if (!element.children().isEmpty()) {
              element.text(text.isBlank() ? null : text);
            } else {
              element.text(lineBreaks(text, layout.lineBreaks()));
            }
          }
          default -> {
            // comments and processing instructions are not kept
          }
        }
      }
      reader.close();
    } catch (XMLStreamException e) {
      throw new Dtd4jException("not a well-formed XML document: " + e.getMessage(), e);
    }
    if (root == null) {
      throw new Dtd4jException("not a well-formed XML document: no root element");
    }
    return root;
  }

  private static String name(XMLStreamReader reader) {
    String prefix = reader.getPrefix();
    return prefix == null || prefix.isEmpty()
        ? reader.getLocalName()
        : prefix + ":" + reader.getLocalName();
  }

  private static boolean isNil(XMLStreamReader reader) {
    String nil = reader.getAttributeValue(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "nil");
    return "true".equals(nil) || "1".equals(nil);
  }

  /** Applies {@link XmlLayout#lineBreaks()} to the text of an element without child elements. */
  static String lineBreaks(String text, XmlLayout.LineBreaks mode) {
    return switch (mode) {
      case STRIP -> text.replaceAll("[\\n\\r]", "").trim();
      // one space for each run of line breaks and the blanks around it
      case SPACE ->
          text.lines()
              .map(String::strip)
              .filter(line -> !line.isEmpty())
              .collect(Collectors.joining(" "));
      case KEEP -> text;
    };
  }
}

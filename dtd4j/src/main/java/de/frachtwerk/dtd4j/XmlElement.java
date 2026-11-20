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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An element of an {@link XmlDocument} with its child elements in document order and its text. Not
 * thread-safe.
 */
public final class XmlElement {

  private final String name;
  private final List<XmlElement> children = new ArrayList<>();
  private String text;
  private boolean nil;

  public XmlElement(String name) {
    this.name = Objects.requireNonNull(name, "name");
  }

  public String name() {
    return name;
  }

  /** The text content, or {@code null} for an element without text. */
  public String text() {
    return text;
  }

  public XmlElement text(String text) {
    this.text = text;
    return this;
  }

  /** {@code true} for {@code xsi:nil="true"}. */
  public boolean nil() {
    return nil;
  }

  public XmlElement nil(boolean nil) {
    this.nil = nil;
    return this;
  }

  /** The child elements in document order; modifiable. */
  public List<XmlElement> children() {
    return children;
  }

  public List<XmlElement> children(String childName) {
    return children.stream().filter(child -> child.name.equals(childName)).toList();
  }

  public Optional<XmlElement> child(String childName) {
    return children.stream().filter(child -> child.name.equals(childName)).findFirst();
  }

  public XmlElement add(XmlElement child) {
    children.add(Objects.requireNonNull(child, "child"));
    return this;
  }

  @Override
  public String toString() {
    return "<" + name + ">" + (children.isEmpty() ? Objects.toString(text, "") : children);
  }
}

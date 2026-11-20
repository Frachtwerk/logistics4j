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

/** Stable codes of {@link Problem}s; applications may translate them. New codes go to the end. */
public enum ProblemCode {
  /** The root element is not the one of the layout. */
  UNEXPECTED_ROOT,
  /** The element is not declared in the DTD. */
  UNKNOWN_ELEMENT,
  /** The child elements do not match the content model of the element. */
  INVALID_CONTENT,
  /** Text in an element that may only contain elements. */
  TEXT_NOT_ALLOWED,
  /** A property of an object has no element in the layout and is not written. */
  UNMAPPED_PROPERTY,
  /** A value cannot be written as the content of its element, e.g. an object for a text element. */
  INVALID_VALUE
}

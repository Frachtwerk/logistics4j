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

package de.frachtwerk.fortras4j;

/** Stable identifiers for problems; meant for translation and handling in applications. */
public enum ProblemCode {
  /** No layout matches the file. */
  NO_SCHEMA,
  LINE_TOO_LONG,
  /** The line starts with no known record prefix. */
  UNKNOWN_RECORD,
  INVALID_VALUE,
  UNKNOWN_CODE,
  VALUE_TOO_LONG,
  /** A mandatory record or group is missing. */
  MISSING,
  /** A record or group occurs more often than allowed. */
  TOO_MANY,
  /** A known record at a position the structure does not allow. */
  UNEXPECTED_RECORD,
  /** A record inside a group carries a different key value (tolerated with keyMismatch: warn). */
  KEY_MISMATCH,
  DUPLICATE_VALUE,
  MISSING_VALUE,
  UNEXPECTED_VALUE
}

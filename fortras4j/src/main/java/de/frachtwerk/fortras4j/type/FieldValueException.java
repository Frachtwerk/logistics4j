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

package de.frachtwerk.fortras4j.type;

import de.frachtwerk.fortras4j.ProblemCode;
import de.frachtwerk.fortras4j.Severity;

/** A field value could not be read or written. */
public class FieldValueException extends RuntimeException {

  private final ProblemCode code;
  private final Severity severity;
  private final transient Object retainedValue;

  public FieldValueException(ProblemCode code, String message) {
    this(code, Severity.ERROR, message, null);
  }

  /**
   * @param retainedValue value to keep in the record despite the problem (e.g. an unknown code)
   */
  public FieldValueException(
      ProblemCode code, Severity severity, String message, Object retainedValue) {
    super(message);
    this.code = code;
    this.severity = severity;
    this.retainedValue = retainedValue;
  }

  public ProblemCode code() {
    return code;
  }

  public Severity severity() {
    return severity;
  }

  public Object retainedValue() {
    return retainedValue;
  }
}

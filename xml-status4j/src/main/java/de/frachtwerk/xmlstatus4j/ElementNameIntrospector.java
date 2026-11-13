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

import java.util.Map;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.AnnotatedField;
import tools.jackson.dataformat.xml.JacksonXmlAnnotationIntrospector;

/**
 * Takes element names from the configuration where it renames a property and from the annotations
 * of the model otherwise.
 */
final class ElementNameIntrospector extends JacksonXmlAnnotationIntrospector {

  private static final long serialVersionUID = 1L;

  /** Element names keyed by {@code <class>.<property>}. */
  private final Map<String, String> elements;

  private final String rootElement;

  ElementNameIntrospector(Map<String, String> elements, String rootElement) {
    this.elements = Map.copyOf(elements);
    this.rootElement = rootElement;
  }

  @Override
  public PropertyName findRootName(MapperConfig<?> config, AnnotatedClass ac) {
    if (ac.getRawType() == FahrDaten.class) {
      return PropertyName.construct(rootElement);
    }
    return super.findRootName(config, ac);
  }

  @Override
  public PropertyName findNameForSerialization(MapperConfig<?> config, Annotated a) {
    PropertyName renamed = renamed(a);
    return renamed != null ? renamed : super.findNameForSerialization(config, a);
  }

  @Override
  public PropertyName findNameForDeserialization(MapperConfig<?> config, Annotated a) {
    PropertyName renamed = renamed(a);
    return renamed != null ? renamed : super.findNameForDeserialization(config, a);
  }

  private PropertyName renamed(Annotated a) {
    if (a instanceof AnnotatedField field) {
      String name = elements.get(field.getDeclaringClass().getSimpleName() + "." + field.getName());
      if (name != null) {
        return PropertyName.construct(name);
      }
    }
    return null;
  }
}

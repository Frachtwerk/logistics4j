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

import de.frachtwerk.dtd4j.ContentMatcher.Element;
import de.frachtwerk.dtd4j.ContentMatcher.Items;
import de.frachtwerk.dtd4j.ContentMatcher.Option;
import de.frachtwerk.dtd4j.ContentMatcher.Part;
import de.frachtwerk.dtd4j.ContentMatcher.Unit;
import de.frachtwerk.dtd4j.ContentModel.Children;
import de.frachtwerk.dtd4j.Particle.Choice;
import de.frachtwerk.dtd4j.Particle.Name;
import de.frachtwerk.dtd4j.Particle.Sequence;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Converts between {@link XmlElement}s and Jackson trees: child elements become properties named by
 * the layout, elements that may occur more than once become lists, and the occurrences of a named
 * group become objects of a list property.
 */
final class Binder {

  private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

  private final XmlLayout layout;
  private final Map<String, Shape> shapes;

  Binder(XmlLayout layout) {
    this.layout = layout;
    this.shapes = Shape.of(layout);
  }

  // ---- reading -------------------------------------------------------------------------------

  /** The value of an element: an object, a text or null. */
  JsonNode toTree(XmlElement element) {
    if (element.nil()) {
      return NODES.nullNode();
    }
    Optional<ContentModel> content = content(element.name());
    if (content.isPresent() && content.get() instanceof Children children) {
      ObjectNode node = NODES.objectNode();
      if (!element.children().isEmpty()) {
        object(node, element, children);
      }
      return node;
    }
    if (!element.children().isEmpty() && !(content.orElse(null) instanceof ContentModel.Mixed)) {
      // ANY or undeclared: bound by name
      ObjectNode node = NODES.objectNode();
      byName(node, element.children(), Shape.NONE, false);
      return node;
    }
    return NODES.stringNode(element.text() == null ? "" : element.text());
  }

  private void object(ObjectNode node, XmlElement element, Children children) {
    Shape shape = shapes.getOrDefault(element.name(), Shape.NONE);
    List<String> names = element.children().stream().map(XmlElement::name).toList();
    Optional<Part> match = ContentMatcher.match(children.particle(), names);
    if (match.isPresent()) {
      fill(node, match.get(), element, shape, false);
    } else {
      // the content does not match the DTD: keep the data, bound by name
      byName(node, element.children(), shape, false);
    }
  }

  private void fill(ObjectNode node, Part part, XmlElement element, Shape shape, boolean inGroup) {
    if (part.particle() == shape.group()) {
      ArrayNode groups = array(node, layout.groups().get(element.name()));
      for (Unit unit : part.occurrences()) {
        fillUnit(groups.addObject(), unit, element, shape, true);
      }
      return;
    }
    for (Unit unit : part.occurrences()) {
      fillUnit(node, unit, element, shape, inGroup);
    }
  }

  private void fillUnit(
      ObjectNode node, Unit unit, XmlElement element, Shape shape, boolean inGroup) {
    switch (unit) {
      case Element child -> put(node, element.children().get(child.index()), shape, inGroup);
      case Items items -> items.parts().forEach(p -> fill(node, p, element, shape, inGroup));
      case Option option -> fill(node, option.part(), element, shape, inGroup);
    }
  }

  private void byName(ObjectNode node, List<XmlElement> children, Shape shape, boolean inGroup) {
    Map<String, Integer> counts = new HashMap<>();
    children.forEach(child -> counts.merge(child.name(), 1, Integer::sum));
    for (XmlElement child : children) {
      String property = layout.property(child.name());
      JsonNode value = toTree(child);
      if (counts.get(child.name()) > 1
          || shape.isList(child.name(), inGroup)
          || shape.isList(child.name(), !inGroup)) {
        array(node, property).add(value);
      } else {
        node.set(property, value);
      }
    }
  }

  private void put(ObjectNode node, XmlElement child, Shape shape, boolean inGroup) {
    String property = layout.property(child.name());
    JsonNode value = toTree(child);
    if (shape.isList(child.name(), inGroup)) {
      array(node, property).add(value);
    } else {
      node.set(property, value);
    }
  }

  private static ArrayNode array(ObjectNode node, String property) {
    return node.get(property) instanceof ArrayNode existing ? existing : node.putArray(property);
  }

  // ---- writing -------------------------------------------------------------------------------

  /** The element for a non-null value; problems go to {@code problems}. */
  XmlElement toElement(String name, JsonNode value, String path, List<Problem> problems) {
    XmlElement element = new XmlElement(name);
    Optional<ContentModel> content = content(name);
    if (content.isPresent() && content.get() instanceof Children children) {
      if (!(value instanceof ObjectNode data)) {
        problem(problems, ProblemCode.INVALID_VALUE, path, name + " needs an object: " + value);
        return element;
      }
      Shape shape = shapes.getOrDefault(name, Shape.NONE);
      Set<String> used = new HashSet<>();
      List<XmlElement> out = element.children();
      emit(name, children.particle(), data, out, shape, false, used, path, problems);
      unmapped(data, used, path, problems);
      if (layout.order() == XmlLayout.Order.ALPHABETICAL && shape.group() == null) {
        out.sort(Comparator.comparing(XmlElement::name));
      }
      return element;
    }
    if (value.isContainer()) {
      problem(problems, ProblemCode.INVALID_VALUE, path, name + " needs a text: " + value);
      return element;
    }
    return element.text(value.asString());
  }

  private void emit(
      String element,
      Particle particle,
      ObjectNode data,
      List<XmlElement> out,
      Shape shape,
      boolean inGroup,
      Set<String> used,
      String path,
      List<Problem> problems) {
    if (particle == shape.group()) {
      String property = layout.groups().get(element);
      used.add(property);
      JsonNode groups = data.get(property);
      if (groups instanceof ArrayNode list) {
        for (JsonNode group : list) {
          if (group instanceof ObjectNode groupData) {
            List<XmlElement> block = new ArrayList<>();
            Set<String> groupUsed = new HashSet<>();
            for (Particle part : Shape.parts(particle)) {
              emit(element, part, groupData, block, shape, true, groupUsed, path, problems);
            }
            unmapped(groupData, groupUsed, path, problems);
            if (layout.order() == XmlLayout.Order.ALPHABETICAL) {
              block.sort(Comparator.comparing(XmlElement::name));
            }
            out.addAll(block);
          }
        }
      }
      return;
    }
    switch (particle) {
      case Name name -> {
        String property = layout.property(name.name());
        used.add(property);
        JsonNode value = data.get(property);
        if (value == null) {
          return;
        }
        // a list for an element that may occur once is written as it is; validation reports it
        if (value.isArray()) {
          for (JsonNode item : value) {
            single(name.name(), item, out, path, problems);
          }
        } else if (!(shape.isList(name.name(), inGroup) && value.isNull())) {
          single(name.name(), value, out, path, problems);
        }
      }
      case Sequence sequence -> {
        for (Particle item : sequence.items()) {
          emit(element, item, data, out, shape, inGroup, used, path, problems);
        }
      }
      case Choice choice -> {
        for (Particle option : choice.options()) {
          emit(element, option, data, out, shape, inGroup, used, path, problems);
        }
      }
    }
  }

  private void single(
      String name, JsonNode value, List<XmlElement> out, String path, List<Problem> problems) {
    if (value.isNull()) {
      boolean container = content(name).orElse(null) instanceof Children;
      if (container && layout.nullObjects() == XmlLayout.NullObjects.OMIT) {
        return;
      }
      switch (layout.nulls()) {
        case EMPTY -> out.add(new XmlElement(name));
        case NIL -> out.add(new XmlElement(name).nil(true));
        case OMIT -> {
          // no element
        }
      }
      return;
    }
    out.add(toElement(name, value, path + "/" + name, problems));
  }

  private void unmapped(ObjectNode data, Set<String> used, String path, List<Problem> problems) {
    for (Map.Entry<String, JsonNode> entry : data.properties()) {
      if (!used.contains(entry.getKey()) && !entry.getValue().isNull()) {
        problem(
            problems,
            ProblemCode.UNMAPPED_PROPERTY,
            path,
            "property " + entry.getKey() + " has no element and is not written");
      }
    }
  }

  private void problem(List<Problem> problems, ProblemCode code, String path, String message) {
    if (layout.validation() != XmlLayout.Validation.OFF) {
      Severity severity =
          layout.validation() == XmlLayout.Validation.ERROR ? Severity.ERROR : Severity.WARNING;
      problems.add(new Problem(severity, code, path, message));
    }
  }

  private Optional<ContentModel> content(String name) {
    return layout.dtd().element(name).map(ElementDeclaration::content);
  }
}

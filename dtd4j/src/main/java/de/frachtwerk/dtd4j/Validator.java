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

import de.frachtwerk.dtd4j.ContentModel.Any;
import de.frachtwerk.dtd4j.ContentModel.Children;
import de.frachtwerk.dtd4j.ContentModel.Empty;
import de.frachtwerk.dtd4j.ContentModel.Mixed;
import de.frachtwerk.dtd4j.ContentModel.Text;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Checks a tree of {@link XmlElement}s against the DTD of a layout. */
final class Validator {

  private final XmlLayout layout;
  private final Severity severity;
  private final List<Problem> problems = new ArrayList<>();

  private Validator(XmlLayout layout) {
    this.layout = layout;
    this.severity =
        layout.validation() == XmlLayout.Validation.ERROR ? Severity.ERROR : Severity.WARNING;
  }

  static List<Problem> validate(XmlElement root, XmlLayout layout) {
    if (layout.validation() == XmlLayout.Validation.OFF) {
      return List.of();
    }
    Validator validator = new Validator(layout);
    if (!root.name().equals(layout.root())) {
      validator.report(
          ProblemCode.UNEXPECTED_ROOT,
          root.name(),
          "expected root element " + layout.root() + " but found " + root.name());
    }
    validator.element(root, root.name());
    return validator.problems;
  }

  private void element(XmlElement element, String path) {
    Optional<ElementDeclaration> declaration = layout.dtd().element(element.name());
    if (declaration.isEmpty()) {
      report(ProblemCode.UNKNOWN_ELEMENT, path, "element " + element.name() + " is not declared");
    } else if (!element.nil()) {
      content(element, declaration.get().content(), path);
    }
    Map<String, Integer> counts = new HashMap<>();
    for (XmlElement child : element.children()) {
      int index = counts.merge(child.name(), 1, Integer::sum);
      element(child, path + "/" + child.name() + "[" + index + "]");
    }
  }

  private void content(XmlElement element, ContentModel content, String path) {
    switch (content) {
      case Empty empty -> {
        if (!element.children().isEmpty() || !isBlank(element.text())) {
          report(ProblemCode.INVALID_CONTENT, path, element.name() + " must be empty");
        }
      }
      case Text text -> {
        if (!element.children().isEmpty()) {
          report(
              ProblemCode.INVALID_CONTENT,
              path,
              element.name() + " may only contain text but contains " + names(element));
        }
      }
      case Mixed mixed -> {
        for (XmlElement child : element.children()) {
          if (!mixed.names().contains(child.name())) {
            report(
                ProblemCode.INVALID_CONTENT,
                path,
                child.name() + " is not allowed in " + element.name() + " " + mixed);
          }
        }
      }
      case Children children -> {
        if (!isBlank(element.text())) {
          report(
              ProblemCode.TEXT_NOT_ALLOWED,
              path,
              element.name() + " may only contain elements but contains text");
        }
        if (ContentMatcher.match(children.particle(), names(element)).isEmpty()) {
          report(
              ProblemCode.INVALID_CONTENT,
              path,
              "content "
                  + names(element)
                  + " of "
                  + element.name()
                  + " does not match "
                  + children);
        }
      }
      case Any any -> {
        // any declared element and text
      }
    }
  }

  private static List<String> names(XmlElement element) {
    return element.children().stream().map(XmlElement::name).toList();
  }

  private static boolean isBlank(String text) {
    return text == null || text.isBlank();
  }

  private void report(ProblemCode code, String path, String message) {
    problems.add(new Problem(severity, code, path, message));
  }
}

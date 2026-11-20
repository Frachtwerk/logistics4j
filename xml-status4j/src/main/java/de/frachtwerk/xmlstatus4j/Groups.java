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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.deser.SettableBeanProperty;
import tools.jackson.databind.deser.bean.BeanDeserializerBase;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.util.NameTransformer;

/**
 * Reads and writes an element whose content is a sequence of groups without an element of their
 * own, e.g. {@code FAHRT (FAR?, SDG+)+}. A new group starts with an element that does not fit
 * behind the previous one: one that comes earlier in the group, or the same again unless it is a
 * list.
 */
final class Groups<E, G> {

  private final Class<E> elementType;
  private final Class<G> groupType;
  private final Supplier<E> newElement;
  private final Supplier<G> newGroup;
  private final Function<E, List<G>> groups;
  private final BiConsumer<E, List<G>> setGroups;
  // properties of the group in the order of the DTD
  private final List<String> order;

  private Groups(
      Class<E> elementType,
      Class<G> groupType,
      Supplier<E> newElement,
      Supplier<G> newGroup,
      Function<E, List<G>> groups,
      BiConsumer<E, List<G>> setGroups) {
    this.elementType = elementType;
    this.groupType = groupType;
    this.newElement = newElement;
    this.newGroup = newGroup;
    this.groups = groups;
    this.setGroups = setGroups;
    this.order = XmlStatusConfig.properties(groupType);
  }

  /** Serializers and deserializers for {@code FAHRT}, {@code POSI} and {@code BCD}. */
  static SimpleModule module() {
    SimpleModule module = new SimpleModule();
    new Groups<>(
            Fahrt.class,
            FahrtAbschnitt.class,
            Fahrt::new,
            FahrtAbschnitt::new,
            Fahrt::getAbschnitte,
            Fahrt::setAbschnitte)
        .register(module);
    new Groups<>(
            PositionsDaten.class,
            Position.class,
            PositionsDaten::new,
            Position::new,
            PositionsDaten::getPositionen,
            PositionsDaten::setPositionen)
        .register(module);
    new Groups<>(
            BarcodeDaten.class,
            Barcode.class,
            BarcodeDaten::new,
            Barcode::new,
            BarcodeDaten::getBarcodes,
            BarcodeDaten::setBarcodes)
        .register(module);
    return module;
  }

  private void register(SimpleModule module) {
    module.addSerializer(elementType, new Writer());
    module.addDeserializer(elementType, new Reader());
  }

  private final class Writer extends ValueSerializer<E> {
    @Override
    public void serialize(E element, JsonGenerator gen, SerializationContext ctxt) {
      gen.writeStartObject(element);
      List<G> list = groups.apply(element);
      if (list != null) {
        ValueSerializer<Object> group =
            ctxt.findValueSerializer(groupType).unwrappingSerializer(NameTransformer.NOP);
        for (G g : list) {
          if (g != null) {
            group.serialize(g, gen, ctxt);
          }
        }
      }
      gen.writeEndObject();
    }
  }

  private final class Reader extends ValueDeserializer<E> {
    // position in the group by element name; depends on the names configured for the mapper
    private volatile Map<String, Integer> positions;

    @Override
    public E deserialize(JsonParser p, DeserializationContext ctxt) {
      E element = newElement.get();
      List<G> result = new ArrayList<>();
      setGroups.accept(element, result);
      if (p.hasToken(JsonToken.VALUE_STRING) && p.getString().isBlank()) {
        // an empty element, e.g. <BCD/>
        return element;
      }
      if (!p.isExpectedStartObjectToken()) {
        return elementType.cast(ctxt.handleUnexpectedToken(elementType, p));
      }
      BeanDeserializerBase bean = groupDeserializer(ctxt);
      G group = null;
      int last = -1;
      Map<SettableBeanProperty, List<Object>> lists = new LinkedHashMap<>();
      for (String name = p.nextName(); name != null; name = p.nextName()) {
        SettableBeanProperty property = bean.findProperty(PropertyName.construct(name));
        p.nextToken();
        if (property == null) {
          ctxt.handleUnknownProperty(p, this, element, name);
          continue;
        }
        int index = positions(ctxt).getOrDefault(name, -1);
        boolean list = property.getType().isCollectionLikeType();
        if (group == null || index < last || (index == last && !list)) {
          setLists(ctxt, group, lists);
          group = newGroup.get();
          result.add(group);
        }
        last = index;
        if (list) {
          lists
              .computeIfAbsent(property, k -> new ArrayList<>())
              .add(ctxt.readValue(p, property.getType().getContentType()));
        } else {
          property.deserializeAndSet(p, ctxt, group);
        }
      }
      setLists(ctxt, group, lists);
      return element;
    }

    private Map<String, Integer> positions(DeserializationContext ctxt) {
      Map<String, Integer> result = positions;
      if (result == null) {
        result = new HashMap<>();
        for (BeanPropertyDefinition property :
            ctxt.introspectBeanDescriptionForCreation(ctxt.constructType(groupType))
                .findProperties()) {
          result.put(property.getName(), order.indexOf(property.getInternalName()));
        }
        positions = result;
      }
      return result;
    }

    private BeanDeserializerBase groupDeserializer(DeserializationContext ctxt) {
      ValueDeserializer<?> deserializer =
          ctxt.findRootValueDeserializer(ctxt.constructType(groupType));
      // the XML module wraps bean deserializers that have unwrapped lists
      while (deserializer.getDelegatee() != null) {
        deserializer = deserializer.getDelegatee();
      }
      return (BeanDeserializerBase) deserializer;
    }

    private void setLists(
        DeserializationContext ctxt, G group, Map<SettableBeanProperty, List<Object>> lists) {
      lists.forEach((property, values) -> property.set(ctxt, group, values));
      lists.clear();
    }
  }
}

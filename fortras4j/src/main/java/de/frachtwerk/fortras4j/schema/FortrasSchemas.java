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

package de.frachtwerk.fortras4j.schema;

import de.frachtwerk.fortras4j.type.FieldType;
import de.frachtwerk.fortras4j.type.FieldTypes;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * An immutable set of layouts, e.g. all releases of BORD and STAT a system understands.
 *
 * <pre>{@code
 * FortrasSchemas schemas = FortrasSchemas.builder()
 *     .addDirectory(Path.of("/etc/fortras/layouts"))
 *     .addClasspath("layouts/bord100.yaml")
 *     .build();
 * }</pre>
 */
public final class FortrasSchemas implements SchemaSource {

  private final Map<String, FortrasSchema> schemas;

  FortrasSchemas(Map<String, FortrasSchema> schemas) {
    this.schemas = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(schemas));
  }

  public static Builder builder() {
    return new Builder();
  }

  public FortrasSchema get(String id) {
    FortrasSchema schema = schemas.get(id);
    if (schema == null) {
      throw new IllegalArgumentException("unknown layout '" + id + "'; known: " + schemas.keySet());
    }
    return schema;
  }

  public Optional<FortrasSchema> find(String id) {
    return Optional.ofNullable(schemas.get(id));
  }

  /**
   * The layout whose {@code detect} pattern matches the first line of a file.
   *
   * @throws IllegalStateException if several layouts match; detect patterns must be unambiguous
   */
  public Optional<FortrasSchema> detect(String firstLine) {
    List<FortrasSchema> matching =
        schemas.values().stream().filter(s -> s.matches(firstLine)).toList();
    if (matching.size() > 1) {
      throw new IllegalStateException(
          "several layouts match '"
              + firstLine
              + "': "
              + matching.stream().map(FortrasSchema::id).toList());
    }
    return matching.stream().findFirst();
  }

  public Collection<FortrasSchema> all() {
    return schemas.values();
  }

  @Override
  public FortrasSchemas schemas() {
    return this;
  }

  public static final class Builder {

    private final List<SchemaLoader.Source> sources = new ArrayList<>();
    private final FieldTypes types = FieldTypes.builtIn();

    private Builder() {}

    /** Registers a custom field type, usable as {@code type: <name>} in layouts. */
    public Builder type(FieldType type) {
      types.register(type);
      return this;
    }

    public Builder add(Path file) {
      try (InputStream in = Files.newInputStream(file)) {
        return add(file.toString(), in);
      } catch (IOException e) {
        throw new UncheckedIOException("cannot read layout " + file, e);
      }
    }

    /** Adds every {@code *.yaml} and {@code *.yml} file of a directory (not recursive). */
    public Builder addDirectory(Path directory) {
      try (Stream<Path> files = Files.list(directory)) {
        files
            .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
            .sorted()
            .forEach(this::add);
        return this;
      } catch (IOException e) {
        throw new UncheckedIOException("cannot list layouts in " + directory, e);
      }
    }

    /** Adds a layout from the classpath; works inside JARs. */
    public Builder addClasspath(String resource) {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      if (loader == null) {
        loader = FortrasSchemas.class.getClassLoader();
      }
      return addClasspath(resource, loader);
    }

    public Builder addClasspath(String resource, ClassLoader loader) {
      try (InputStream in = loader.getResourceAsStream(resource)) {
        if (in == null) {
          throw new IllegalArgumentException("layout not found on classpath: " + resource);
        }
        return add("classpath:" + resource, in);
      } catch (IOException e) {
        throw new UncheckedIOException("cannot read layout " + resource, e);
      }
    }

    /** Adds a layout in YAML (or JSON, which is valid YAML). The stream is read but not closed. */
    public Builder add(String sourceName, InputStream yaml) {
      sources.add(SchemaLoader.read(sourceName, yaml));
      return this;
    }

    /**
     * @throws SchemaException listing every problem in every layout
     */
    public FortrasSchemas build() {
      return new FortrasSchemas(new LinkedHashMap<>(SchemaLoader.build(sources, types)));
    }
  }
}

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

import de.frachtwerk.fortras4j.bind.FortrasBinder;
import de.frachtwerk.fortras4j.schema.FortrasSchema;
import de.frachtwerk.fortras4j.schema.SchemaSource;
import java.util.List;
import java.util.Objects;

/**
 * Entry point combining reader, writer and binder.
 *
 * <pre>{@code
 * Fortras4j fortras = Fortras4j.of(FortrasSchemas.builder().addDirectory(dir).build());
 *
 * FortrasDocument document = fortras.read(lines);          // generic records and groups
 * Bordero bordero = fortras.read(lines, Bordero.class);    // your own classes
 * List<String> out = fortras.write(bordero, "bord100");
 * }</pre>
 */
public final class Fortras4j {

  private final SchemaSource source;
  private final FortrasReader reader;
  private final FortrasWriter writer;
  private final FortrasBinder binder;

  private Fortras4j(SchemaSource source, FortrasWriter writer, FortrasBinder binder) {
    this.source = Objects.requireNonNull(source);
    this.reader = FortrasReader.of(source);
    this.writer = writer;
    this.binder = binder;
  }

  public static Fortras4j of(SchemaSource source) {
    return new Fortras4j(source, FortrasWriter.create(), FortrasBinder.create());
  }

  public Fortras4j withBinder(FortrasBinder binder) {
    return new Fortras4j(source, writer, binder);
  }

  public Fortras4j withWriter(FortrasWriter writer) {
    return new Fortras4j(source, writer, binder);
  }

  /** Reads leniently; check {@link FortrasDocument#problems()}. */
  public FortrasDocument read(List<String> lines) {
    return reader.read(lines);
  }

  /**
   * Reads and binds to {@code type}.
   *
   * @throws FortrasException if the file has errors
   */
  public <T> T read(List<String> lines, Class<T> type) {
    return binder.toObject(reader.failOnError(true).read(lines), type);
  }

  public List<String> write(FortrasDocument document) {
    return writer.write(document).lines();
  }

  public List<String> write(Object object, String schemaId) {
    FortrasSchema schema = source.schemas().get(schemaId);
    return write(binder.fromObject(object, schema));
  }

  public FortrasReader reader() {
    return reader;
  }

  public FortrasWriter writer() {
    return writer;
  }

  public FortrasBinder binder() {
    return binder;
  }
}

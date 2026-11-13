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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Holds layouts that can be reloaded at runtime. A reload that fails keeps the previous layouts
 * active and rethrows, so a broken file never replaces a working configuration.
 */
public final class ReloadableSchemas implements SchemaSource {

  private final Supplier<FortrasSchemas> loader;
  private final AtomicReference<FortrasSchemas> current;

  public ReloadableSchemas(Supplier<FortrasSchemas> loader) {
    this.loader = Objects.requireNonNull(loader);
    this.current = new AtomicReference<>(loader.get());
  }

  public FortrasSchemas reload() {
    FortrasSchemas loaded = loader.get();
    current.set(loaded);
    return loaded;
  }

  @Override
  public FortrasSchemas schemas() {
    return current.get();
  }
}

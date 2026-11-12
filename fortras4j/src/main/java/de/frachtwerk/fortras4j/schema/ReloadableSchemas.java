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

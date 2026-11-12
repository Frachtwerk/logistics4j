package de.frachtwerk.fortras4j;

import java.util.List;

/** Element of a parsed document: a {@link Record} or a {@link Group}. */
public sealed interface Node permits Record, Group {

  /** All records below this node in file order, including this node itself if it is a record. */
  List<Record> flatten();
}

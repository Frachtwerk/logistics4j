package de.frachtwerk.fortras4j.schema;

import de.frachtwerk.fortras4j.schema.StructureNode.ChoiceNode;
import de.frachtwerk.fortras4j.schema.StructureNode.GroupNode;
import de.frachtwerk.fortras4j.schema.StructureNode.RecordRef;
import de.frachtwerk.fortras4j.type.FieldCodec;
import de.frachtwerk.fortras4j.type.FieldContext;
import de.frachtwerk.fortras4j.type.FieldOptions;
import de.frachtwerk.fortras4j.type.FieldType;
import de.frachtwerk.fortras4j.type.FieldTypes;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

/** Turns layout files into validated {@link FortrasSchema}s, collecting all errors. */
final class SchemaLoader {

  record Source(String name, ObjectNode root) {}

  private static final YAMLMapper YAML = YAMLMapper.builder().build();

  private static final Set<String> TOP_LEVEL_KEYS =
      Set.of(
          "id",
          "extends",
          "format",
          "release",
          "description",
          "detect",
          "maxLineLength",
          "trimTrailingSpaces",
          "passthrough",
          "defaults",
          "codelists",
          "records",
          "structure");
  private static final Set<String> MERGED_MAPS = Set.of("defaults", "codelists", "records");
  private static final Set<String> FIELD_KEYS =
      Set.of("name", "start", "end", "length", "type", "description", "default", "required");
  private static final Set<String> REPEAT_KEYS =
      Set.of(
          "repeat", "start", "count", "stride", "skipEmpty", "presentIf", "description", "fields");
  private static final Set<String> RECORD_KEYS =
      Set.of("prefix", "description", "length", "fields");
  private static final Set<String> RECORD_REF_KEYS = Set.of("record", "occurs", "as");
  private static final Set<String> GROUP_KEYS =
      Set.of("group", "occurs", "key", "keyMismatch", "constraints", "children");
  private static final Set<String> CHOICE_KEYS = Set.of("choice", "occurs", "options");

  private SchemaLoader() {}

  static Source read(String sourceName, InputStream yaml) {
    JsonNode node;
    try {
      node = YAML.readTree(yaml);
    } catch (JacksonException e) {
      throw new SchemaException(sourceName + ": not valid YAML: " + e.getOriginalMessage());
    }
    if (node == null || !node.isObject()) {
      throw new SchemaException(sourceName + ": a layout must be a YAML mapping");
    }
    return new Source(sourceName, (ObjectNode) node);
  }

  static Map<String, FortrasSchema> build(List<Source> sources, FieldTypes types) {
    List<String> errors = new ArrayList<>();
    Map<String, Source> byId = new LinkedHashMap<>();
    for (Source source : sources) {
      JsonNode id = source.root().get("id");
      if (id == null || !id.isString() || id.asString().isBlank()) {
        errors.add(source.name() + ": missing 'id'");
      } else if (byId.containsKey(id.asString())) {
        errors.add(
            source.name()
                + ": id '"
                + id.asString()
                + "' already defined in "
                + byId.get(id.asString()).name());
      } else {
        byId.put(id.asString(), source);
      }
    }

    Map<String, FortrasSchema> result = new LinkedHashMap<>();
    Map<String, ObjectNode> resolved = new LinkedHashMap<>();
    for (String id : byId.keySet()) {
      ObjectNode node = resolve(id, byId, resolved, new LinkedHashSet<>(), errors);
      if (node != null) {
        FortrasSchema schema = new SchemaBuilder(id, node, types, errors).build();
        if (schema != null) {
          result.put(id, schema);
        }
      }
    }
    if (!errors.isEmpty()) {
      throw new SchemaException(errors);
    }
    return result;
  }

  /** Applies {@code extends}: maps listed in {@link #MERGED_MAPS} merge per entry. */
  private static ObjectNode resolve(
      String id,
      Map<String, Source> byId,
      Map<String, ObjectNode> resolved,
      Set<String> visiting,
      List<String> errors) {
    if (resolved.containsKey(id)) {
      return resolved.get(id);
    }
    Source source = byId.get(id);
    if (!visiting.add(id)) {
      errors.add(source.name() + ": circular 'extends' via " + visiting);
      return null;
    }
    ObjectNode own = source.root();
    JsonNode parentId = own.get("extends");
    ObjectNode merged;
    if (parentId == null) {
      merged = own.deepCopy();
    } else if (!byId.containsKey(parentId.asString())) {
      errors.add(source.name() + ": extends unknown layout '" + parentId.asString() + "'");
      return null;
    } else {
      ObjectNode parent = resolve(parentId.asString(), byId, resolved, visiting, errors);
      if (parent == null) {
        return null;
      }
      merged = parent.deepCopy();
      for (Map.Entry<String, JsonNode> entry : own.properties()) {
        JsonNode existing = merged.get(entry.getKey());
        if (MERGED_MAPS.contains(entry.getKey())
            && existing != null
            && existing.isObject()
            && entry.getValue().isObject()) {
          ObjectNode map = (ObjectNode) existing;
          entry.getValue().properties().forEach(e -> map.set(e.getKey(), e.getValue().deepCopy()));
        } else {
          merged.set(entry.getKey(), entry.getValue().deepCopy());
        }
      }
      merged.remove("extends");
    }
    merged.put("id", id);
    visiting.remove(id);
    resolved.put(id, merged);
    return merged;
  }

  /** Builds one schema; errors are prefixed with the layout id and a path into the file. */
  private static final class SchemaBuilder {

    private final String id;
    private final ObjectNode root;
    private final FieldTypes types;
    private final List<String> errors;
    private final int errorsBefore;
    private final Map<String, CodeList> codeLists = new LinkedHashMap<>();
    private final Map<String, RecordDefinition> records = new LinkedHashMap<>();
    private Map<String, Map<String, Object>> typeDefaults = Map.of();
    private int maxLineLength;

    SchemaBuilder(String id, ObjectNode root, FieldTypes types, List<String> errors) {
      this.id = id;
      this.root = root;
      this.types = types;
      this.errors = errors;
      this.errorsBefore = errors.size();
    }

    FortrasSchema build() {
      unknownKeys(root, TOP_LEVEL_KEYS, "");
      maxLineLength = intValue(root.get("maxLineLength"), "maxLineLength", 512);
      Pattern detect = pattern(root.get("detect"), "detect");
      if (detect == null) {
        error("detect", "a 'detect' pattern for the first line is required");
      }
      Pattern passthrough = pattern(root.get("passthrough"), "passthrough");
      boolean trimTrailingSpaces = boolValue(root.get("trimTrailingSpaces"), false);

      typeDefaults = parseDefaults(root.get("defaults"));
      parseCodeLists(root.get("codelists"));
      parseRecords(root.get("records"));
      GroupNode structure = parseRoot(root.get("structure"));

      if (errors.size() > errorsBefore) {
        return null;
      }
      return new FortrasSchema(
          id,
          text(root.get("format")),
          text(root.get("release")),
          text(root.get("description")),
          detect,
          maxLineLength,
          trimTrailingSpaces,
          passthrough,
          records,
          codeLists,
          structure);
    }

    // ---- defaults and code lists ------------------------------------------------------------

    private Map<String, Map<String, Object>> parseDefaults(JsonNode node) {
      if (node == null) {
        return Map.of();
      }
      Map<String, Map<String, Object>> result = new LinkedHashMap<>();
      for (Map.Entry<String, JsonNode> entry : node.properties()) {
        if (types.get(entry.getKey()) == null) {
          error("defaults." + entry.getKey(), "unknown type; known: " + types.names());
        } else if (!entry.getValue().isObject()) {
          error("defaults." + entry.getKey(), "must be a mapping of options");
        } else {
          result.put(entry.getKey(), options(entry.getValue(), Set.of()));
        }
      }
      return result;
    }

    private void parseCodeLists(JsonNode node) {
      if (node == null) {
        return;
      }
      for (Map.Entry<String, JsonNode> entry : node.properties()) {
        String path = "codelists." + entry.getKey();
        JsonNode list = entry.getValue();
        boolean strict = true;
        JsonNode codes = list;
        if (list.isObject()) {
          unknownKeys(list, Set.of("strict", "codes"), path);
          strict = boolValue(list.get("strict"), true);
          codes = list.get("codes");
        }
        Map<String, String> parsed = new LinkedHashMap<>();
        if (codes == null) {
          error(path, "missing 'codes'");
        } else if (codes.isArray()) {
          for (JsonNode code : codes) {
            if (!code.isString()) {
              error(path, "code " + code + " is a number; write it as a quoted string");
            } else {
              parsed.put(code.asString(), null);
            }
          }
        } else if (codes.isObject()) {
          codes.properties().forEach(e -> parsed.put(e.getKey(), text(e.getValue())));
        } else {
          error(path, "'codes' must be a list or a mapping code -> description");
        }
        codeLists.put(entry.getKey(), new CodeList(entry.getKey(), parsed, strict));
      }
    }

    // ---- records ------------------------------------------------------------------------------

    private void parseRecords(JsonNode node) {
      if (node == null || !node.isObject() || node.isEmpty()) {
        error("records", "at least one record is required");
        return;
      }
      Map<String, String> prefixes = new LinkedHashMap<>();
      for (Map.Entry<String, JsonNode> entry : node.properties()) {
        String recordId = entry.getKey();
        String path = "records." + recordId;
        JsonNode definition = entry.getValue();
        if (!definition.isObject()) {
          error(path, "must be a mapping");
          continue;
        }
        unknownKeys(definition, RECORD_KEYS, path);
        String prefix = definition.has("prefix") ? text(definition.get("prefix")) : recordId;
        if (prefixes.containsKey(prefix)) {
          error(path, "prefix '" + prefix + "' already used by " + prefixes.get(prefix));
        }
        prefixes.put(prefix, recordId);

        List<FieldElement> elements = new ArrayList<>();
        JsonNode fields = definition.get("fields");
        if (fields != null && !fields.isArray()) {
          error(path + ".fields", "must be a list");
        } else if (fields != null) {
          int index = 0;
          for (JsonNode field : fields) {
            FieldElement element = parseElement(field, path + ".fields[" + index++ + "]");
            if (element != null) {
              elements.add(element);
            }
          }
        }
        checkPositions(path, prefix, elements);

        int lastEnd =
            Math.max(
                prefix.length(), elements.stream().mapToInt(FieldElement::end).max().orElse(0));
        int length = intValue(definition.get("length"), path + ".length", lastEnd);
        if (length < lastEnd) {
          error(path + ".length", "is shorter than the last field (ends at " + lastEnd + ")");
        }
        records.put(
            recordId,
            new RecordDefinition(
                recordId, prefix, text(definition.get("description")), elements, length));
      }
    }

    private FieldElement parseElement(JsonNode node, String path) {
      if (!node.isObject()) {
        error(path, "must be a mapping");
        return null;
      }
      if (node.has("repeat")) {
        return parseRepeat(node, path);
      }
      return parseField(node, path, false);
    }

    private FieldDefinition parseField(JsonNode node, String path, boolean relative) {
      String name = text(node.get("name"));
      if (name == null || name.isBlank()) {
        error(path, "missing 'name'");
        return null;
      }
      path = path + " (" + name + ")";
      int start = intValue(node.get("start"), path + ".start", -1);
      int end;
      if (node.has("end") == node.has("length")) {
        error(path, "exactly one of 'end' and 'length' is required");
        return null;
      } else if (node.has("end")) {
        end = intValue(node.get("end"), path + ".end", -1);
      } else {
        end = start + intValue(node.get("length"), path + ".length", 0) - 1;
      }
      if (start < 1 || end < start) {
        error(path, "invalid position " + start + "-" + end);
        return null;
      }
      if (!relative && end > maxLineLength) {
        error(path, "ends at " + end + ", beyond maxLineLength " + maxLineLength);
      }

      String typeName = node.has("type") ? text(node.get("type")) : "string";
      FieldType type = types.get(typeName);
      if (type == null) {
        error(path + ".type", "unknown type '" + typeName + "'; known: " + types.names());
        return null;
      }
      Map<String, Object> merged =
          new LinkedHashMap<>(typeDefaults.getOrDefault(typeName, Map.of()));
      merged.putAll(options(node, FIELD_KEYS));
      FieldOptions options = new FieldOptions(merged);
      FieldCodec codec;
      try {
        codec = type.create(options, new FieldContext(name, end - start + 1, codeLists));
      } catch (IllegalArgumentException e) {
        error(path, e.getMessage());
        return null;
      }
      Set<String> unused = options.unused();
      unused.removeAll(typeDefaults.getOrDefault(typeName, Map.of()).keySet());
      if (!unused.isEmpty()) {
        error(path, "unknown option(s) " + unused + " for type '" + typeName + "'");
      }

      Object defaultValue = null;
      if (node.has("default")) {
        try {
          defaultValue = codec.coerce(toJava(node.get("default")));
          codec.format(defaultValue, end - start + 1);
        } catch (RuntimeException e) {
          error(path + ".default", e.getMessage());
        }
      }
      return new FieldDefinition(
          name,
          start,
          end,
          typeName,
          codec,
          defaultValue,
          boolValue(node.get("required"), false),
          text(node.get("description")));
    }

    private RepeatDefinition parseRepeat(JsonNode node, String path) {
      unknownKeys(node, REPEAT_KEYS, path);
      String name = text(node.get("repeat"));
      path = path + " (" + name + ")";
      int start = intValue(node.get("start"), path + ".start", -1);
      int count = intValue(node.get("count"), path + ".count", -1);
      int stride = intValue(node.get("stride"), path + ".stride", -1);
      if (start < 1 || count < 1 || stride < 1) {
        error(path, "'start', 'count' and 'stride' must be positive");
        return null;
      }
      if (start + count * stride - 1 > maxLineLength) {
        error(path, "ends beyond maxLineLength " + maxLineLength);
      }
      List<FieldDefinition> fields = new ArrayList<>();
      JsonNode fieldNodes = node.get("fields");
      if (fieldNodes == null || !fieldNodes.isArray() || fieldNodes.isEmpty()) {
        error(path + ".fields", "a repeated block needs fields");
        return null;
      }
      Set<String> names = new HashSet<>();
      int index = 0;
      for (JsonNode fieldNode : fieldNodes) {
        String fieldPath = path + ".fields[" + index++ + "]";
        FieldDefinition field =
            fieldNode.isObject() ? parseField(fieldNode, fieldPath, true) : null;
        if (field == null) {
          continue;
        }
        if (field.end() > stride) {
          error(fieldPath, "ends at " + field.end() + ", beyond the block width " + stride);
        }
        if (!names.add(field.name())) {
          error(fieldPath, "duplicate field '" + field.name() + "'");
        }
        fields.add(field);
      }
      String presentIf = text(node.get("presentIf"));
      if (presentIf != null && !names.contains(presentIf)) {
        error(path + ".presentIf", "'" + presentIf + "' is no field of the block");
      }
      return new RepeatDefinition(
          name,
          start,
          count,
          stride,
          fields,
          boolValue(node.get("skipEmpty"), true),
          presentIf,
          text(node.get("description")));
    }

    /** Fields must not overlap each other or the record prefix; names must be unique. */
    private void checkPositions(String path, String prefix, List<FieldElement> elements) {
      record Range(String name, int start, int end) {}
      List<Range> ranges = new ArrayList<>();
      if (!prefix.isEmpty()) {
        ranges.add(new Range("prefix '" + prefix + "'", 1, prefix.length()));
      }
      Set<String> names = new HashSet<>();
      for (FieldElement element : elements) {
        if (!names.add(element.name())) {
          error(path, "duplicate field '" + element.name() + "'");
        }
        if (element instanceof FieldDefinition field) {
          ranges.add(new Range(field.name(), field.start(), field.end()));
        } else if (element instanceof RepeatDefinition repeat) {
          for (int i = 0; i < repeat.count(); i++) {
            for (FieldDefinition field : repeat.fields()) {
              int absolute = repeat.absoluteStart(i, field);
              ranges.add(
                  new Range(
                      repeat.name() + "[" + i + "]." + field.name(),
                      absolute,
                      absolute + field.length() - 1));
            }
          }
        }
      }
      ranges.sort((a, b) -> Integer.compare(a.start(), b.start()));
      for (int i = 1; i < ranges.size(); i++) {
        Range previous = ranges.get(i - 1);
        Range current = ranges.get(i);
        if (current.start() <= previous.end()) {
          error(
              path,
              current.name()
                  + " ("
                  + current.start()
                  + "-"
                  + current.end()
                  + ") overlaps "
                  + previous.name()
                  + " ("
                  + previous.start()
                  + "-"
                  + previous.end()
                  + ")");
        }
      }
    }

    // ---- structure ----------------------------------------------------------------------------

    private GroupNode parseRoot(JsonNode node) {
      if (node == null || !node.isArray() || node.isEmpty()) {
        error("structure", "a non-empty list is required");
        return new GroupNode("document", Occurs.ONCE, null, false, List.of(), List.of());
      }
      List<StructureNode> children = parseChildren(node, "structure");
      GroupNode root = new GroupNode("document", Occurs.ONCE, null, false, children, List.of());
      checkGroup(root, "structure");
      return root;
    }

    private List<StructureNode> parseChildren(JsonNode list, String path) {
      List<StructureNode> children = new ArrayList<>();
      int index = 0;
      for (JsonNode child : list) {
        StructureNode parsed = parseNode(child, path + "[" + index++ + "]", false);
        if (parsed != null) {
          children.add(parsed);
        }
      }
      return children;
    }

    private StructureNode parseNode(JsonNode node, String path, boolean option) {
      if (!node.isObject()) {
        error(path, "must be a mapping with 'record', 'group' or 'choice'");
        return null;
      }
      if (option && node.has("occurs")) {
        error(path, "options of a choice occur once per choice; set 'occurs' on the choice");
      }
      Occurs occurs = occurs(node.get("occurs"), path);
      if (node.has("record")) {
        unknownKeys(node, RECORD_REF_KEYS, path);
        String recordId = text(node.get("record"));
        if (!records.containsKey(recordId)) {
          error(path, "unknown record '" + recordId + "'");
        }
        return new RecordRef(recordId, occurs, text(node.get("as")));
      }
      if (node.has("group")) {
        unknownKeys(node, GROUP_KEYS, path);
        String name = text(node.get("group"));
        path = path + " (" + name + ")";
        JsonNode children = node.get("children");
        if (children == null || !children.isArray() || children.isEmpty()) {
          error(path, "a group needs 'children'");
          return null;
        }
        List<Constraint> constraints = parseConstraints(node.get("constraints"), path);
        String keyMismatch = node.has("keyMismatch") ? text(node.get("keyMismatch")) : "error";
        if (!keyMismatch.equals("error") && !keyMismatch.equals("warn")) {
          error(path + ".keyMismatch", "must be 'error' or 'warn'");
        }
        if (node.has("keyMismatch") && !node.has("key")) {
          error(path + ".keyMismatch", "requires 'key'");
        }
        return new GroupNode(
            name,
            occurs,
            text(node.get("key")),
            keyMismatch.equals("warn"),
            parseChildren(children, path + ".children"),
            constraints);
      }
      if (node.has("choice")) {
        unknownKeys(node, CHOICE_KEYS, path);
        String name = text(node.get("choice"));
        JsonNode options = node.get("options");
        if (options == null || !options.isArray() || options.size() < 2) {
          error(path, "a choice needs at least two 'options'");
          return null;
        }
        List<StructureNode> parsed = new ArrayList<>();
        int index = 0;
        for (JsonNode o : options) {
          StructureNode option1 = parseNode(o, path + ".options[" + index++ + "]", true);
          if (option1 != null) {
            parsed.add(option1);
          }
        }
        return new ChoiceNode(name, occurs, parsed);
      }
      error(path, "must contain 'record', 'group' or 'choice'");
      return null;
    }

    private List<Constraint> parseConstraints(JsonNode node, String path) {
      if (node == null) {
        return List.of();
      }
      List<Constraint> result = new ArrayList<>();
      int index = 0;
      for (JsonNode c : node) {
        String cPath = path + ".constraints[" + index++ + "]";
        unknownKeys(c, Set.of("unique", "required", "first", "record", "values"), cPath);
        Constraint.Kind kind = null;
        String field = null;
        for (Constraint.Kind k : Constraint.Kind.values()) {
          String key = k.name().toLowerCase(java.util.Locale.ROOT);
          if (c.has(key)) {
            if (kind != null) {
              error(cPath, "only one of unique/required/first per constraint");
            }
            kind = k;
            field = text(c.get(key));
          }
        }
        if (kind == null) {
          error(cPath, "needs one of 'unique', 'required' or 'first'");
          continue;
        }
        List<String> values = new ArrayList<>();
        if (c.has("values")) {
          c.get("values").forEach(v -> values.add(text(v)));
        }
        if (kind != Constraint.Kind.UNIQUE && values.isEmpty()) {
          error(cPath, "'" + kind.name().toLowerCase(java.util.Locale.ROOT) + "' needs 'values'");
        }
        result.add(new Constraint(kind, text(c.get("record")), field, values));
      }
      return result;
    }

    /** Binding names must be unique per group; keys and constraints must refer to real fields. */
    private void checkGroup(GroupNode group, String path) {
      Set<String> names = new HashSet<>();
      for (StructureNode.Binding child : StructureNode.bindings(group.children())) {
        if (!names.add(child.name())) {
          error(
              path,
              "'" + child.name() + "' is used twice in group '" + group.name() + "'; set 'as'");
        }
      }
      Set<String> descendants = new HashSet<>();
      collectRecords(group, descendants);
      if (group.key() != null
          && descendants.stream()
              .map(records::get)
              .noneMatch(r -> r != null && r.hasField(group.key()))) {
        error(
            path,
            "key '" + group.key() + "' is no field of any record in group '" + group.name() + "'");
      }
      for (Constraint constraint : group.constraints()) {
        RecordDefinition definition = records.get(constraint.recordId());
        if (definition == null || !descendants.contains(constraint.recordId())) {
          error(
              path,
              "constraint refers to record '" + constraint.recordId() + "' outside the group");
        } else if (!definition.hasField(constraint.field())) {
          error(
              path,
              "constraint refers to unknown field "
                  + constraint.recordId()
                  + "."
                  + constraint.field());
        }
      }
      for (StructureNode child : group.children()) {
        visitGroups(child, path);
      }
    }

    private void visitGroups(StructureNode node, String path) {
      switch (node) {
        case GroupNode g -> checkGroup(g, path + "/" + g.name());
        case ChoiceNode c -> c.options().forEach(o -> visitGroups(o, path));
        case RecordRef r -> {
          // nothing to check
        }
      }
    }

    // ---- helpers ------------------------------------------------------------------------------

    private void error(String path, String message) {
      errors.add(id + ": " + path + ": " + message);
    }

    private void unknownKeys(JsonNode node, Set<String> allowed, String path) {
      for (String key : node.propertyNames()) {
        if (!allowed.contains(key)) {
          error(path.isEmpty() ? key : path, "unknown key '" + key + "'; allowed: " + allowed);
        }
      }
    }

    private Occurs occurs(JsonNode node, String path) {
      if (node == null) {
        return Occurs.ONCE;
      }
      try {
        return Occurs.parse(node.asString());
      } catch (IllegalArgumentException e) {
        error(path + ".occurs", "invalid '" + node.asString() + "'; use e.g. 1, 0..1, 1..*, 0..9");
        return Occurs.ONCE;
      }
    }

    private Pattern pattern(JsonNode node, String path) {
      if (node == null) {
        return null;
      }
      try {
        return Pattern.compile(node.asString());
      } catch (PatternSyntaxException e) {
        error(path, "invalid regular expression: " + e.getDescription());
        return null;
      }
    }

    private int intValue(JsonNode node, String path, int defaultValue) {
      if (node == null) {
        return defaultValue;
      }
      if (node.isIntegralNumber()) {
        return node.intValue();
      }
      try {
        return Integer.parseInt(node.asString().trim());
      } catch (NumberFormatException e) {
        error(path, "must be a number");
        return defaultValue;
      }
    }

    private static boolean boolValue(JsonNode node, boolean defaultValue) {
      if (node == null) {
        return defaultValue;
      }
      return node.isBoolean() ? node.booleanValue() : Boolean.parseBoolean(node.asString());
    }

    private static Map<String, Object> options(JsonNode node, Set<String> exclude) {
      Map<String, Object> result = new LinkedHashMap<>();
      for (Map.Entry<String, JsonNode> entry : node.properties()) {
        if (!exclude.contains(entry.getKey())) {
          result.put(entry.getKey(), toJava(entry.getValue()));
        }
      }
      return result;
    }
  }

  static void collectRecords(StructureNode node, Set<String> into) {
    switch (node) {
      case RecordRef r -> into.add(r.recordId());
      case GroupNode g -> g.children().forEach(c -> collectRecords(c, into));
      case ChoiceNode c -> c.options().forEach(o -> collectRecords(o, into));
    }
  }

  private static String text(JsonNode node) {
    return node == null || node.isNull() ? null : node.asString();
  }

  private static Object toJava(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isBoolean()) {
      return node.booleanValue();
    }
    if (node.isIntegralNumber()) {
      return node.longValue();
    }
    if (node.isNumber()) {
      return node.decimalValue();
    }
    if (node.isArray()) {
      List<Object> list = new ArrayList<>();
      node.forEach(e -> list.add(toJava(e)));
      return list;
    }
    if (node.isObject()) {
      Map<String, Object> map = new LinkedHashMap<>();
      node.properties().forEach(e -> map.put(e.getKey(), toJava(e.getValue())));
      return map;
    }
    return node.asString();
  }
}

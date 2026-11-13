# Changelog

## Version `0.3.0-SNAPSHOT`

### 🌟 Features

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

## Version `0.2.0`

### 🌟 Features

- `fortras4j`: value rules (`rules`) restrict a field's values depending on other fields of the record
- `fortras4j`: field type `datetime` (`LocalDateTime`) with `pattern`
- `fortras4j`: `FortrasReader.widthsIn(charset)` and `FortrasWriter.widthsIn(charset)` count positions, lengths and `maxLineLength` in bytes of a charset
- `ais-scan4j`: status codes per scan type and the timestamp pattern come from the layout; new `statusCodesFor(scanType)`
- `ais-scan4j`: `AisScans.of(schemas, layoutId)` for a layout that extends the bundled one
- `ais-scan4j`: `AisScans.widthsIn(charset)` for byte widths
- `xml-status4j`: `XmlStatusConfig` (builder or YAML) and `XmlStatusCodec` configure charset, declaration, indentation, line breaks in values, DOCTYPE handling and element names; `XmlStatusFormat` keeps the behaviour of 0.1.0

### 💥 Breaking Changes

- `ais-scan4j`: removed `AisScans.allowedStatusCodes(String)`; use `statusCodesFor(scanType)` on an instance

### 🐞 Bug Fixes

- `fortras4j`: a boolean field with blank text is written as an empty field instead of failing with a `NullPointerException`

### 🔨 Dependency Upgrades

## Version `0.1.0`

### 🌟 Features

- `fortras4j`: layout-driven reader and writer for FORTRAS and other fixed-width files
- `ais-scan4j`: reader and writer for AIS scans
- `xml-status4j`: model, reader and writer for XML status files

### 🐞 Bug Fixes

### 🔨 Dependency Upgrades

- Upgraded tools.jackson:jackson-bom from 3.2.4 to 3.3.0
- Upgraded org.apache.maven.plugins:maven-compiler-plugin from 3.15.0 to 3.16.0

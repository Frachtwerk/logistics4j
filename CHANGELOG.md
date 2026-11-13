# Changelog

## Version `0.2.0-SNAPSHOT`

### 🌟 Features

- `fortras4j`: value rules in the layout (`rules`) restrict a field's values depending on other fields of the record
- `fortras4j`: field type `datetime` (`LocalDateTime`) with `pattern`
- `ais-scan4j`: allowed status codes per scan type and the timestamp pattern come from the layout; new `statusCodesFor(scanType)`, static `allowedStatusCodes` is deprecated
- `xml-status4j`: `XmlStatusConfig` (builder or YAML) and `XmlStatusCodec` make the charset and XML
  declaration, indentation, line breaks in values, DOCTYPE handling and element names configurable;
  `XmlStatusFormat` keeps the behaviour of 0.1.0

### 🐞 Bug Fixes

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

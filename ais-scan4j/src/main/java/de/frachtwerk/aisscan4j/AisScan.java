package de.frachtwerk.aisscan4j;

import java.time.LocalDateTime;

/**
 * One AIS scan. Values that are empty in the file are {@code null}.
 *
 * @param scanType FBS, HES, LBH, HBS, FES or LBD
 * @param hub hub, e.g. MUB
 * @param issuer person or device that caused the scan; empty for tour statuses
 * @param statusCode allowed values depend on the scan type, see {@link
 *     AisScans#allowedStatusCodes(String)}
 */
public record AisScan(
    Integer version,
    String scanType,
    String relation,
    String hub,
    Integer vehicleNumber,
    String barcode,
    String issuer,
    Integer statusCode,
    LocalDateTime timestamp,
    String additionalText) {}

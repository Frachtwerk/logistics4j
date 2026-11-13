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

/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.profile;

import java.util.List;

/**
 * Minecraft-free signature payload interned and consumed on the main thread. Airflow,
 * material contact, and radiation remain independent channels.
 */
public record ResolvedThermalSignature(
        int mediumId,
        int materialProfileId,
        List<LocalAirRegionPattern> airRegions,
        int materialContactPatternId,
        int radiationOcclusionPatternId,
        int sourceProfileId,
        int gateKind,
        int flags
) {
    public ResolvedThermalSignature {
        requireId("mediumId", mediumId);
        requireId("materialProfileId", materialProfileId);
        requireId("materialContactPatternId", materialContactPatternId);
        requireId("radiationOcclusionPatternId", radiationOcclusionPatternId);
        requireId("sourceProfileId", sourceProfileId);
        requireId("gateKind", gateKind);
        airRegions = List.copyOf(airRegions);

        long occupiedMicrocells = 0L;
        for (int expectedId = 0; expectedId < airRegions.size(); expectedId++) {
            LocalAirRegionPattern region = airRegions.get(expectedId);
            if (region.localRegionId() != expectedId) {
                throw new IllegalArgumentException("local air region IDs must be dense and ordered");
            }
            if ((occupiedMicrocells & region.provenAirMicrocellMask()) != 0L) {
                throw new IllegalArgumentException("local air regions must not overlap");
            }
            occupiedMicrocells |= region.provenAirMicrocellMask();
        }
    }

    public int localAirRegionCount() {
        return airRegions.size();
    }

    private static void requireId(String name, int value) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be a non-negative int ID");
        }
    }
}

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.profile.LocalAirRegionPattern;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;

import java.util.List;

public final class ThermalTestFixtures {
    private static final int TEST_ARENA_LIMIT = 1_000_000;

    private ThermalTestFixtures() {
    }

    public static ThermalCellArena.BrickAllocation regularBrick(
            ThermalCellArena arena,
            int pageSlot,
            int generation,
            int minX,
            int minY,
            int minZ,
            double totalCapacityJPerK,
            double initialTemperatureC,
            double referenceTemperatureC
    ) {
        ThermalCellArena.BrickCellLayout layout =
                new ThermalCellArena.BrickCellLayout();
        layout.reset(minX, minY, minZ);
        layout.setRegularAir(0, 0, totalCapacityJPerK / 64.0D);
        ThermalCellArena.BrickAllocation allocation = arena.stageBrickCells(
                pageSlot,
                generation,
                layout,
                initialTemperatureC,
                referenceTemperatureC,
                TEST_ARENA_LIMIT);
        arena.commitStagedCells(allocation.cellSpan());
        return allocation;
    }

    public static ThermalCellArena.BrickAllocation phaseBrick(
            ThermalCellArena arena,
            int pageSlot,
            int generation,
            int minX,
            int minY,
            int minZ,
            double airCapacityJPerK,
            int phaseProfileId,
            long candidateMask,
            double transitionTemperatureC,
            double transitionEnergyJ,
            double referenceTemperatureC
    ) {
        ThermalCellArena.BrickCellLayout layout =
                new ThermalCellArena.BrickCellLayout();
        layout.reset(minX, minY, minZ);
        layout.setRegularAir(0, 0, airCapacityJPerK / 64.0D);
        layout.addPhaseReservoir(
                minX,
                minY,
                minZ,
                phaseProfileId,
                candidateMask,
                transitionTemperatureC,
                transitionEnergyJ);
        ThermalCellArena.BrickAllocation allocation = arena.stageBrickCells(
                pageSlot,
                generation,
                layout,
                referenceTemperatureC,
                referenceTemperatureC,
                TEST_ARENA_LIMIT);
        arena.commitStagedCells(allocation.cellSpan());
        return allocation;
    }

    public static PageSignatures filledPageSignatures(int signatureId) {
        PageSignatures.Builder builder = new PageSignatures.Builder();
        for (int block = 0; block < PageSignatures.ENTRY_COUNT; block++) {
            builder.set(block, signatureId);
        }
        return builder.build();
    }

    public static ResolvedThermalSignature fullAirSignature() {
        return new ResolvedThermalSignature(
                0,
                0,
                List.of(new LocalAirRegionPattern(
                        0, -1L,
                        0xffff, 0xffff, 0xffff,
                        0xffff, 0xffff, 0xffff)),
                0, 0, 0, 0, 0);
    }

    public static ResolvedThermalSignature solidSignature() {
        return new ResolvedThermalSignature(
                0, 0, List.of(), 0, 0, 0, 0, 0);
    }
}

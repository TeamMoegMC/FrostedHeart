/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureCatalog;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;

import java.util.Arrays;

/** Stateless Brick-local enthalpy and phase-state migration kernel. */
final class BrickMigrationKernel {
    private final ThermalCellArena arena;
    private final ThermalSignatureCatalog signatures;
    private final ThermalTopologyParameters parameters;

    BrickMigrationKernel(
            ThermalCellArena arena,
            ThermalSignatureCatalog signatures,
            ThermalTopologyParameters parameters
    ) {
        this.arena = arena;
        this.signatures = signatures;
        this.parameters = parameters;
    }

    void migrate(
            WorkerPageStore.PageState page,
            int brick,
            WorkerBrickTopology old,
            WorkerBrickTopology next,
            PageSignatures nextSignatures
    ) {
        if (next.span.count() == 0) {
            return;
        }
        int count = next.span.count();
        double[] enthalpy = new double[count];
        double[] overlapCapacity = new double[count];
        for (int offset = 0; offset < count; offset++) {
            enthalpy[offset] = arena.enthalpyJ(
                    next.span.firstSlot() + offset);
        }
        if (old.span.count() != 0) {
            Arrays.fill(enthalpy, 0.0D);
            migrateAir(
                    page,
                    brick,
                    old,
                    next,
                    nextSignatures,
                    enthalpy,
                    overlapCapacity);
            migrateMaterial(old, next, enthalpy);
            migratePhase(old, next, enthalpy);
        } else if (page.dormantAir != null
                && page.dormantAir.hasBrick(brick)) {
            restoreDormant(page.dormantAir, brick, next, enthalpy);
        }
        for (int offset = 0; offset < count; offset++) {
            arena.stageEnthalpyJ(
                    next.span.firstSlot() + offset,
                    enthalpy[offset]);
        }
    }

    private void migrateAir(
            WorkerPageStore.PageState page,
            int brick,
            WorkerBrickTopology old,
            WorkerBrickTopology next,
            PageSignatures nextSignatures,
            double[] enthalpy,
            double[] overlapCapacity
    ) {
        double microCapacity =
                parameters.effectiveAirCapacityJPerBlockK() / 64.0D;
        for (int block = 0; block < 64; block++) {
            for (int microcell = 0; microcell < 64; microcell++) {
                int oldSlot = airSlot(
                        old, page.signatures, brick, block, microcell);
                int newSlot = airSlot(
                        next, nextSignatures, brick, block, microcell);
                if (oldSlot < 0 || newSlot < 0) {
                    continue;
                }
                int offset = newSlot - next.span.firstSlot();
                double oldOffset = arena.enthalpyJ(oldSlot)
                        * arena.inverseCapacityKPerJ(oldSlot);
                enthalpy[offset] += oldOffset * microCapacity;
                overlapCapacity[offset] += microCapacity;
            }
        }
        double initialOffset = page.naturalTemperatureC
                - parameters.referenceTemperatureC();
        for (int offset = 0; offset < enthalpy.length; offset++) {
            int slot = next.span.firstSlot() + offset;
            if (arena.isMaterialPole(slot) || arena.isPhaseReservoir(slot)) {
                enthalpy[offset] = arena.enthalpyJ(slot);
                continue;
            }
            double added = Math.max(
                    0.0D,
                    arena.capacityJPerK(slot) - overlapCapacity[offset]);
            enthalpy[offset] += added * initialOffset;
        }
    }

    private void restoreDormant(
            ThermalInputBatch.DormantAirCut cut,
            int brick,
            WorkerBrickTopology next,
            double[] enthalpy
    ) {
        if (next.coverageSlot < 0) {
            return;
        }
        int airComponents = next.mixedGeometry == null
                ? 1 : next.mixedGeometry.componentCount();
        int first = next.span.firstSlot();
        for (int component = 0; component < airComponents; component++) {
            int slot = next.coverageSlot + component;
            double temperature = cut.componentTemperatureC(
                    brick, component, airComponents);
            enthalpy[slot - first] = (temperature
                    - parameters.referenceTemperatureC())
                    * arena.capacityJPerK(slot);
        }
        double materialTemperature = cut.meanTemperatureC(brick);
        for (int slot : next.materialPoles.slot()) {
            enthalpy[slot - first] = (materialTemperature
                    - parameters.referenceTemperatureC())
                    * arena.capacityJPerK(slot);
        }
    }

    private int airSlot(
            WorkerBrickTopology topology,
            PageSignatures pageSignatures,
            int brick,
            int block,
            int microcell
    ) {
        if (!topology.cellsResolved || topology.coverageSlot < 0) {
            return -1;
        }
        int localX = ((brick & 3) << 2) + (block & 3);
        int localZ = ((brick >>> 2 & 3) << 2)
                + (block >>> 2 & 3);
        int localY = ((brick >>> 4 & 3) << 2)
                + (block >>> 4 & 3);
        int signatureId = pageSignatures.get(
                localX | localZ << 4 | localY << 8);
        int region = signatures.componentOrdinal(signatureId, microcell);
        if (region == 0xff) {
            return -1;
        }
        if (topology.mixedGeometry == null) {
            return topology.coverageSlot;
        }
        int component = topology.mixedGeometry.compiledComponentAt(
                block, region);
        return component < 0
                ? -1
                : topology.coverageSlot + component;
    }

    private void migrateMaterial(
            WorkerBrickTopology old,
            WorkerBrickTopology next,
            double[] enthalpy
    ) {
        for (int nextIndex = 0;
             nextIndex < next.materialPoles.size();
             nextIndex++) {
            int oldIndex = findMaterial(
                    old.materialPoles, next.materialPoles, nextIndex);
            if (oldIndex < 0) {
                continue;
            }
            int oldSlot = old.materialPoles.slot()[oldIndex];
            int newSlot = next.materialPoles.slot()[nextIndex];
            enthalpy[newSlot - next.span.firstSlot()] =
                    arena.enthalpyJ(oldSlot)
                            * arena.inverseCapacityKPerJ(oldSlot)
                            * arena.capacityJPerK(newSlot);
        }
    }

    private static int findMaterial(
            WorkerBrickTopology.MaterialPoles old,
            WorkerBrickTopology.MaterialPoles next,
            int nextIndex
    ) {
        for (int index = 0; index < old.size(); index++) {
            if (old.blockX()[index] == next.blockX()[nextIndex]
                    && old.blockY()[index] == next.blockY()[nextIndex]
                    && old.blockZ()[index] == next.blockZ()[nextIndex]
                    && old.profileId()[index] == next.profileId()[nextIndex]) {
                return index;
            }
        }
        return -1;
    }

    private void migratePhase(
            WorkerBrickTopology old,
            WorkerBrickTopology next,
            double[] enthalpy
    ) {
        for (int nextIndex = 0;
             nextIndex < next.phaseReservoirs.size();
             nextIndex++) {
            int oldIndex = findPhase(
                    old.phaseReservoirs, next.phaseReservoirs, nextIndex);
            if (oldIndex < 0) {
                continue;
            }
            int oldSlot = old.phaseReservoirs.slot()[oldIndex];
            int newSlot = next.phaseReservoirs.slot()[nextIndex];
            arena.copyPhaseRequestState(oldSlot, newSlot);
            enthalpy[newSlot - next.span.firstSlot()] =
                    arena.enthalpyJ(newSlot);
        }
    }

    private static int findPhase(
            WorkerBrickTopology.PhaseReservoirs old,
            WorkerBrickTopology.PhaseReservoirs next,
            int nextIndex
    ) {
        for (int index = 0; index < old.size(); index++) {
            if (old.brickMinX()[index] == next.brickMinX()[nextIndex]
                    && old.brickMinY()[index] == next.brickMinY()[nextIndex]
                    && old.brickMinZ()[index] == next.brickMinZ()[nextIndex]
                    && old.profileId()[index] == next.profileId()[nextIndex]) {
                return index;
            }
        }
        return -1;
    }
}

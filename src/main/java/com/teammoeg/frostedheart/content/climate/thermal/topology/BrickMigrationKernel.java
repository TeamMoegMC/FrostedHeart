/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;

import java.util.Arrays;

/** Reusable Brick-local enthalpy and phase-state migration kernel. */
final class BrickMigrationKernel {
    private final ThermalCellArena arena;
    private final ThermalSignatureTable signatures;
    private final ThermalTopologyParameters parameters;
    private double[] enthalpyScratch = new double[0];
    private double[] overlapCapacityScratch = new double[0];

    BrickMigrationKernel(
            ThermalCellArena arena,
            ThermalSignatureTable signatures,
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
            PageSignatures nextSignatures,
            boolean sameLifecycle
    ) {
        if (next.span.count() == 0) {
            return;
        }
        int count = next.span.count();
        boolean migrateCommitted = old.span.count() != 0;
        boolean restoreDormant = !migrateCommitted
                && page.dormantAir != null
                && page.dormantAir.hasBrick(brick);
        if (!migrateCommitted && !restoreDormant) {
            return;
        }
        ensureScratch(count);
        double[] enthalpy = enthalpyScratch;
        double[] overlapCapacity = overlapCapacityScratch;
        if (migrateCommitted) {
            Arrays.fill(enthalpy, 0, count, 0.0D);
            Arrays.fill(overlapCapacity, 0, count, 0.0D);
            migrateAir(
                    page,
                    brick,
                    old,
                    next,
                    nextSignatures,
                    enthalpy,
                    overlapCapacity);
            migrateMaterial(old, next, enthalpy);
            if (sameLifecycle) {
                migratePhase(old, next, enthalpy);
            }
        } else {
            for (int offset = 0; offset < count; offset++) {
                enthalpy[offset] = arena.enthalpyJ(
                        next.span.firstSlot() + offset);
            }
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
        if (old.coverageSlot >= 0 && next.coverageSlot >= 0) {
            if (old.mixedGeometry == null && next.mixedGeometry == null) {
                migrateRegularAir(
                        old, next, enthalpy, overlapCapacity);
            } else {
                double microCapacity =
                        parameters.effectiveAirCapacityJPerBlockK() / 64.0D;
                migrateMixedAir(
                        page.signatures, nextSignatures, brick,
                        old, next, microCapacity,
                        enthalpy, overlapCapacity);
            }
        }
        double initialOffset = page.naturalTemperatureC
                - parameters.referenceTemperatureC();
        for (int offset = 0; offset < next.span.count(); offset++) {
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

    private void migrateMixedAir(
            PageSignatures oldSignatures,
            PageSignatures nextSignatures,
            int brick,
            WorkerBrickTopology old,
            WorkerBrickTopology next,
            double microCapacity,
            double[] enthalpy,
            double[] overlapCapacity
    ) {
        for (int block = 0; block < 64; block++) {
            int oldSignature = signatureAt(
                    oldSignatures, brick, block);
            int nextSignature = signatureAt(
                    nextSignatures, brick, block);
            long remaining = signatures.airMask(oldSignature)
                    & signatures.airMask(nextSignature);
            while (remaining != 0L) {
                int microcell = Long.numberOfTrailingZeros(remaining);
                remaining &= remaining - 1L;
                int oldSlot = airSlot(
                        old, oldSignature, block, microcell);
                int newSlot = airSlot(
                        next, nextSignature, block, microcell);
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
    }

    private void migrateRegularAir(
            WorkerBrickTopology old,
            WorkerBrickTopology next,
            double[] enthalpy,
            double[] overlapCapacity
    ) {
        int offset = next.coverageSlot - next.span.firstSlot();
        double nextCapacity = arena.capacityJPerK(next.coverageSlot);
        enthalpy[offset] = arena.enthalpyJ(old.coverageSlot)
                * arena.inverseCapacityKPerJ(old.coverageSlot)
                * nextCapacity;
        overlapCapacity[offset] = nextCapacity;
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
            int signatureId,
            int block,
            int microcell
    ) {
        if (!topology.cellsResolved || topology.coverageSlot < 0) {
            return -1;
        }
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

    private static int signatureAt(
            PageSignatures pageSignatures,
            int brick,
            int block
    ) {
        int localX = ((brick & 3) << 2) + (block & 3);
        int localZ = ((brick >>> 2 & 3) << 2)
                + (block >>> 2 & 3);
        int localY = ((brick >>> 4 & 3) << 2)
                + (block >>> 4 & 3);
        return pageSignatures.get(localX | localZ << 4 | localY << 8);
    }

    private void ensureScratch(int count) {
        if (enthalpyScratch.length >= count) {
            return;
        }
        int capacity = Math.max(count, Math.max(4, enthalpyScratch.length * 2));
        enthalpyScratch = new double[capacity];
        overlapCapacityScratch = new double[capacity];
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
                    arena.enthalpyJ(oldSlot);
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

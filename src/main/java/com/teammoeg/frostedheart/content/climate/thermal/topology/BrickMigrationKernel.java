/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockBrickLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch.MaterialChanges;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/** Move whole-block enthalpy to a replacement layout without changing phase request identity. */
final class BrickMigrationKernel {
    private final ThermalCellArena arena;
    private final ThermalSignatureTable signatures;
    private final ThermalTopologyParameters parameters;
    private final MaterialBoundaryRegistry materials;
    private final double[] nodeEnthalpiesJ = new double[64], blockTemperaturesC = new double[64];
    private final double[] changedMaterialEnergy = new double[64];
    private final byte[] changedMaterialBranch = new byte[64];
    double externalMaterialEnergyJ;
    private final MaterialSample restoredMaterial = new MaterialSample();

    BrickMigrationKernel(
            ThermalCellArena arena,
            ThermalSignatureTable signatures,
            ThermalTopologyParameters parameters,
            MaterialBoundaryRegistry materials) {
        this.arena = arena;
        this.signatures = signatures;
        this.parameters = parameters;
        this.materials = materials;
    }

    void migrate(
            WorkerPageStore.PageState page,
            int brick,
            WorkerBrickTopology oldBrick,
            WorkerBrickTopology newBrick,
            PageSignatures nextSignatures,
            boolean sameLifecycle,
            MaterialChanges changes,
            IntArrayList changeIndexes,
            ThermalInputBatch.DormantMaterialCut dormantMaterials,
            double initialTemperatureC,
            boolean resetMaterials) {
        externalMaterialEnergyJ = 0;
        int nodeCount = newBrick.span.count(), firstSlot = newBrick.span.firstSlot();
        if (nodeCount == 0) {
            for (int slot = oldBrick.span.firstSlot();
                    slot < oldBrick.span.endSlotExclusive();
                    slot++) {
                if (arena.materialLaw(slot) != null)
                    externalMaterialEnergyJ -= arena.enthalpyJ(slot);
            }
            return;
        }
        if (oldBrick.span.count() == 0
                && dormantMaterials == null
                && (page.dormantAir == null || !page.dormantAir.hasBrick(brick))) return;
        if (oldBrick.coverageSlot >= 0
                && newBrick.coverageSlot >= 0
                && oldBrick.blockLayout == null
                && newBrick.blockLayout == null) {
            arena.stageEnthalpyJ(newBrick.coverageSlot, arena.enthalpyJ(oldBrick.coverageSlot));
            return;
        }
        for (int i = 0; i < nodeCount; i++) nodeEnthalpiesJ[i] = arena.enthalpyJ(firstSlot + i);
        long changedMaterials =
                resetMaterials
                        ? 0
                        : applyMaterialChanges(
                                oldBrick, changes, changeIndexes, initialTemperatureC);
        if (oldBrick.span.count() > 0) {
            for (int blockIndex = 0; blockIndex < 64; blockIndex++) {
                int oldSlot = oldBrick.slotAt(blockIndex), newSlot = newBrick.slotAt(blockIndex);
                if (resetMaterials) {
                    if (oldSlot >= 0 && arena.materialLaw(oldSlot) != null)
                        externalMaterialEnergyJ -= arena.enthalpyJ(oldSlot);
                    if (newSlot >= 0 && arena.materialLaw(newSlot) != null) {
                        externalMaterialEnergyJ += arena.enthalpyJ(newSlot);
                        continue;
                    }
                }
                if (newSlot >= 0
                        && arena.materialLaw(newSlot) != null
                        && (changedMaterials & 1L << blockIndex) != 0) {
                    nodeEnthalpiesJ[newSlot - firstSlot] = changedMaterialEnergy[blockIndex];
                    arena.stageMaterialState(
                            newSlot,
                            changedMaterialEnergy[blockIndex],
                            changedMaterialBranch[blockIndex]);
                    continue;
                }
                if (oldSlot < 0 || newSlot < 0) continue;
                int pageBlockIndex = BlockBrickLayout.pageBlock(brick, blockIndex);
                if (arena.materialLaw(oldSlot) != null && arena.materialLaw(newSlot) != null) {
                    var transition = arena.materialTransition(oldSlot);
                    if (arena.materialTransitionAcknowledged(oldSlot)
                            && transition != null
                            && transition.targetStateId()
                                    == signatures.materialStateId(
                                            nextSignatures.get(pageBlockIndex))) {
                        nodeEnthalpiesJ[newSlot - firstSlot] = arena.enthalpyJ(oldSlot);
                        continue;
                    }
                }
                if (signatures.materialProfileId(page.signatures.get(pageBlockIndex))
                        != signatures.materialProfileId(nextSignatures.get(pageBlockIndex)))
                    continue;
                if (!signatures.sameMaterialType(
                        page.signatures.get(pageBlockIndex), nextSignatures.get(pageBlockIndex)))
                    continue;
                double previousEnergyPerBlockJ =
                        arena.enthalpyJ(oldSlot)
                                / Long.bitCount(
                                        oldBrick.nodeMask(oldSlot - oldBrick.span.firstSlot()));
                double initialEnergyPerBlockJ =
                        arena.enthalpyJ(newSlot)
                                / Long.bitCount(newBrick.nodeMask(newSlot - firstSlot));
                nodeEnthalpiesJ[newSlot - firstSlot] +=
                        previousEnergyPerBlockJ - initialEnergyPerBlockJ;
            }
            if (sameLifecycle && !resetMaterials)
                migratePhase(oldBrick, newBrick, nodeEnthalpiesJ, changedMaterials);
        } else if (page.dormantAir != null && page.dormantAir.hasBrick(brick)) {
            page.dormantAir.fillBlockTemperatures(brick, blockTemperaturesC);
            for (int i = 0; i < nodeCount; i++) {
                int slot = firstSlot + i;
                if (!arena.isAirCell(slot)) continue;
                double temperatureC = page.dormantAir.meanTemperatureC(brick);
                if (i < newBrick.airNodeCount) {
                    long mask = newBrick.nodeMask(i);
                    double temperatureSumC = 0;
                    int count = Long.bitCount(mask);
                    while (mask != 0) {
                        int blockIndex = Long.numberOfTrailingZeros(mask);
                        mask &= mask - 1;
                        temperatureSumC += blockTemperaturesC[blockIndex];
                    }
                    temperatureC = temperatureSumC / count;
                }
                nodeEnthalpiesJ[i] =
                        (temperatureC - parameters.referenceTemperatureC())
                                * arena.capacityJPerK(slot);
            }
        }
        if (!resetMaterials && oldBrick.span.count() == 0 && dormantMaterials != null) {
            for (int block = 0; block < 64; block++) {
                int slot = newBrick.slotAt(block);
                if (slot < 0 || arena.materialLaw(slot) == null) continue;
                int position = BlockBrickLayout.pageBlock(brick, block);
                int stored = dormantMaterials.state().find(position);
                if (stored < 0
                        || dormantMaterials.state().stateId(stored)
                                != signatures.materialStateId(nextSignatures.get(position)))
                    continue;
                dormantMaterials
                        .state()
                        .read(
                                stored,
                                arena.materialLaw(slot),
                                dormantMaterials.tick(),
                                dormantMaterials.naturalC(),
                                dormantMaterials.coolingRate(),
                                restoredMaterial);
                nodeEnthalpiesJ[slot - firstSlot] = restoredMaterial.enthalpyJ();
                arena.stageMaterialState(
                        slot, restoredMaterial.enthalpyJ(), restoredMaterial.branch());
            }
        }
        for (int i = 0; i < nodeCount; i++) arena.stageEnthalpyJ(firstSlot + i, nodeEnthalpiesJ[i]);
    }

    private void migratePhase(
            WorkerBrickTopology oldBrick,
            WorkerBrickTopology newBrick,
            double[] nodeEnthalpiesJ,
            long changedMaterials) {
        // Preserve pending requests only for the same body, law and Page lifecycle.
        for (int newSlot : newBrick.phaseSlots) {
            int block =
                    (arena.minimum(newSlot, 0) & 3)
                            | (arena.minimum(newSlot, 2) & 3) << 2
                            | (arena.minimum(newSlot, 1) & 3) << 4;
            if ((changedMaterials & 1L << block) != 0) continue;
            int profile = arena.materialProfileId(newSlot);
            for (int oldSlot : oldBrick.phaseSlots) {
                if (arena.materialProfileId(oldSlot) == profile
                        && arena.minimum(oldSlot, 0) == arena.minimum(newSlot, 0)
                        && arena.minimum(oldSlot, 1) == arena.minimum(newSlot, 1)
                        && arena.minimum(oldSlot, 2) == arena.minimum(newSlot, 2)) {
                    arena.copyPhaseRequestState(oldSlot, newSlot);
                    if (arena.materialLaw(newSlot) != null) {
                        arena.stageMaterialState(
                                newSlot, arena.enthalpyJ(oldSlot), arena.materialBranch(oldSlot));
                    }
                    nodeEnthalpiesJ[newSlot - newBrick.span.firstSlot()] = arena.enthalpyJ(oldSlot);
                    break;
                }
            }
        }
    }

    private long applyMaterialChanges(
            WorkerBrickTopology oldBrick,
            MaterialChanges changes,
            IntArrayList indexes,
            double naturalTemperatureC) {
        if (indexes == null || indexes.isEmpty() || oldBrick.span.count() == 0) return 0;
        long changed = 0;
        for (int cursor = 0; cursor < indexes.size(); cursor++) {
            int event = indexes.getInt(cursor);
            int pageBlock = changes.blockIndex(event);
            int block = (pageBlock & 3) | (pageBlock >>> 4 & 3) << 2 | (pageBlock >>> 8 & 3) << 4;
            long bit = 1L << block;
            if ((changed & bit) == 0) {
                int oldSlot = oldBrick.slotAt(block);
                changedMaterialEnergy[block] =
                        oldSlot >= 0 && arena.materialLaw(oldSlot) != null
                                ? arena.enthalpyJ(oldSlot)
                                : 0;
                changedMaterialBranch[block] =
                        oldSlot >= 0 ? arena.materialBranch(oldSlot) : MaterialThermalLaw.SENSIBLE;
                changed |= bit;
            }
            MaterialThermalLaw before = law(changes.previousSignature(event));
            MaterialThermalLaw after = law(changes.nextSignature(event));
            double previousEnergy = changedMaterialEnergy[block];
            if (changes.cause(event) == MaterialChanges.REPLACE || before == null) {
                changedMaterialEnergy[block] =
                        after == null ? 0 : after.enthalpyAtTemperature(naturalTemperatureC);
                changedMaterialBranch[block] = MaterialThermalLaw.SENSIBLE;
            } else if (after == null) {
                changedMaterialEnergy[block] = 0;
                changedMaterialBranch[block] = MaterialThermalLaw.SENSIBLE;
            } else if (changes.cause(event) == MaterialChanges.MASS_CHANGE) {
                changedMaterialEnergy[block] =
                        before.afterMassChange(
                                changedMaterialEnergy[block], after, naturalTemperatureC);
                changedMaterialBranch[block] = MaterialThermalLaw.SENSIBLE;
            } else if (changes.cause(event) == MaterialChanges.GAMEPLAY_TRANSITION) {
                changedMaterialEnergy[block] =
                        after.enthalpyAtTemperature(
                                before.temperatureC(
                                        changedMaterialEnergy[block],
                                        changedMaterialBranch[block]));
                changedMaterialBranch[block] = MaterialThermalLaw.SENSIBLE;
            } else if (changes.cause(event) == MaterialChanges.THERMAL_TRANSITION) {
                changedMaterialBranch[block] = MaterialThermalLaw.SENSIBLE;
            }
            externalMaterialEnergyJ += changedMaterialEnergy[block] - previousEnergy;
        }
        return changed;
    }

    private MaterialThermalLaw law(int signature) {
        var profile = materials.profileOrNull(signatures.materialProfileId(signature));
        return profile == null ? null : profile.thermalLaw();
    }
}

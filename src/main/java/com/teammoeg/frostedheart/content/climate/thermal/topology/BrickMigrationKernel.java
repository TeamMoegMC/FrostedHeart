/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.*;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch.MaterialChanges;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;

/** Bounded whole-block enthalpy migration; phase requests keep their identity. */
final class BrickMigrationKernel {
    private final ThermalCellArena arena;
    private final ThermalSignatureTable signatures;
    private final ThermalTopologyParameters parameters;
    private final MaterialBoundaryRegistry materials;
    private final double[] enthalpy=new double[64], temperatures=new double[64];
    private final double[] changedMaterialEnergy = new double[64];
    private final byte[] changedMaterialBranch = new byte[64];
    double externalMaterialEnergyJ;
    private final QueryPublication.MutableMaterialSample restoredMaterial = new QueryPublication.MutableMaterialSample();
    BrickMigrationKernel(ThermalCellArena arena,ThermalSignatureTable signatures,ThermalTopologyParameters parameters,
            MaterialBoundaryRegistry materials) {
        this.arena=arena; this.signatures=signatures; this.parameters=parameters; this.materials=materials;
    }
    void migrate(WorkerPageStore.PageState page,int brick,WorkerBrickTopology old,WorkerBrickTopology next,
            PageSignatures nextSignatures,boolean sameLifecycle, MaterialChanges changes, IntArrayList changeIndexes,
            ThermalInputBatch.DormantMaterialCut dormantMaterials, double initialTemperatureC, boolean resetMaterials) {
        externalMaterialEnergyJ = 0;
        int n=next.span.count(), first=next.span.firstSlot();
        if(n==0) {
            for (int slot = old.span.firstSlot(); slot < old.span.endSlotExclusive(); slot++) {
                if (arena.materialLaw(slot) != null) externalMaterialEnergyJ -= arena.enthalpyJ(slot);
            }
            return;
        }
        if (old.span.count() == 0 && dormantMaterials == null
                && (page.dormantAir == null || !page.dormantAir.hasBrick(brick))) return;
        if (old.coverageSlot >= 0 && next.coverageSlot >= 0
                && old.blockLayout == null && next.blockLayout == null) {
            arena.stageEnthalpyJ(next.coverageSlot, arena.enthalpyJ(old.coverageSlot));
            return;
        }
        for(int i=0;i<n;i++) enthalpy[i]=arena.enthalpyJ(first+i);
        long changedMaterials = resetMaterials ? 0 : applyMaterialChanges(old, changes, changeIndexes, initialTemperatureC);
        if(old.span.count()>0) {
            for(int b=0;b<64;b++) {
                int os=old.slotAt(b), ns=next.slotAt(b);
                if (resetMaterials) {
                    if (os >= 0 && arena.materialLaw(os) != null) externalMaterialEnergyJ -= arena.enthalpyJ(os);
                    if (ns >= 0 && arena.materialLaw(ns) != null) {
                        externalMaterialEnergyJ += arena.enthalpyJ(ns);
                        continue;
                    }
                }
                if (ns >= 0 && arena.materialLaw(ns) != null && (changedMaterials & 1L << b) != 0) {
                    enthalpy[ns - first] = changedMaterialEnergy[b];
                    arena.stageMaterialState(ns, changedMaterialEnergy[b], changedMaterialBranch[b]);
                    continue;
                }
                if(os<0 || ns<0) continue;
                int pb=BlockBrickLayout.pageBlock(brick,b);
                if (arena.materialLaw(os) != null && arena.materialLaw(ns) != null) {
                    var transition = arena.materialTransition(os);
                    if (arena.materialTransitionAcknowledged(os) && transition != null
                            && transition.targetStateId() == signatures.materialStateId(nextSignatures.get(pb))) {
                        enthalpy[ns - first] = arena.enthalpyJ(os);
                        continue;
                    }
                }
                if(signatures.materialProfileId(page.signatures.get(pb))!=signatures.materialProfileId(nextSignatures.get(pb))) continue;
                if (!signatures.sameMaterialType(page.signatures.get(pb), nextSignatures.get(pb))) continue;
                double previous=arena.enthalpyJ(os)/Long.bitCount(old.nodeMask(os-old.span.firstSlot()));
                double initial=arena.enthalpyJ(ns)/Long.bitCount(next.nodeMask(ns-first));
                enthalpy[ns-first]+=previous-initial;
            }
            if(sameLifecycle && !resetMaterials) migratePhase(old,next,enthalpy,changedMaterials);
        } else if(page.dormantAir!=null && page.dormantAir.hasBrick(brick)) {
            page.dormantAir.fillBlockTemperatures(brick,temperatures);
            for(int i=0;i<n;i++) {
                int slot=first+i;
                if(!arena.isAirCell(slot)) continue;
                double t=page.dormantAir.meanTemperatureC(brick);
                if(i<next.transportNodeCount) {
                    long mask=next.nodeMask(i); double total=0;
                    int count=Long.bitCount(mask);
                    while(mask!=0) { int b=Long.numberOfTrailingZeros(mask);mask&=mask-1;total+=temperatures[b]; }
                    t=total/count;
                }
                enthalpy[i]=(t-parameters.referenceTemperatureC())*arena.capacityJPerK(slot);
            }
        }
        if (!resetMaterials && old.span.count() == 0 && dormantMaterials != null) {
            for (int block = 0; block < 64; block++) {
                int slot = next.slotAt(block);
                if (slot < 0 || arena.materialLaw(slot) == null) continue;
                int position = BlockBrickLayout.pageBlock(brick, block);
                int stored = dormantMaterials.state().find(position);
                if (stored < 0 || dormantMaterials.state().stateId(stored) != signatures.materialStateId(nextSignatures.get(position))) continue;
                dormantMaterials.state().read(stored, arena.materialLaw(slot), dormantMaterials.tick(),
                        dormantMaterials.naturalC(), dormantMaterials.coolingRate(), restoredMaterial);
                enthalpy[slot - first] = restoredMaterial.enthalpyJ();
                arena.stageMaterialState(slot, restoredMaterial.enthalpyJ(), restoredMaterial.branch());
            }
        }
        for(int i=0;i<n;i++) arena.stageEnthalpyJ(first+i,enthalpy[i]);
    }
    private void migratePhase(
            WorkerBrickTopology old,
            WorkerBrickTopology next,
            double[] enthalpy,
            long changedMaterials
    ) {
        // Preserve pending requests only for the same body, law and Page lifecycle.
        for (int newSlot : next.phaseSlots) {
            int block = (arena.minimum(newSlot, 0) & 3) | (arena.minimum(newSlot, 2) & 3) << 2
                    | (arena.minimum(newSlot, 1) & 3) << 4;
            if ((changedMaterials & 1L << block) != 0) continue;
            int profile = arena.materialProfileId(newSlot);
            for (int oldSlot : old.phaseSlots) {
                if (arena.materialProfileId(oldSlot) == profile
                        && arena.minimum(oldSlot, 0) == arena.minimum(newSlot, 0)
                        && arena.minimum(oldSlot, 1) == arena.minimum(newSlot, 1)
                        && arena.minimum(oldSlot, 2) == arena.minimum(newSlot, 2)) {
                    arena.copyPhaseRequestState(oldSlot, newSlot);
                    if (arena.materialLaw(newSlot) != null) {
                        arena.stageMaterialState(newSlot, arena.enthalpyJ(oldSlot), arena.materialBranch(oldSlot));
                    }
                    enthalpy[newSlot - next.span.firstSlot()] = arena.enthalpyJ(oldSlot);
                    break;
                }
            }
        }
    }

    private long applyMaterialChanges(WorkerBrickTopology old, MaterialChanges changes,
            IntArrayList indexes, double naturalTemperatureC) {
        if (indexes == null || indexes.isEmpty() || old.span.count() == 0) return 0;
        long changed = 0;
        for (int cursor = 0; cursor < indexes.size(); cursor++) {
            int event = indexes.getInt(cursor);
            int pageBlock = changes.blockIndex(event);
            int block = (pageBlock & 3) | (pageBlock >>> 4 & 3) << 2 | (pageBlock >>> 8 & 3) << 4;
            long bit = 1L << block;
            if ((changed & bit) == 0) {
                int oldSlot = old.slotAt(block);
                changedMaterialEnergy[block] = oldSlot >= 0 && arena.materialLaw(oldSlot) != null
                        ? arena.enthalpyJ(oldSlot) : 0;
                changedMaterialBranch[block] = oldSlot >= 0 ? arena.materialBranch(oldSlot) : MaterialThermalLaw.SENSIBLE;
                changed |= bit;
            }
            MaterialThermalLaw before = law(changes.previousSignature(event));
            MaterialThermalLaw after = law(changes.nextSignature(event));
            double previousEnergy = changedMaterialEnergy[block];
            if (changes.cause(event) == MaterialChanges.REPLACE || before == null) {
                changedMaterialEnergy[block] = after == null ? 0 : after.enthalpyAtTemperature(naturalTemperatureC);
                changedMaterialBranch[block] = MaterialThermalLaw.SENSIBLE;
            } else if (after == null) {
                changedMaterialEnergy[block] = 0;
                changedMaterialBranch[block] = MaterialThermalLaw.SENSIBLE;
            } else if (changes.cause(event) == MaterialChanges.MASS_CHANGE) {
                changedMaterialEnergy[block] = before.afterMassChange(changedMaterialEnergy[block], after, naturalTemperatureC);
                changedMaterialBranch[block] = MaterialThermalLaw.SENSIBLE;
            } else if (changes.cause(event) == MaterialChanges.GAMEPLAY_TRANSITION) {
                changedMaterialEnergy[block] = after.enthalpyAtTemperature(
                        before.temperatureC(changedMaterialEnergy[block], changedMaterialBranch[block]));
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

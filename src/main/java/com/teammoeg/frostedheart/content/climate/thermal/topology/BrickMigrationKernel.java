/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.*;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;

/** Bounded whole-block enthalpy migration; phase requests keep their identity. */
final class BrickMigrationKernel {
    private final ThermalCellArena arena;
    private final ThermalSignatureTable signatures;
    private final ThermalTopologyParameters parameters;
    private final double[] enthalpy=new double[64], temperatures=new double[64];
    BrickMigrationKernel(ThermalCellArena arena,ThermalSignatureTable signatures,ThermalTopologyParameters parameters) {
        this.arena=arena; this.signatures=signatures; this.parameters=parameters;
    }
    void migrate(WorkerPageStore.PageState page,int brick,WorkerBrickTopology old,WorkerBrickTopology next,
            PageSignatures nextSignatures,boolean sameLifecycle) {
        int n=next.span.count(), first=next.span.firstSlot();
        if(n==0) return;
        if (old.span.count() == 0 && (page.dormantAir == null || !page.dormantAir.hasBrick(brick))) return;
        if (old.coverageSlot >= 0 && next.coverageSlot >= 0
                && old.blockLayout == null && next.blockLayout == null) {
            arena.stageEnthalpyJ(next.coverageSlot, arena.enthalpyJ(old.coverageSlot));
            return;
        }
        for(int i=0;i<n;i++) enthalpy[i]=arena.enthalpyJ(first+i);
        if(old.span.count()>0) {
            for(int b=0;b<64;b++) {
                int os=old.slotAt(b), ns=next.slotAt(b);
                if(os<0 || ns<0 || arena.isPhaseReservoir(os) || arena.isPhaseReservoir(ns)) continue;
                int pb=BlockBrickLayout.pageBlock(brick,b);
                if(signatures.materialProfileId(page.signatures.get(pb))!=signatures.materialProfileId(nextSignatures.get(pb))) continue;
                double previous=arena.enthalpyJ(os)/Long.bitCount(old.nodeMask(os-old.span.firstSlot()));
                double initial=arena.enthalpyJ(ns)/Long.bitCount(next.nodeMask(ns-first));
                enthalpy[ns-first]+=previous-initial;
            }
            if(sameLifecycle) migratePhase(old,next,enthalpy);
        } else if(page.dormantAir!=null && page.dormantAir.hasBrick(brick)) {
            page.dormantAir.fillBlockTemperatures(brick,temperatures);
            for(int i=0;i<n;i++) {
                int slot=first+i;
                if(arena.isPhaseReservoir(slot)) continue;
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
        for(int i=0;i<n;i++) arena.stageEnthalpyJ(first+i,enthalpy[i]);
    }
    private void migratePhase(
            WorkerBrickTopology old,
            WorkerBrickTopology next,
            double[] enthalpy
    ) {
        // The caller already fixes Brick position and Page lifecycle. Each profile
        // has exactly one reservoir in that Brick; its identity lives in the arena.
        for (int newSlot : next.phaseSlots) {
            int profile = arena.phaseProfileId(newSlot);
            for (int oldSlot : old.phaseSlots) {
                if (arena.phaseProfileId(oldSlot) == profile) {
                    arena.copyPhaseRequestState(oldSlot, newSlot);
                    enthalpy[newSlot - next.span.firstSlot()] = arena.enthalpyJ(oldSlot);
                    break;
                }
            }
        }
    }
}

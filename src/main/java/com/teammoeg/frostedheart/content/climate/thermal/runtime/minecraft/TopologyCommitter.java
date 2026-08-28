/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSolver;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;

/** Stateless allocation-free authoritative write order for one prepared delta. */
final class TopologyCommitter {
    void commit(
            PreparedTopologyChange change,
            WorkerPageStore pages,
            ThermalCellArena arena,
            ThermalSolver solver,
            PhaseTransitionRuntime phases
    ) {
        if (solver.structuralVersion() != change.baseStructuralVersion) {
            throw new IllegalStateException(
                    "prepared topology base version is no longer current");
        }
        for (int slot : change.removedReservoirSlots) {
            if (!arena.isLive(slot) || !arena.isPhaseReservoir(slot)) {
                throw new IllegalStateException(
                        "prepared removed phase reservoir is no longer current");
            }
        }
        for (int slot : change.addedReservoirSlots) {
            if (!arena.isStagedCell(slot)
                    || !arena.isPhaseReservoir(slot)) {
                throw new IllegalStateException(
                        "prepared added phase reservoir is not staged");
            }
        }
        for (PreparedTopologyChange.PageWrite write : change.pageWrites) {
            if (!pages.canCommit(write.page, write.admission)) {
                throw new IllegalStateException(
                        "prepared Page ownership is no longer current");
            }
            for (int index = 0; index < write.brickIndexes.length; index++) {
                if ((write.stagedBrickMask
                        & 1L << write.brickIndexes[index]) != 0L
                        && !arena.ownsStagedCells(
                                write.bricks[index].span)) {
                    throw new IllegalStateException(
                            "prepared Brick staging span is no longer current");
                }
            }
        }
        for (PreparedTopologyChange.PageWrite write : change.pageWrites) {
            for (int index = 0; index < write.brickIndexes.length; index++) {
                if ((write.stagedBrickMask
                        & 1L << write.brickIndexes[index]) != 0L) {
                    arena.commitStagedCells(write.bricks[index].span);
                }
            }
        }
        for (int slot : change.removedReservoirSlots) {
            phases.unregisterReservoir(slot);
        }
        for (int index = 0; index < change.fragmentIndexes.length; index++) {
            solver.installFragment(
                    change.fragmentIndexes[index],
                    change.fragments[index]);
        }
        for (int index = 0; index < change.materialEdgeKeys.length; index++) {
            solver.installMaterialEdge(
                    change.materialEdgeKeys[index],
                    change.materialEdges[index]);
        }
        for (int index = 0;
             index < change.materialExecutionFragments.length;
             index++) {
            solver.installMaterialExecution(
                    change.materialExecutionFragments[index],
                    change.materialExecutions[index]);
        }
        for (PreparedTopologyChange.PageWrite write : change.pageWrites) {
            if (write.admission) {
                pages.commitAdmission(write.page);
            }
            if (write.retirement) {
                continue;
            }
            for (int index = 0; index < write.brickIndexes.length; index++) {
                pages.installBrick(
                        write.page,
                        write.brickIndexes[index],
                        write.bricks[index]);
            }
            if (write.naturalTemperatureChanged) {
                pages.installNaturalTemperature(
                        write.page, write.naturalTemperatureC);
                solver.installNaturalTemperature(
                        write.page.pageSlot, write.naturalTemperatureC);
            }
            for (int index = 0; index < write.skyColumns.length; index++) {
                pages.installSkyColumn(
                        write.page,
                        Short.toUnsignedInt(write.skyColumns[index]),
                        write.firstExposedLocalY[index]);
            }
            pages.installPageState(
                    write.page,
                    write.signatures,
                    write.publication.geometryRevision(),
                    write.publication.topologyGeneration(),
                    write.resolvedBrickMask,
                    write.publication);
        }
        for (int slot : change.addedReservoirSlots) {
            phases.registerReservoir(slot);
        }
        if (change.nextStructuralVersion != change.baseStructuralVersion) {
            solver.finishTopologyCommit(change.nextStructuralVersion);
        }
        for (PreparedTopologyChange.PageWrite write : change.pageWrites) {
            if (write.retirement) {
                pages.commitRetirement(write.page);
                solver.clearNaturalTemperature(write.page.pageSlot);
                write.page.handle.publish(
                        com.teammoeg.frostedheart.content.climate.thermal.mesh
                                .PagePublication.EMPTY);
            } else {
                write.page.handle.publish(write.publication);
            }
        }
    }

    void releaseOldSpans(
            PreparedTopologyChange change,
            ThermalCellArena arena,
            ThermalSolver solver,
            ThermalSourceLedger sources
    ) {
        for (PreparedTopologyChange.OldSpan old : change.oldSpans) {
            if (solver.references(old.span())) {
                throw new IllegalStateException(
                        "old topology span remains in the installed solver");
            }
            for (int slot = old.span().firstSlot();
                 slot < old.span().endSlotExclusive();
                 slot++) {
                if (sources.referencesThermalNode(
                        slot, old.lifecycleGeneration())) {
                    throw new IllegalStateException(
                        "old topology span remains source-bound");
                }
            }
            if (!arena.ownsLiveCells(
                    old.pageSlot(),
                    old.lifecycleGeneration(),
                    old.span())) {
                throw new IllegalStateException(
                        "old topology span is no longer owned by its Page");
            }
        }
        for (PreparedTopologyChange.OldSpan old : change.oldSpans) {
            arena.releasePageCells(
                    old.pageSlot(),
                    old.lifecycleGeneration(),
                    old.span());
        }
    }
}

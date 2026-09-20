/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSolver;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;

/**
 * 按固定顺序安装一个 {@link PreparedTopologyChange} 的无状态提交器。
 *
 * <p>所有可能失败的计算必须在 plan 阶段结束；这里仅更新已预留的权威结构，
 * 发布新引用，最后释放旧 span。</p>
 */
public final class TopologyCommitter {
    private TopologyCommitter() {
    }

    public static void commit(
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
        for (int slot : change.removedPhaseSlots) {
            if (!arena.isLive(slot) || !arena.hasMaterialTransition(slot)) {
                throw new IllegalStateException(
                        "prepared removed phase material is no longer current");
            }
        }
        for (int slot : change.addedPhaseSlots) {
            if (!arena.isStagedCell(slot)
                    || !arena.hasMaterialTransition(slot)) {
                throw new IllegalStateException(
                        "prepared added phase material is not staged");
            }
        }
        for (PreparedTopologyChange.PageWrite write : change.pageWrites) {
            if (!pages.canCommit(
                    write.page, write.replacedPage, write.admission)) {
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
        for (int slot : change.removedPhaseSlots) {
            phases.unregisterMaterial(slot);
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
                    write.residentBrickMask,
                    write.sourceSeedMask,
                    write.publication);
        }
        for (int slot : change.addedPhaseSlots) {
            phases.registerMaterial(slot);
        }
        arena.recordExternalMaterialEnergy(change.externalMaterialEnergyJ);
        if (change.nextStructuralVersion != change.baseStructuralVersion) {
            solver.finishTopologyCommit(change.nextStructuralVersion);
        }
        for (PreparedTopologyChange.PageWrite write : change.pageWrites) {
            if (write.retirement) {
                pages.commitRetirement(write.page);
                solver.clearNaturalTemperature(write.page.pageSlot);
            }
        }
        for (PreparedTopologyChange.PageWrite write : change.pageWrites) {
            if (write.retirement) {
                write.page.handle.publish(
                        com.teammoeg.frostedheart.content.climate.thermal.mesh
                                .PagePublication.EMPTY);
            } else {
                if (write.replacedPage != null) {
                    write.replacedPage.handle.publish(
                            com.teammoeg.frostedheart.content.climate.thermal.mesh
                                    .PagePublication.EMPTY);
                }
                write.page.handle.publish(write.publication);
            }
        }
    }

    /**
     * Restore only the Page references when numeric publication fails. This does not roll back
     * the installed solver; the failed engine is closed and restarted by its owner.
     */
    public static void restorePagePublications(PreparedTopologyChange change) {
        for (PreparedTopologyChange.PageWrite write : change.pageWrites) {
            write.page.handle.publish(write.rollbackPublication);
            if (write.replacedPage != null) {
                write.replacedPage.handle.publish(write.replacedPage.publication);
            }
        }
    }

    /** Source rebinding must finish before these unreferenced slots can be returned to the arena. */
    public static void releaseOldSpans(
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
        }
        for (PreparedTopologyChange.OldSpan old : change.oldSpans) {
            arena.releasePageCells(
                    old.pageSlot(),
                    old.lifecycleGeneration(),
                    old.span());
        }
    }
}

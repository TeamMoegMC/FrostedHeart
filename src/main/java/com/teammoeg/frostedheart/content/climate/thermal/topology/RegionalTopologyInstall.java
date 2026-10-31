/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSolver;

/** Private staging and retirement for one local topology exchange. No partially installed view is visible. */
public final class RegionalTopologyInstall {
    public final InstallToken token = new InstallToken();
    private final PreparedTopologyChange change;
    private final WorkerPageStore pages;
    private final ThermalCellArena arena;
    private final ThermalSolver solver;
    private final PhaseTransitionRuntime phases;
    private final WorkerPageStore.PageState[] stagedPages;
    private int phase, index, brick, cleanupPhase, cleanupIndex, cleanupBrick;
    private boolean valid = true, prepared;

    public RegionalTopologyInstall(PreparedTopologyChange change, WorkerPageStore pages, ThermalCellArena arena,
            ThermalSolver solver, PhaseTransitionRuntime phases) {
        this.change = change; this.pages = pages; this.arena = arena; this.solver = solver; this.phases = phases;
        stagedPages = new WorkerPageStore.PageState[change.pageWrites.length];
    }
    public boolean valid() { return valid; }
    /** Claims are owned by the containing GeometryWork until cleanup completes. */
    public boolean prepare(long deadline) {
        if (prepared || !valid) return true;
        do {
            switch (phase) {
                case 0 -> {
                    if (index == change.pageWrites.length) { nextPhase(); break; }
                    var write = change.pageWrites[index++];
                    var current = pages.pageAt(write.page.handle.sectionKey());
                    var expected = write.replacedPage != null ? write.replacedPage : write.admission ? null : write.page;
                    if (current != expected || expected != null && expected.publication != write.expectedPublication) { valid = false; return true; }
                }
                case 1 -> {
                    if (index == change.removedPhaseSlots.length) { nextPhase(); break; }
                    int slot = change.removedPhaseSlots[index++];
                    if (!arena.isLive(slot) || !arena.hasMaterialTransition(slot)) { valid = false; return true; }
                }
                case 2 -> {
                    if (index == change.addedPhaseSlots.length) { nextPhase(); break; }
                    int slot = change.addedPhaseSlots[index++];
                    if (!arena.isStagedCell(slot) || !arena.hasMaterialTransition(slot)) { valid = false; return true; }
                }
                case 3 -> {
                    if (index == change.pageWrites.length) { nextPhase(); break; }
                    var write = change.pageWrites[index];
                    if (brick == write.brickIndexes.length) { brick = 0; index++; break; }
                    int local = brick++;
                    if ((write.stagedBrickMask & 1L << write.brickIndexes[local]) != 0
                            && !arena.ownsStagedCells(write.bricks[local].span)) { valid = false; return true; }
                }
                case 4 -> {
                    if (index == change.pageWrites.length) { nextPhase(); break; }
                    var write = change.pageWrites[index];
                    if (brick == write.brickIndexes.length) { brick = 0; index++; break; }
                    int local = brick++;
                    if ((write.stagedBrickMask & 1L << write.brickIndexes[local]) != 0)
                        arena.stageVisibility(write.bricks[local].span, token, true);
                }
                case 5 -> {
                    if (index == change.oldSpans.length) { nextPhase(); break; }
                    arena.stageVisibility(change.oldSpans[index++].span(), token, false);
                }
                case 6 -> {
                    if (index == change.fragmentIndexes.length) { nextPhase(); break; }
                    solver.stageFragment(change.fragmentIndexes[index], change.fragments[index], token); index++;
                }
                case 7 -> {
                    if (index == change.removedPhaseSlots.length) { nextPhase(); break; }
                    phases.stageMaterial(change.removedPhaseSlots[index++], false, token);
                }
                case 8 -> {
                    if (index == change.addedPhaseSlots.length) { nextPhase(); break; }
                    phases.stageMaterial(change.addedPhaseSlots[index++], true, token);
                }
                case 9 -> {
                    if (index == change.pageWrites.length) { prepared = true; return true; }
                    stagedPages[index] = pages.stagePage(change.pageWrites[index], token); index++;
                }
                default -> throw new IllegalStateException("Unknown topology preparation phase");
            }
        } while (System.nanoTime() < deadline);
        return false;
    }
    private void nextPhase() { phase++; index = brick = 0; }

    /** Called inside the query/runtime exchange after their preparation has succeeded. */
    public void commit() {
        if (!prepared || !valid) throw new IllegalStateException("Topology exchange was not prepared");
        arena.recordExternalMaterialEnergy(change.externalMaterialEnergyJ);
        if (change.nextStructuralVersion != change.baseStructuralVersion) solver.finishTopologyCommit(solver.structuralVersion() + 1);
        // New regional owners capture environment during this same exchange.
        // Installing it in cleanup leaves their first boundary inputs as NaN.
        for (int index = 0; index < change.pageWrites.length; index++) if (stagedPages[index] != null) {
            var write = change.pageWrites[index];
            if (write.retirement) solver.clearNaturalTemperature(write.page.pageSlot);
            else solver.installNaturalTemperature(write.page.pageSlot, stagedPages[index].naturalTemperatureC);
        }
        token.commit();
    }
    public void cancel() { token.cancel(); }

    /** Old spans remain pinned until source successors and every actual geometry reader release them. */
    public boolean cleanup(long deadline) {
        do {
            switch (cleanupPhase) {
                case 0 -> {
                    if (cleanupIndex == change.fragmentIndexes.length) { nextCleanup(); break; }
                    solver.finishFragment(change.fragmentIndexes[cleanupIndex], token); cleanupIndex++;
                }
                case 1 -> {
                    if (cleanupIndex == change.pageWrites.length) { nextCleanup(); break; }
                    var write = change.pageWrites[cleanupIndex];
                    if (cleanupBrick == write.brickIndexes.length) { cleanupBrick = 0; cleanupIndex++; break; }
                    int local = cleanupBrick++;
                    if ((write.stagedBrickMask & 1L << write.brickIndexes[local]) != 0)
                        arena.finishVisibility(write.bricks[local].span, token, true);
                }
                case 2 -> {
                    if (cleanupIndex == change.removedPhaseSlots.length) { nextCleanup(); break; }
                    phases.finishMaterial(change.removedPhaseSlots[cleanupIndex++], false, token);
                }
                case 3 -> {
                    if (cleanupIndex == change.addedPhaseSlots.length) { nextCleanup(); break; }
                    phases.finishMaterial(change.addedPhaseSlots[cleanupIndex++], true, token);
                }
                case 4 -> {
                    if (cleanupIndex == change.pageWrites.length) { nextCleanup(); break; }
                    var write = change.pageWrites[cleanupIndex];
                    if (stagedPages[cleanupIndex] != null) {
                        pages.finishPage(stagedPages[cleanupIndex], write, token);
                    }
                    cleanupIndex++;
                }
                case 5 -> {
                    if (cleanupIndex == change.oldSpans.length) { nextCleanup(); break; }
                    var old = change.oldSpans[cleanupIndex];
                    if (token.committed()) {
                        if (solver.references(old.span())) throw new IllegalStateException("Retired span still belongs to a committed fragment");
                    }
                    arena.finishVisibility(old.span(), token, false);
                    if (token.committed()) arena.releasePageCells(old.pageSlot(), old.lifecycleGeneration(), old.span());
                    cleanupIndex++;
                }
                default -> { return true; }
            }
        } while (System.nanoTime() < deadline);
        return false;
    }
    private void nextCleanup() { cleanupPhase++; cleanupIndex = cleanupBrick = 0; }
}

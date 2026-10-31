/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.topology.RegionalContinuationBoundary;
import java.util.IdentityHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

/** Read-only witness of two real structural tasks; never postpones either task. */
public final class StructuralIsolationObservation {
    private record Active(long sequence, long tick, boolean remote) {}
    private static final IdentityHashMap<Object, Active> active = new IdentityHashMap<>();
    private static BlockPos local;
    private static int remoteX;
    private static boolean issued, crossed;
    private static long localSequence, remoteSequence;

    public static synchronized void arm(BlockPos localPoint, BlockPos remotePoint) {
        active.clear(); local = localPoint.immutable(); remoteX = remotePoint.getX();
        issued = crossed = false; localSequence = remoteSequence = -1;
    }

    public static synchronized void advancing(Object task, ThermalInputBatch input, RegionalContinuationBoundary boundary, String stage) {
        if (local == null) return;
        boolean preparing = switch (stage) {
            case "MATERIAL_INPUT", "MATERIAL_SETTLE", "TOPOLOGY", "REGIONS", "PUBLICATION" -> true;
            default -> false;
        };
        if (!preparing) return;
        boolean remote = false;
        for (long section : boundary.sections()) remote |= Math.abs((SectionPos.x(section) << 4) - remoteX) < 100;
        var previous = active.get(task);
        if (previous == null || previous.sequence != input.sequence() || previous.tick != input.targetTick())
            active.put(task, new Active(input.sequence(), input.targetTick(), remote));
    }

    public static synchronized void published(Object task, ThermalInputBatch input) {
        Active done = active.remove(task);
        if (local == null || !issued || done == null || done.remote) return;
        long section = SectionPos.asLong(local.getX() >> 4, local.getY() >> 4, local.getZ() >> 4);
        boolean touches = false;
        for (var page : input.admissions()) touches |= page.page().sectionKey() == section;
        for (var update : input.residencyUpdates()) touches |= update.page().sectionKey() == section;
        for (int i = 0; i < input.geometry().size(); i++) touches |= input.geometry().page(i).sectionKey() == section;
        if (!touches) return;
        for (var unfinished : active.values()) if (unfinished.remote && unfinished.sequence < input.sequence()) {
            crossed = true; localSequence = input.sequence(); remoteSequence = unfinished.sequence;
            FHMain.LOGGER.info("THERMAL_STRUCTURE_INDEPENDENT_COMMIT,localSequence={},localTick={},unfinishedSequence={},unfinishedTick={}",
                    localSequence, input.targetTick(), remoteSequence, unfinished.tick);
            return;
        }
        long pendingTick = RegionalBoundaryObservation.unfinishedRemoteTick();
        if (pendingTick < input.targetTick()) {
            crossed = true; localSequence = input.sequence(); remoteSequence = -1;
            FHMain.LOGGER.info("THERMAL_STRUCTURE_INDEPENDENT_COMMIT,localSequence={},localTick={},unfinishedTick={},witness=pending_input",
                    localSequence, input.targetTick(), pendingTick);
        }
    }

    public static synchronized void finished(Object task) { active.remove(task); }

    public static synchronized boolean takeWorldAction() {
        if (local == null || issued) return false;
        for (var task : active.values()) if (task.remote) { issued = true; return true; }
        return false;
    }
    public static synchronized boolean crossed() { return crossed; }
    public static synchronized String summary() {
        return "actionIssued=" + issued + ",crossed=" + crossed + ",localSequence=" + localSequence + ",unfinishedSequence=" + remoteSequence;
    }
    public static synchronized void end() { local = null; active.clear(); }
}

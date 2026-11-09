/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

/** Reads real coordinator state without changing its scheduling or numerical result. */
public final class CheckpointBoundaryProbe {
    private CheckpointBoundaryProbe() { }

    public static int jobs(RegionalAirRuntime runtime) {
        int count = 0;
        for (var region : runtime.observedRegions()) if (region.job != null) count++;
        return count;
    }

    public static boolean allHeldThrough(RegionalAirRuntime runtime, long boundary) {
        if (runtime.observedRegions().isEmpty()) return false;
        for (var region : runtime.observedRegions()) if (region.executionLimit() > boundary) return false;
        return true;
    }
}

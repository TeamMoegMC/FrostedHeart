/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

/** Small worker-owned cost history. Contains no field, source receipt or clock authority. */
final class RegionWorkPolicy {
    private static final int WINDOW_CUTS = 8;
    private static final long FIRST_RETRY_TICKS = 600, MAX_RETRY_TICKS = 9600;
    private long fineBuildWork, coarseBuildWork, advanceWork, windowWork;
    private long retryAfterTick = Long.MAX_VALUE, retryDelayTicks = FIRST_RETRY_TICKS;
    private int windowCuts, cheapWindows, installedStep = 1;
    private boolean coarseRequested, recovering;

    RegionWorkPolicy() {}
    RegionWorkPolicy(RegionWorkPolicy previous) {
        fineBuildWork = previous.fineBuildWork; coarseBuildWork = previous.coarseBuildWork;
        advanceWork = previous.advanceWork; windowWork = previous.windowWork;
        retryAfterTick = previous.retryAfterTick; retryDelayTicks = previous.retryDelayTicks;
        windowCuts = previous.windowCuts; cheapWindows = previous.cheapWindows;
        installedStep = previous.installedStep; coarseRequested = previous.coarseRequested;
        recovering = previous.recovering;
    }
    int select(int currentStep, long tick, long buildLimit) {
        if (currentStep == 1) return coarseRequested || fineBuildWork > buildLimit ? 4 : 1;
        return recoveryDue(tick) ? 1 : 4;
    }
    private boolean recoveryDue(long tick) { return cheapWindows >= 2 && tick >= retryAfterTick; }
    void coarse(long tick, boolean failedRecovery) {
        coarseRequested = true; cheapWindows = windowCuts = 0; windowWork = 0;
        if (failedRecovery || recovering) retryDelayTicks = Math.min(MAX_RETRY_TICKS, retryDelayTicks * 2);
        retryAfterTick = tick + retryDelayTicks;
        recovering = false;
    }
    void built(int step, long cost) {
        if (step == 1) fineBuildWork = cost;
        else coarseBuildWork = cost;
    }
    boolean installed(int step, long tick) {
        if (step == installedStep) return false;
        installedStep = step; cheapWindows = windowCuts = 0; windowWork = 0;
        if (step == 1) { coarseRequested = false; recovering = true; }
        else if (retryAfterTick == Long.MAX_VALUE || retryAfterTick <= tick) retryAfterTick = tick + retryDelayTicks;
        return true;
    }
    void advanced(int step, long cost, long allowance, long tick) {
        windowWork += cost;
        if (++windowCuts < WINDOW_CUTS) return;
        advanceWork = windowWork / windowCuts; windowWork = 0; windowCuts = 0;
        cheapWindows = advanceWork < allowance / 2 ? Math.min(2, cheapWindows + 1) : 0;
        if (step == 1) {
            if (advanceWork > allowance) coarse(tick, recovering);
            else if (cheapWindows == 2) { retryDelayTicks = FIRST_RETRY_TICKS; recovering = false; }
        } else if (advanceWork >= allowance) {
            retryAfterTick = Math.max(retryAfterTick, tick + retryDelayTicks);
        }
    }
    String summary() {
        return "fineBuildWorkNs=" + fineBuildWork + ",coarseBuildWorkNs=" + coarseBuildWork
                + ",advanceMeanWorkNs=" + advanceWork + ",retryAfterTick=" + retryAfterTick
                + ",retryDelayTicks=" + retryDelayTicks + ",cheapWindows=" + cheapWindows;
    }
}

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope;
import com.teammoeg.frostedheart.content.climate.thermal.solver.RegionalAirRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import java.util.IdentityHashMap;

/** Transport receipt, Page intake retirement and physics are distinct observed lifetimes. */
public final class QueuedInputObservation {
    private static boolean armed;
    private static long accepted, acknowledged, payloadReleases, generalWhileRunning, yieldedRounds, earlyReceipts, lastSequence;
    private static double maximumImbalanceJ;
    private static String failure;
    private static net.minecraft.server.level.ServerLevel actionLevel;
    private static Runnable captureAction;
    private static long captureActionDeadline;
    private static int captureActions;
    private static final IdentityHashMap<Object, Long> structures = new IdentityHashMap<>();
    public static synchronized void arm() {
        armed = true; accepted = acknowledged = payloadReleases = generalWhileRunning = yieldedRounds = earlyReceipts = lastSequence = 0;
        maximumImbalanceJ = 0; failure = null; structures.clear(); actionLevel = null; captureAction = null; captureActions = 0;
    }
    public static synchronized void accepted(ThermalInputEnvelope batch, boolean unfinished) {
        if (!armed) return;
        check(batch.sequence() > lastSequence, "An immutable envelope was admitted twice or out of order");
        lastSequence = batch.sequence(); accepted++;
        if (unfinished && (batch.pageInputs().length > 0 || batch.sourceWindows().length > 0)) generalWhileRunning++;
    }
    public static synchronized void retired(long sequence, long section) { if (armed) payloadReleases++; }
    public static synchronized void acknowledged(long sequence, boolean unfinished) {
        if (!armed) return;
        acknowledged++; if (unfinished) earlyReceipts++;
    }
    public static synchronized void round(RegionalAirRuntime air, ThermalSourceLedger ledger, boolean pending) {
        if (!armed) return;
        if (pending) yieldedRounds++;
        var balance = ledger.energyBalance();
        double imbalance = balance.outputJ() - balance.deliveredJ() - balance.declaredLossJ()
                - balance.degradedLossJ() - balance.unacceptedJ() - balance.exportedJ() - air.pendingEnergyJ();
        maximumImbalanceJ = Math.max(maximumImbalanceJ, Math.abs(imbalance));
        check(Math.abs(imbalance) < 1e-6, "Retained world input changed the once-only energy balance");
    }
    public static synchronized void structure(Object task, long tick, boolean completed) {
        if (!armed) return;
        if (completed) structures.remove(task); else structures.put(task, tick);
    }
    private static void check(boolean value, String message) { if (!value && failure == null) failure = message; }
    public static synchronized boolean observedAckDecoupling() { return earlyReceipts > 0; }
    public static synchronized boolean observedYield() { return yieldedRounds > 0; }
    public static synchronized boolean processingStructureSince(long tick) {
        for (long active : structures.values()) if (active >= tick) return true;
        return false;
    }
    public static synchronized void requestCaptureAction(net.minecraft.server.level.ServerLevel level, long deadline, Runnable action) {
        actionLevel = level; captureActionDeadline = deadline; captureAction = action;
    }
    public static synchronized Runnable takeCaptureAction(net.minecraft.server.level.ServerLevel level) {
        if (!armed || captureAction == null || actionLevel != level || level.getGameTime() > captureActionDeadline) return null;
        Runnable action = captureAction; captureAction = null; captureActions++; return action;
    }
    public static synchronized boolean observedGeneralWhileRunning() { return generalWhileRunning > 0; }
    public static synchronized long acknowledged() { return acknowledged; }
    public static synchronized long payloadReleases() { return payloadReleases; }
    public static synchronized String failure() { return failure; }
    public static synchronized String summary() { return "acceptedEnvelopes=" + accepted + ",transportReceipts=" + acknowledged
            + ",pagePayloadRetirements=" + payloadReleases + ",earlyReceipts=" + earlyReceipts + ",generalWhileRunning=" + generalWhileRunning
            + ",yieldedRounds=" + yieldedRounds + ",maxImbalanceJ=" + maximumImbalanceJ + ",captureActions=" + captureActions + ",failure=" + failure; }
    public static synchronized void end() { armed = false; structures.clear(); actionLevel = null; captureAction = null; }
}

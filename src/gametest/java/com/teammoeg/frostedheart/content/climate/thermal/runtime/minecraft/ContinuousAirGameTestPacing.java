/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.Util;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.online.GameTestNativeClockAccess;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.locks.LockSupport;

/** Real-clock capacity runs, or explicit lockstep acceleration for behavior comparisons. */
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ContinuousAirGameTestPacing {
    private static long nextTickNanos;
    private static long processingStartedNanos, lastProcessingNanos;
    private static final boolean LOCKSTEP = "lockstep".equalsIgnoreCase(
            System.getenv("FH_CONTINUOUS_AIR_TEST_CLOCK"));
    private static final boolean EXPLICIT_CLOCK = System.getenv("FH_CONTINUOUS_AIR_TEST_CLOCK") != null;
    private static final boolean LEGACY_NATIVE_CLOCK = "realtime20_legacy".equalsIgnoreCase(
            System.getenv("FH_CONTINUOUS_AIR_TEST_CLOCK"));
    private static boolean announced;
    private static long barrierWaitNanos;
    private static ServerProcessing processingEvent;
    private static long measurementStartedTick = -1;
    private static final jdk.jfr.EventType PROCESSING_EVENT_TYPE = jdk.jfr.EventType.getEventType(ServerProcessing.class);

    @jdk.jfr.Name("frostedheart.GameTestServerProcessing")
    @jdk.jfr.Label("Full Forge test tick excluding clock waits")
    @jdk.jfr.StackTrace(false)
    private static final class ServerProcessing extends jdk.jfr.Event {
        long gameTick;
        long measuredElapsedTick;
    }

    static long lastProcessingNanos() { return lastProcessingNanos; }
    static long barrierWaitNanos() { return barrierWaitNanos; }
    static String clockMode() { return LOCKSTEP ? "lockstep" : LEGACY_NATIVE_CLOCK ? "realtime20_legacy" : "realtime20"; }

    /** Scene construction can take seconds; start the measured cadence after it finishes. */
    static void beginMeasurement() {
        measurementStartedTick = -1;
        if (!LOCKSTEP) nextTickNanos = System.nanoTime() + 50_000_000L;
    }

    private ContinuousAirGameTestPacing() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if ((event.phase != TickEvent.Phase.START || !(event.getServer() instanceof GameTestServer))) return;
        if (!announced) {
            announced = true;
            FHMain.LOGGER.info("Continuous-Air GameTest clock: {}", clockMode());
        }
        if (LOCKSTEP) {
            long started = System.nanoTime();
            boolean settled;
            do {
                settled = true;
                for (var level : event.getServer().getAllLevels()) {
                    // Completion/ACK can finish without another game tick. Geometry recovery
                    // may need future ticks, so never wait for geometry_current here.
                    if (!MinecraftThermalInput.awaitSubmittedCut(level)) settled = false;
                }
                if (!settled) LockSupport.parkNanos(100_000L);
            } while (!settled);
            barrierWaitNanos += System.nanoTime() - started;
        } else {
            if (nextTickNanos == 0) nextTickNanos = System.nanoTime() + 50_000_000L;
            if (!LEGACY_NATIVE_CLOCK) {
                // Our END pacing drops catch-up debt. Vanilla must use the same
                // deadline or its optional unload queue can lose every spare tick.
                var clock = (GameTestNativeClockAccess) event.getServer();
                clock.thermalClock$deadlineMillis(Math.max(clock.thermalClock$deadlineMillis(), Util.getMillis() + 50L));
            }
        }
        processingStartedNanos = System.nanoTime();
        if (PROCESSING_EVENT_TYPE.isEnabled()) {
            processingEvent = new ServerProcessing();
            processingEvent.gameTick = event.getServer().overworld().getGameTime();
            if (measurementStartedTick < 0) measurementStartedTick = processingEvent.gameTick;
            processingEvent.measuredElapsedTick = processingEvent.gameTick - measurementStartedTick;
            processingEvent.begin();
        }
    }

    /** Include END listeners as well as START listeners, excluding our pacing wait. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onServerTickEnd(TickEvent.ServerTickEvent event) {
        if ((event.phase == TickEvent.Phase.END && event.getServer() instanceof GameTestServer)) {
            lastProcessingNanos = System.nanoTime() - processingStartedNanos;
            if (processingEvent != null) {
                processingEvent.end();
                processingEvent.commit();
                processingEvent = null;
            }
            if (!LOCKSTEP) {
                // Pacing must not consume the tick's native haveTime() budget before
                // ChunkMap processes its real save/unload queue.
                long remaining;
                while ((remaining = nextTickNanos - System.nanoTime()) > 0L) LockSupport.parkNanos(remaining);
                // GameTestServer.waitUntilNextTick drains tasks without the ordinary
                // server wait. Do not repay a native/VM stall with a burst of ticks:
                // that would compress an Air cut's 40-tick deadline below real time.
                nextTickNanos = Math.max(nextTickNanos + 50_000_000L, System.nanoTime());
            }
        }
    }
}

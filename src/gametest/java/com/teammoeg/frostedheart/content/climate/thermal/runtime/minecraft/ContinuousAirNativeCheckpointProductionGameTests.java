/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Actual early ticket removal and return; no fabricated heat or worker timing. */
@GameTestHolder("frostedheart_native_checkpoint")
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = FHMain.MODID)
public final class ContinuousAirNativeCheckpointProductionGameTests {
    private static Scenario observation;
    private static int scenarioIndex;

    @SubscribeEvent
    public static void onUnload(ChunkEvent.Unload event) {
        Scenario current = observation;
        if (current != null && event.getLevel() == current.helper.getLevel()
                && event.getChunk().getPos().equals(current.original.getPos())) current.unloaded = event.getChunk();
    }

    @GameTest(template = "phase0a_empty", batch = "native_early_unload", timeoutTicks = 900)
    public static void uncapturedInitialChunkUnloadsWithoutInventingThermalResult(GameTestHelper helper) throws java.io.IOException {
        start(helper, false, false);
    }

    @GameTest(template = "phase0a_empty", batch = "native_initial_geometry", timeoutTicks = 900)
    public static void initializedGeometryWaitsForItsRealThermalResult(GameTestHelper helper) throws java.io.IOException {
        start(helper, false, true);
    }

    @GameTest(template = "phase0a_empty", batch = "native_ticket_return", timeoutTicks = 900)
    public static void ticketReturnCancelsPendingThermalUnload(GameTestHelper helper) throws java.io.IOException {
        start(helper, true, false);
    }

    /** Real user load at the native queue boundary; does not hold or fabricate a checkpoint future. */
    public static void nativeRequestCreated(net.minecraft.server.level.ServerLevel level, ChunkAccess access) {
        Scenario current = observation;
        var chunk = access instanceof net.minecraft.world.level.chunk.ImposterProtoChunk imposter
                ? imposter.getWrapped() : access;
        if (current != null && current.returnWhilePending && !current.returned
                && level == current.helper.getLevel() && chunk == current.original
                && MinecraftThermalInput.observedNativeRequestForChunk(level, access)) {
            current.returnTickets(-1, "native_request_created");
            // A real synchronous chunk request processes vanilla's returned ticket
            // before the queued unload callback, even when the checkpoint is already ready.
            level.getChunkSource().getChunkFuture(chunk.getPos().x, chunk.getPos().z,
                    net.minecraft.world.level.chunk.ChunkStatus.FULL, true);
        }
    }

    public static void nativeUnloadLoop(net.minecraft.server.level.ServerLevel level, java.util.function.BooleanSupplier haveTime,
            it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap<net.minecraft.server.level.ChunkHolder> pending, int queueSize) {
        Scenario current = observation;
        if (current == null || current.finished || level != current.helper.getLevel()) return;
        current.unloadLoops++;
        if (haveTime.getAsBoolean()) current.spareTimeLoops++;
        if (pending.get(current.original.getPos().toLong()) == current.originalHolder) current.pendingHolderLoops++;
        current.maximumUnloadQueue = Math.max(current.maximumUnloadQueue, queueSize);
    }

    private static void start(GameTestHelper helper, boolean returnWhilePending, boolean waitForGeometry) throws java.io.IOException {
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        ContinuousAirPrecisionProductionGameTests.restoreReferenceClimate(helper, true);
        BlockPos origin = helper.absolutePos(new BlockPos(517 + 512 * scenarioIndex++, 0, 7));
        ContinuousAirTwoBaseProductionGameTests.forceBase(helper.getLevel(), origin, true);
        if (returnWhilePending) {
            // Finish actual vanilla terrain prerequisites before lighting fires.
            // Then first Page capture precedes the first heat publication, which
            // gives a real pending request that ticket return can cancel.
            for (int x = 0; x < 50; x += 15) for (int z = 0; z < 14; z += 13)
                helper.getLevel().getChunkAt(origin.offset(x, 0, z));
            var target = new net.minecraft.world.level.ChunkPos(origin.offset(47, 7, 11));
            helper.startSequence().thenWaitUntil(() -> {
                boolean ready = false;
                for (var holder : helper.getLevel().getChunkSource().chunkMap.getChunks())
                    if (holder.getPos().equals(target)) ready = holder.getChunkToSave().isDone();
                helper.assertTrue(ready, "Actual vanilla target terrain must be ready before this return scenario");
            }).thenExecute(() -> buildAndRun(helper, origin, true, true));
            return;
        }
        buildAndRun(helper, origin, returnWhilePending, waitForGeometry);
    }

    private static void buildAndRun(GameTestHelper helper, BlockPos origin, boolean returnWhilePending, boolean waitForGeometry) {
        var built = ContinuousAirLargeBaseProductionGameTests.buildBase(helper, origin);
        helper.assertTrue(MinecraftThermalInput.usesNativeCheckpointScheduling(helper.getLevel()),
                "Run the native checkpoint candidate on the actual configured backend");
        Scenario scenario = new Scenario(helper, origin, built.fires(), returnWhilePending, waitForGeometry);
        observation = scenario;
        FHMain.LOGGER.info("THERMAL_NATIVE_SCENE_READY,worldTick={},targetChunk={},returnWhilePending={}",
                helper.getLevel().getGameTime(), scenario.original.getPos(), returnWhilePending);
        if (!waitForGeometry) {
            ContinuousAirTwoBaseProductionGameTests.forceBase(helper.getLevel(), origin, false);
            scenario.dropRequested = true;
        }
        for (int tick = 1; tick <= 800; tick++) {
            int elapsed = tick;
            helper.runAfterDelay(tick, () -> scenario.tick(elapsed));
        }
    }

    private static final class Scenario {
        final GameTestHelper helper;
        final BlockPos origin;
        final BlockPos[] fires;
        final boolean returnWhilePending;
        final boolean waitForGeometry;
        final ChunkAccess original;
        final net.minecraft.server.level.ChunkHolder originalHolder;
        final long initialWait;
        ChunkAccess unloaded;
        boolean returned, finished, dropRequested;
        long unloadLoops, spareTimeLoops, pendingHolderLoops;
        int maximumUnloadQueue;

        Scenario(GameTestHelper helper, BlockPos origin, BlockPos[] fires, boolean returnWhilePending, boolean waitForGeometry) {
            this.helper = helper; this.origin = origin; this.fires = fires; this.returnWhilePending = returnWhilePending;
            this.waitForGeometry = waitForGeometry;
            original = helper.getLevel().getChunkAt(fires[127]);
            net.minecraft.server.level.ChunkHolder holder = null;
            for (var candidate : helper.getLevel().getChunkSource().chunkMap.getChunks())
                if (candidate.getPos().equals(original.getPos())) holder = candidate;
            originalHolder = holder;
            var waits = MinecraftThermalInput.observedCheckpointStatistics(helper.getLevel());
            initialWait = waits.saveWaitNanos() + waits.unloadWaitNanos();
        }

        void tick(int elapsed) {
            if (finished) return;
            var level = helper.getLevel();
            if (!dropRequested) {
                var chunk = (net.minecraft.world.level.chunk.LevelChunk) original;
                var owner = ((com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftThermalSectionAttachment)
                        (Object) chunk.getSections()[chunk.getSectionIndex(fires[127].getY())]).frostedheart$getThermalInputOwner();
                if (owner != null && owner.page() != null) {
                    ContinuousAirTwoBaseProductionGameTests.forceBase(level, origin, false);
                    dropRequested = true;
                    FHMain.LOGGER.info("THERMAL_NATIVE_GEOMETRY_DROP,elapsedTicks={},published={}", elapsed,
                            owner.page().lastPublication() != null);
                }
            }
            var nativeState = MinecraftThermalInput.observedNativeCheckpoints(level);
            if (returnWhilePending && !returned && MinecraftThermalInput.observedNativeRequestForChunk(level, original)) {
                returnTickets(elapsed, "next_tick");
            }
            boolean completed = returnWhilePending
                    ? returned && nativeState.cancelled() > 0 && nativeState.pending() == 0
                            && level.getChunkSource().getChunkNow(original.getPos().x, original.getPos().z) == original
                    : unloaded == original && nativeState.pending() == 0
                            && level.getChunkSource().getChunkNow(original.getPos().x, original.getPos().z) == null;
            if (!completed && (elapsed == 100 || elapsed == 300 || elapsed == 599)) {
                var cached = level.getChunkSource().getChunkNow(original.getPos().x, original.getPos().z);
                FHMain.LOGGER.info("THERMAL_NATIVE_UNLOAD_PROGRESS,elapsedTicks={},chunk={},returnWhilePending={},waitForGeometry={},ticketLevel={},saveFutureDone={},originalClass={},unloadedClass={},unloadedOriginal={},cachedOriginal={},cachedAbsent={},requests={},pending={},unloadLoops={},spareTimeLoops={},pendingHolderLoops={},maximumUnloadQueue={}",
                        elapsed, original.getPos(), returnWhilePending, waitForGeometry,
                        originalHolder == null ? -1 : originalHolder.getTicketLevel(),
                        originalHolder != null && originalHolder.getChunkToSave().isDone(), original.getClass().getSimpleName(),
                        unloaded == null ? "none" : unloaded.getClass().getSimpleName(), unloaded == original,
                        cached == original, cached == null, nativeState.requests(), nativeState.pending(),
                        unloadLoops, spareTimeLoops, pendingHolderLoops, maximumUnloadQueue);
            }
            helper.assertTrue(completed || elapsed < 600,
                    "Actual early unload/return must complete; returned=" + returned + "," + nativeState);
            if (!completed) return;
            var waits = MinecraftThermalInput.observedCheckpointStatistics(level);
            long waited = waits.saveWaitNanos() + waits.unloadWaitNanos() - initialWait;
            helper.assertTrue(waited <= 5_000_000L, "Actual early lifecycle must not block the main thread: " + waited);
            helper.assertTrue(nativeState.fallbacks() == 0, "No early lifecycle may fall back to blocking");
            if (returnWhilePending) helper.assertTrue(unloaded == null, "A returned chunk must not receive a stale unload");
            else helper.assertTrue(nativeState.prepared() > 0, "The deferred request must eventually prepare a real snapshot");
            if (!returnWhilePending) {
                var state = MinecraftThermalInput.dormantState((net.minecraft.world.level.chunk.LevelChunk) original);
                if (!waitForGeometry) helper.assertTrue(state == null || state.oldestAirSavedTick() < 0,
                        "An unpublished field must not invent Air samples; real unconsumed J may be saved in v6");
                net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                if (state != null) state.encode(tag);
                var sections = tag.getCompound("FrostedHeartThermal").getList("sections", net.minecraft.nbt.Tag.TAG_COMPOUND);
                int points = 0;
                double maximum = 0;
                for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
                    var section = sections.getCompound(sectionIndex);
                    if (!section.contains("air", net.minecraft.nbt.Tag.TAG_COMPOUND)) continue;
                    var air = com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.AirFieldCheckpoint.decode(section.getCompound("air"));
                    helper.assertTrue(air != null, "The actual early unload must produce a decodable saved Air field");
                    for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++)
                        for (int corner = 0; corner < 8; corner++) {
                            double value = air.sampleC(original.getPos().getMinBlockX() + x + ((corner & 1) == 0 ? .25 : .75),
                                    (section.getInt("y") << 4) + y + ((corner & 2) == 0 ? .25 : .75),
                                    original.getPos().getMinBlockZ() + z + ((corner & 4) == 0 ? .25 : .75));
                            if (Double.isFinite(value)) {
                                points++; maximum = Math.max(maximum, Math.abs(value - section.getDouble("natural")));
                            }
                        }
                }
                FHMain.LOGGER.info("THERMAL_NATIVE_INITIAL_AIR,points={},maximumDifferenceFromSavedNaturalC={},savedAirTick={}",
                        points, maximum, state == null ? -1 : state.oldestAirSavedTick());
            }
            FHMain.LOGGER.info("THERMAL_NATIVE_EARLY_RESULT,returnWhilePending={},elapsedTicks={},requests={},deferred={},prepared={},cancelled={},pending={},thermalWaitUs={}",
                    returnWhilePending, elapsed, nativeState.requests(), nativeState.deferred(), nativeState.prepared(),
                    nativeState.cancelled(), nativeState.pending(), waited / 1000);
            finished = true; observation = null;
            for (BlockPos fire : fires) {
                if (level.getChunkSource().getChunkNow(fire.getX() >> 4, fire.getZ() >> 4) != null)
                    ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
            }
            ContinuousAirTwoBaseProductionGameTests.forceBase(level, origin, false);
            MinecraftThermalInput.closeActiveLevel(level);
            helper.startSequence().thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.thermalBudgetUsedBytes() == 0,
                    "Early lifecycle asynchronous close releases its requests and thermal backing"))
                    .thenExecute(() -> FHMain.LOGGER.info("THERMAL_NATIVE_EARLY_RELEASE,rootAfterBytes={}",
                            MinecraftThermalInput.thermalBudgetUsedBytes())).thenSucceed();
        }

        void returnTickets(int elapsed, String trigger) {
            ContinuousAirTwoBaseProductionGameTests.forceBase(helper.getLevel(), origin, true);
            returned = true;
            var state = MinecraftThermalInput.observedNativeCheckpoints(helper.getLevel());
            FHMain.LOGGER.info("THERMAL_NATIVE_TICKET_RETURN,elapsedTicks={},pending={},deferred={},trigger={}",
                    elapsed, state.pending(), state.deferred(), trigger);
        }
    }
}

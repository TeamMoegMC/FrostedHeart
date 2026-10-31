/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ScatteredLoadObservation;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** One persistent real server, progressively scattered rooms and 100 gameplay temperature consumers. */
@GameTestHolder("frostedheart_scattered_bases")
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = FHMain.MODID)
public final class ContinuousAirScatteredBaseGameTests {
    private static final int[] COUNTS = {2, 8, 16, 32};
    private static final int CONSUMERS = 100, WINDOW_TICKS = 2400, WARMUP_TICKS = 200, TIMEOUT_TICKS = 26000;
    private static final int BUILD_PER_TICK = 128, ROOM_BLOCKS = 17 * 17 * 9;
    private static Scene active;

    @jdk.jfr.Name("frostedheart.ScatteredBaseWindow")
    @jdk.jfr.StackTrace(false)
    private static final class WindowEvent extends jdk.jfr.Event {
        public String phase;
        public int bases, consumers;
        public long fromTick, throughTick;
    }

    @SubscribeEvent public static void unloaded(ChunkEvent.Unload event) {
        var scene = active;
        if (scene != null && event.getLevel() == scene.level && scene.paused != null
                && event.getChunk().getPos().equals(new net.minecraft.world.level.ChunkPos(scene.paused.probe)))
            scene.unloaded = event.getChunk();
    }

    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { active = null; }

    @GameTest(template = "phase0a_empty", timeoutTicks = TIMEOUT_TICKS)
    public static void hundredConsumersAcrossGrowingIndependentBases(GameTestHelper helper) throws java.io.IOException {
        helper.assertTrue(true, "Use the configured continuous runtime");
        ContinuousAirPrecisionProductionGameTests.restoreReferenceClimate(helper, true);
        var scene = new Scene(helper);
        active = scene;
        var sequence = helper.startSequence();
        int previous = 0;
        for (int count : COUNTS) {
            sequence.thenExecute(() -> scene.phase("build-" + count));
            for (int index = previous; index < count; index++) {
                var base = scene.bases.get(index);
                sequence.thenExecute(() -> base.force(scene.level, true))
                        .thenWaitUntil(() -> helper.assertTrue(base.loaded(scene.level), "Load the actual room and surface-capture chunks"))
                        .thenExecuteFor((ROOM_BLOCKS + BUILD_PER_TICK - 1) / BUILD_PER_TICK, () -> base.build(scene))
                        .thenExecute(() -> base.activate(scene));
            }
            previous = count;
            sequence.thenExecute(() -> scene.phase("warmup-" + count))
                    .thenIdle(WARMUP_TICKS)
                    .thenExecute(() -> scene.beginWindow("bases-" + count, true))
                    .thenIdle(WINDOW_TICKS)
                    .thenExecute(() -> { scene.finishWindow(); scene.closeOpenings(); scene.restoreSources(false); scene.save(); });
        }
        sequence.thenExecute(() -> scene.phase("settle-before-withdrawal")).thenIdle(WARMUP_TICKS).thenExecute(() -> {
            scene.phase("withdraw-sources-keep-structure");
            scene.restoreSources(true);
            scene.beginWindow("withdraw-sources-keep-structure", false);
        }).thenIdle(WINDOW_TICKS).thenExecute(() -> { scene.finishWindow(); scene.save(); })
                .thenExecute(scene::beginUnload)
                .thenWaitUntil(scene::assertUnloaded)
                .thenExecute(scene::beginReload)
                .thenWaitUntil(scene::assertReloaded)
                .thenIdle(WARMUP_TICKS)
                .thenExecute(() -> scene.beginWindow("after-reload", false))
                .thenIdle(WINDOW_TICKS)
                .thenExecute(() -> {
                    scene.finishWindow(); scene.save();
                    var observed = scene.observation.snapshot();
                    FHMain.LOGGER.info("THERMAL_SCATTERED_RESULT,bases={},consumers={},windows={},saves={},lifecycleReads={},lifecycleFallbacks={},rootPeakBytes={},observation={},failures={}",
                            scene.built, CONSUMERS, scene.windows, scene.saves, scene.lifecycleReads, scene.lifecycleFallbacks,
                            MinecraftThermalInput.thermalBudgetPeakBytes(), observed, scene.failures);
                    scene.close();
                    helper.assertTrue(observed.energyObservations() > 0 && observed.energyFailures() == 0
                                    && observed.orderingFailures() == 0 && observed.runtimeFailures() == 0 && scene.failures.isEmpty(),
                            "Complete every measured window, ownership balance and lifecycle: " + scene.failures + "," + observed);
                    helper.assertTrue(scene.windows == COUNTS.length + 2 && scene.saves == COUNTS.length + 3,
                            "Every stage and the real unload complete their native saves");
                }).thenSucceed();
        ContinuousAirGameTestPacing.beginMeasurement();
        ContinuousAirProductionTiming.eachTick(helper, TIMEOUT_TICKS - 1, ignored -> scene.tick());
    }

    private static final class Base {
        final int id;
        final BlockPos origin, probe, pillar, opening;
        final BlockPos[] fires = new BlockPos[8];
        int builtBlocks;
        long activatedAt, firstOnlineAt = -1, lastSampleTick = -1;
        boolean paused;
        Base(int id) {
            this.id = id;
            origin = new BlockPos(4096 + id % 8 * 128, 80, 4096 + id / 8 * 128);
            for (int i = 0; i < fires.length; i++) fires[i] = origin.offset(3 + i % 4 * 3, 1, 4 + i / 4 * 8);
            probe = fires[0].above(); pillar = origin.offset(8, 1, 8); opening = origin.offset(0, 2, 8);
        }
        void force(ServerLevel level, boolean forced) {
            for (int x = (origin.getX() - 6) >> 4; x <= (origin.getX() + 22) >> 4; x++)
                for (int z = (origin.getZ() - 6) >> 4; z <= (origin.getZ() + 22) >> 4; z++)
                    level.setChunkForced(x, z, forced);
        }
        boolean keepsLoaded(net.minecraft.world.level.ChunkPos target) {
            int forcedLevel = net.minecraft.server.level.ChunkLevel.byStatus(
                    net.minecraft.server.level.FullChunkStatus.ENTITY_TICKING);
            for (int x = (origin.getX() - 6) >> 4; x <= (origin.getX() + 22) >> 4; x++)
                for (int z = (origin.getZ() - 6) >> 4; z <= (origin.getZ() + 22) >> 4; z++)
                    if (net.minecraft.server.level.ChunkLevel.isLoaded(forcedLevel
                            + Math.max(Math.abs(x - target.x), Math.abs(z - target.z)))) return true;
            return false;
        }
        boolean loaded(ServerLevel level) {
            for (int x = (origin.getX() - 6) >> 4; x <= (origin.getX() + 22) >> 4; x++)
                for (int z = (origin.getZ() - 6) >> 4; z <= (origin.getZ() + 22) >> 4; z++)
                    if (level.getChunkSource().getChunkNow(x, z) == null) return false;
            return true;
        }
        void build(Scene scene) {
            int end = Math.min(ROOM_BLOCKS, builtBlocks + BUILD_PER_TICK);
            while (builtBlocks < end) {
                int index = builtBlocks++, x = index % 17, z = index / 17 % 17, y = index / (17 * 17);
                boolean wall = x == 0 || x == 16 || z == 0 || z == 16 || y == 0 || y == 8;
                scene.level.setBlockAndUpdate(origin.offset(x, y, z), (wall ? Blocks.STONE : Blocks.AIR).defaultBlockState());
            }
        }
        void activate(Scene scene) {
            scene.helper.assertTrue(builtBlocks == ROOM_BLOCKS, "Batched construction completes the actual room");
            scene.level.setBlockAndUpdate(pillar, Blocks.OAK_LOG.defaultBlockState());
            for (var fire : fires) {
                scene.level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
                fuel(scene, fire); lit(scene, fire, true);
            }
            activatedAt = scene.level.getGameTime(); scene.built++;
        }
        void fuel(Scene scene, BlockPos fire) {
            var entity = scene.level.getBlockEntity(fire);
            scene.helper.assertTrue(entity != null, "Actual campfire entity exists");
            var inventory = entity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(
                    () -> new IllegalStateException("Campfire fuel capability is absent"));
            if (inventory.getStackInSlot(0).getCount() < 16) inventory.insertItem(0, new ItemStack(Items.COAL, 64), false);
        }
        void lit(Scene scene, BlockPos fire, boolean enabled) {
            if (scene.level.getBlockState(fire).getValue(CampfireBlock.LIT) != enabled)
                ContinuousAirProductionGameTests.useItem(scene.helper, fire, enabled ? Items.FLINT_AND_STEEL : Items.IRON_SHOVEL);
        }
        void change(Scene scene, int ordinal) {
            BlockPos fire = fires[ordinal % fires.length];
            lit(scene, fire, !scene.level.getBlockState(fire).getValue(CampfireBlock.LIT));
            var state = scene.level.getBlockState(pillar);
            scene.level.setBlockAndUpdate(pillar, state.setValue(RotatedPillarBlock.AXIS,
                    state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y ? Direction.Axis.X : Direction.Axis.Y));
            scene.level.setBlockAndUpdate(opening, (scene.level.getBlockState(opening).isAir() ? Blocks.STONE : Blocks.AIR).defaultBlockState());
        }
    }

    private static final class Scene {
        final GameTestHelper helper;
        final ServerLevel level;
        final List<Base> bases = new ArrayList<>();
        final List<String> failures = new ArrayList<>();
        final FakePlayer[] players = new FakePlayer[CONSUMERS];
        final long[] playerTicks = new long[CONSUMERS];
        final int[] playerBases = new int[CONSUMERS];
        final ThermalEnvironmentSample sample = new ThermalEnvironmentSample();
        final QueryPublication.MutableSample raw = new QueryPublication.MutableSample();
        final ScatteredLoadObservation observation;
        String phase = "build";
        long phaseTick, phaseMaximumMainTick, phaseMaximumServerTick, lifecycleReads, lifecycleFallbacks;
        int built, windows, saves;
        boolean closed;
        Base paused;
        ChunkAccess beforeUnload, unloaded;
        net.minecraft.server.level.ChunkHolder beforeUnloadHolder;
        long unloadStartedTick;
        Window window;
        Scene(GameTestHelper helper) {
            this.helper = helper; level = helper.getLevel();
            for (int i = 0; i < COUNTS[COUNTS.length - 1]; i++) bases.add(new Base(i));
            observation = new ScatteredLoadObservation(bases.get(0).fires[0].asLong());
            Arrays.fill(playerTicks, -1); Arrays.fill(playerBases, -1);
            for (int i = 0; i < players.length; i++) {
                String name = "scattered" + i;
                players[i] = FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name));
            }
            FHMain.LOGGER.info("THERMAL_SCATTERED_START,consumers={},stages={},sourcesPerBase=8,measurementTicks={},warmupTicks={},buildBlocksPerTick={},workBudgetMsPerSecond={},memoryTargetBytes={},processors={},workers={},maxHeapBytes={},clock={}",
                    CONSUMERS, Arrays.toString(COUNTS), WINDOW_TICKS, WARMUP_TICKS, BUILD_PER_TICK,
                    FHConfig.COMMON.THERMAL_RUNTIME.continuousAirWorkBudgetMsPerSecond.get(), MinecraftThermalInput.thermalBudgetLimitBytes(),
                    Runtime.getRuntime().availableProcessors(),
                    com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalWorkerPool.shared().parallelism(),
                    Runtime.getRuntime().maxMemory(), ContinuousAirGameTestPacing.clockMode());
            phaseTick = level.getGameTime();
        }
        void phase(String next) {
            FHMain.LOGGER.info("THERMAL_SCATTERED_PHASE,phase={},fromTick={},throughTick={},bases={},maxMainThermalUs={},maxServerProcessingUs={}",
                    phase, phaseTick, level.getGameTime(), built, phaseMaximumMainTick / 1000, phaseMaximumServerTick / 1000);
            phase = next; phaseTick = level.getGameTime(); phaseMaximumMainTick = phaseMaximumServerTick = 0;
        }
        void beginWindow(String name, boolean changes) {
            phase(name);
            window = new Window(name, built, level.getGameTime(), changes);
            FHMain.LOGGER.info("THERMAL_SCATTERED_WINDOW_START,phase={},bases={},sources={},loadedChunks={},tick={},rootBytes={},observation={}",
                    name, built, MinecraftThermalInput.enabledPhysicalSourceCount(level), level.getChunkSource().getLoadedChunksCount(),
                    level.getGameTime(), MinecraftThermalInput.thermalBudgetUsedBytes(), observation.snapshot());
        }
        void tick() {
            if (closed || built == 0) return;
            long tick = level.getGameTime();
            long main = MinecraftThermalInput.observedMainTickNanos(level), server = ContinuousAirGameTestPacing.lastProcessingNanos();
            phaseMaximumMainTick = Math.max(phaseMaximumMainTick, main); phaseMaximumServerTick = Math.max(phaseMaximumServerTick, server);
            int elapsed = window == null ? -1 : (int) (tick - window.startedTick);
            boolean measuring = window != null && elapsed >= 0 && elapsed < WINDOW_TICKS;
            if (measuring) {
                window.times(main, server);
                if (window.changes && elapsed >= WINDOW_TICKS / 2 && elapsed % 5 == 0) {
                    int ordinal = (elapsed - WINDOW_TICKS / 2) / 5;
                    bases.get(ordinal % built).change(this, ordinal);
                    window.actions++;
                }
            }
            if (tick % 200 == 0) {
                var observed = observation.snapshot();
                if (observed.runtimeFailures() != 0) {
                    close();
                    helper.fail("The actual thermal runtime failed during the survey: " + observed);
                    return;
                }
                for (int i = 0; i < built; i++) if (!bases.get(i).paused) for (var fire : bases.get(i).fires) bases.get(i).fuel(this, fire);
                FHMain.LOGGER.info("THERMAL_SCATTERED_PROGRESS,phase={},bases={},sources={},tick={},loadedChunks={},rootBytes={},observation={},worker={}",
                        phase, built, MinecraftThermalInput.enabledPhysicalSourceCount(level), tick,
                        level.getChunkSource().getLoadedChunksCount(), MinecraftThermalInput.thermalBudgetUsedBytes(), observation.snapshot(),
                        MinecraftThermalInput.describeWorkerProgress(level));
                if (measuring) window.heapPeak = Math.max(window.heapPeak, ContinuousAirProductionTiming.resources()[6]);
            }
            for (int i = (int) (tick % 20); i < CONSUMERS; i += 20) {
                var base = bases.get(i % built);
                if (base.paused) base = bases.get(0);
                var position = base.probe;
                players[i].setPos(position.getX() + .5 + .15 * Math.sin(tick * .02 + i), position.getY() - 1, position.getZ() + .5);
                MinecraftThermalInput.gameplayPlayerEnvironment(players[i], WorldTemperature.naturalAir(level, position), sample);
                if (sample.airAvailable() && sample.airSampleTick() >= 0) {
                    if (playerBases[i] == base.id && sample.airSampleTick() < playerTicks[i]) failure("consumer time reversed at base " + base.id);
                    playerTicks[i] = sample.airSampleTick(); playerBases[i] = base.id;
                }
                if (measuring) window.consumer(sample, tick);
                else if (paused != null) { lifecycleReads++; if (!sample.airAvailable() || sample.airSampleTick() < 0) lifecycleFallbacks++; }
            }
            var publication = MinecraftThermalInput.queryPublicationForObservation(level);
            for (int i = (int) (tick % 20); i < built; i += 20) {
                var base = bases.get(i);
                if (base.paused) continue;
                var point = base.probe;
                ContinuousAirProductionGameTests.sampleAt(level, point.getX() + .5, point.getY() + .5, point.getZ() + .5, sample);
                if (sample.airAvailable() && sample.airSampleTick() >= 0) {
                    long sampled = sample.airSampleTick();
                    if (sampled < base.lastSampleTick) failure("probe time reversed at base " + i);
                    if (base.firstOnlineAt < 0) {
                        base.firstOnlineAt = tick;
                        FHMain.LOGGER.info("THERMAL_SCATTERED_FIRST_READ,base={},activatedTick={},firstOnlineTick={},latencyTicks={},minimum={}",
                                i, base.activatedAt, tick, tick - base.activatedAt, sample.airMinimumQuality());
                    }
                    if (measuring && sampled > base.lastSampleTick) window.commits[i][Math.min(5, elapsed / 400)]++;
                    base.lastSampleTick = sampled;
                }
                if (measuring) {
                    boolean strict = publication != null && publication.tryReadAir(point.getX() + .5, point.getY() + .5, point.getZ() + .5, raw);
                    if (!strict) window.strictMissing++;
                    else if (!raw.minimumQuality() && tick - raw.sampleTick() > 40) window.fineExpiredProbes++;
                    if (!MinecraftThermalInput.describeAirRead(level, point).startsWith("geometry_current")) window.geometryNotCurrent++;
                }
            }
        }
        void finishWindow() {
            var completed = window; window = null; windows++;
            completed.finish(this);
        }
        void failure(String reason) { if (!failures.contains(reason)) failures.add(reason); }
        void restoreSources(boolean onePerBase) {
            for (int i = 0; i < built; i++) {
                var base = bases.get(i);
                for (int fire = 0; fire < base.fires.length; fire++) base.lit(this, base.fires[fire], !onePerBase || fire == 0);
            }
        }
        void closeOpenings() {
            for (int i = 0; i < built; i++) level.setBlockAndUpdate(bases.get(i).opening, Blocks.STONE.defaultBlockState());
        }
        void save() {
            long started = System.nanoTime();
            FHMain.LOGGER.info("THERMAL_SCATTERED_SAVE_BEGIN,phase={},tick={},bases={}", phase, level.getGameTime(), built);
            level.getServer().saveEverything(true, true, true); saves++;
            FHMain.LOGGER.info("THERMAL_SCATTERED_SAVE_END,phase={},tick={},durationUs={},{}", phase, level.getGameTime(),
                    (System.nanoTime() - started) / 1000, MinecraftThermalInput.observedMainWaits(level));
        }
        void beginUnload() {
            phase("unload"); paused = bases.get(built - 1); paused.paused = true;
            beforeUnload = level.getChunkAt(paused.probe); unloaded = null;
            unloadStartedTick = level.getGameTime(); beforeUnloadHolder = null;
            for (var holder : level.getChunkSource().chunkMap.getChunks())
                if (holder.getPos().equals(beforeUnload.getPos())) { beforeUnloadHolder = holder; break; }
            save();
            // Nearby forced chunks retain generation dependencies beyond the full
            // chunk cache. Remove this fixture's contributing tickets as well.
            int releasedBases = 0;
            for (var base : bases) if (base.keepsLoaded(beforeUnload.getPos())) {
                base.paused = true; base.force(level, false); releasedBases++;
            }
            FHMain.LOGGER.info("THERMAL_SCATTERED_UNLOAD_TICKETS,chunk={},releasedBases={},remainingBases={}",
                    beforeUnload.getPos(), releasedBases, built - releasedBases);
        }
        void assertUnloaded() {
            if (level.getGameTime() % 200 == 0) FHMain.LOGGER.info(
                    "THERMAL_SCATTERED_UNLOAD_WAIT,elapsedTicks={},chunk={},ticketLevel={},saveFutureDone={},unloadedOriginal={},cachedAbsent={},native={}",
                    level.getGameTime() - unloadStartedTick, beforeUnload.getPos(),
                    beforeUnloadHolder == null ? -1 : beforeUnloadHolder.getTicketLevel(),
                    beforeUnloadHolder != null && beforeUnloadHolder.getChunkToSave().isDone(), unloaded == beforeUnload,
                    level.getChunkSource().getChunkNow(paused.probe.getX() >> 4, paused.probe.getZ() >> 4) == null,
                    MinecraftThermalInput.observedNativeCheckpoints(level));
            helper.assertTrue(unloaded == beforeUnload && level.getChunkSource().getChunkNow(paused.probe.getX() >> 4, paused.probe.getZ() >> 4) == null,
                    "Wait for actual ChunkEvent.Unload and native cache removal while other bases keep being queried");
        }
        void beginReload() {
            phase("reload");
            for (var base : bases) if (base.paused) base.force(level, true);
        }
        void assertReloaded() {
            for (var base : bases) if (base.paused)
                helper.assertTrue(base.loaded(level), "Reload every room whose own tickets were removed");
            var current = level.getChunkSource().getChunkNow(paused.probe.getX() >> 4, paused.probe.getZ() >> 4);
            helper.assertTrue(current != null && current != beforeUnload, "Reload must create a new native chunk instance");
            for (var fire : paused.fires) helper.assertTrue(level.getBlockState(fire).is(Blocks.CAMPFIRE), "Saved room keeps all original campfire blocks");
            for (var base : bases) base.paused = false;
            paused = null;
            beforeUnload = unloaded = null;
            beforeUnloadHolder = null;
        }
        void close() {
            closed = true; observation.close();
            for (var base : bases) base.force(level, false);
            beforeUnload = unloaded = null; beforeUnloadHolder = null;
            if (active == this) active = null;
        }
    }

    private static final class Window {
        final String name;
        final long startedTick;
        final boolean changes;
        final long[] startedResources = ContinuousAirProductionTiming.resources();
        final long[] mainTimes = new long[WINDOW_TICKS + 1], serverTimes = new long[WINDOW_TICKS + 1];
        final int[] fineAges = new int[(WINDOW_TICKS + 1) * 5], coarseAges = new int[(WINDOW_TICKS + 1) * 5];
        final int[][] commits;
        final WindowEvent event = new WindowEvent();
        int ticks, fine, coarse, stored, natural, invalid, fineExpiredProbes, strictMissing, geometryNotCurrent, actions;
        long heapPeak = startedResources[6];
        Window(String name, int bases, long tick, boolean changes) {
            this.name = name; startedTick = tick; this.changes = changes; commits = new int[bases][6];
            event.phase = name; event.bases = bases; event.consumers = CONSUMERS; event.fromTick = tick; event.begin();
        }
        void times(long main, long server) { mainTimes[ticks] = main; serverTimes[ticks++] = server; }
        void consumer(ThermalEnvironmentSample sample, long now) {
            if (!sample.airAvailable()) { natural++; return; }
            if (!Double.isFinite(sample.airTemperatureC())) { invalid++; return; }
            if (sample.airSampleTick() < 0) { stored++; return; }
            long age = now - sample.airSampleTick();
            if (age < 0) { invalid++; return; }
            if (sample.airMinimumQuality()) coarseAges[coarse++] = Math.toIntExact(age);
            else fineAges[fine++] = Math.toIntExact(age);
        }
        void finish(Scene scene) {
            event.throughTick = scene.level.getGameTime(); event.end(); event.commit();
            long[] end = ContinuousAirProductionTiming.resources();
            int minimumCommits = Integer.MAX_VALUE;
            for (int base = 0; base < commits.length; base++) for (int window = 0; window < 6; window++) {
                minimumCommits = Math.min(minimumCommits, commits[base][window]);
                if (commits[base][window] < 2) scene.failure(name + " base " + base + " made " + commits[base][window] + " advances in window " + window);
            }
            if (invalid != 0) scene.failure(name + " returned invalid numerical readings: " + invalid);
            var observation = scene.observation.snapshot();
            Arrays.sort(mainTimes, 0, ticks); Arrays.sort(serverTimes, 0, ticks);
            Arrays.sort(fineAges, 0, fine); Arrays.sort(coarseAges, 0, coarse);
            String result = "phase=" + name + ",bases=" + commits.length + ",ticks=" + ticks + ",sources=" + MinecraftThermalInput.enabledPhysicalSourceCount(scene.level)
                    + ",fineReads=" + fine + ",coarseReads=" + coarse + ",storedFallbacks=" + stored + ",naturalFallbacks=" + natural
                    + ",invalidReads=" + invalid + ",fineAgeP99=" + percentile(fineAges, fine, .99) + ",coarseAgeP99=" + percentile(coarseAges, coarse, .99)
                    + ",fineAgeMax=" + percentile(fineAges, fine, 1) + ",coarseAgeMax=" + percentile(coarseAges, coarse, 1)
                    + ",fineExpiredProbes=" + fineExpiredProbes + ",strictMissingProbes=" + strictMissing + ",geometryNotCurrentProbes=" + geometryNotCurrent
                    + ",minimumWindowCommits=" + minimumCommits + ",actions=" + actions + ",loadedChunks=" + scene.level.getChunkSource().getLoadedChunksCount()
                    + ",workerCpuMs=" + (end[0] - startedResources[0]) / 1000000 + ",workerAllocatedBytes=" + (end[1] - startedResources[1])
                    + ",serverCpuMs=" + (end[2] - startedResources[2]) / 1000000 + ",serverAllocatedBytes=" + (end[3] - startedResources[3])
                    + ",gcCollections=" + (end[4] - startedResources[4]) + ",gcMillis=" + (end[5] - startedResources[5])
                    + ",mainThermalP95Us=" + percentile(mainTimes, ticks, .95) / 1000 + ",mainThermalP99Us=" + percentile(mainTimes, ticks, .99) / 1000
                    + ",serverP95Us=" + percentile(serverTimes, ticks, .95) / 1000 + ",serverP99Us=" + percentile(serverTimes, ticks, .99) / 1000
                    + ",serverMaxUs=" + percentile(serverTimes, ticks, 1) / 1000 + ",heapPeakBytes=" + Math.max(heapPeak, end[6])
                    + ",rootBytes=" + MinecraftThermalInput.thermalBudgetUsedBytes() + ",rootPeakBytes=" + MinecraftThermalInput.thermalBudgetPeakBytes()
                    + ",observation=" + observation;
            FHMain.LOGGER.info("THERMAL_SCATTERED_WINDOW,{}", result);
        }
        private static long percentile(long[] values, int count, double fraction) {
            if (count == 0) return -1;
            return values[Math.max(0, (int) Math.ceil(count * fraction) - 1)];
        }
        private static int percentile(int[] values, int count, double fraction) {
            if (count == 0) return -1;
            return values[Math.max(0, (int) Math.ceil(count * fraction) - 1)];
        }
    }
}

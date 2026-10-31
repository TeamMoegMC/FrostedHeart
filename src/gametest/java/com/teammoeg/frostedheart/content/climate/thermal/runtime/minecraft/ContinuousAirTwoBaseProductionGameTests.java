/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Same-dimension two actual dense 128-fire bases; never creates thermal state. */
@GameTestHolder("frostedheart_two_dense_bases")
@PrefixGameTestTemplate(false)
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = FHMain.MODID)
public final class ContinuousAirTwoBaseProductionGameTests {
    private ContinuousAirTwoBaseProductionGameTests() {}
    private static BaseLifecycle observedLifecycle;

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onActualChunkUnload(net.minecraftforge.event.level.ChunkEvent.Unload event) {
        BaseLifecycle observation = observedLifecycle;
        if (observation != null && event.getLevel() == observation.helper.getLevel()
                && event.getChunk().getPos().x == observation.target.getX() >> 4
                && event.getChunk().getPos().z == observation.target.getZ() >> 4)
            observation.unloadedChunk = event.getChunk();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 2500)
    public static void twoDenseBasesKeepCurrentAirAndFloors(GameTestHelper helper) throws java.io.IOException {
        run(helper, false);
    }

    static void run(GameTestHelper helper, boolean operatingMachines) throws java.io.IOException {
        int endTick = Math.max(600, Math.min(2400, Integer.parseInt(
                System.getenv().getOrDefault("FH_CONTINUOUS_AIR_TWO_BASE_TICKS", "2400"))));
        run(helper, operatingMachines, endTick, false);
    }

    static void run(GameTestHelper helper, boolean operatingMachines, int endTick, boolean endurance) throws java.io.IOException {
        run(helper, operatingMachines, endTick, endurance, false);
    }

    static void runBusyLifecycle(GameTestHelper helper) throws java.io.IOException {
        int endTick = Math.max(4000, Math.min(12000, Integer.parseInt(
                System.getenv().getOrDefault("FH_CONTINUOUS_AIR_TWO_BASE_TICKS", "4000"))));
        run(helper, true, endTick, true, true);
    }

    private static void run(GameTestHelper helper, boolean operatingMachines, int endTick,
                            boolean endurance, boolean busyLifecycle) throws java.io.IOException {
        ContinuousAirPrecisionProductionGameTests.restoreReferenceClimate(helper, true);
        var level = helper.getLevel();
        int cycleTicks = endurance ? 4000 : 2400;
        // Loaded terrain extends beyond ticking chunks. Keep the lifecycle target outside
        // both spawn retention and the other forced base's terrain dependencies.
        int separationBlocks = endurance ? 512 : 160;
        boolean aOnly = "A_only".equalsIgnoreCase(System.getenv("FH_CONTINUOUS_AIR_TWO_BASE_ACTIONS"));
        BlockPos[] origins = {helper.absolutePos(new BlockPos(5, 0, 7)), helper.absolutePos(new BlockPos(5 + separationBlocks, 0, 7))};
        BlockPos[] fires = new BlockPos[256], probes = new BlockPos[16];
        for (int base = 0; base < origins.length; base++) {
            BlockPos origin = origins[base];
            // Keep the actual base and R6 surface capture loaded, including the remote base.
            forceBase(level, origin, true);
            var built = ContinuousAirLargeBaseProductionGameTests.buildBase(helper, origin);
            System.arraycopy(built.fires(), 0, fires, base * 128, 128);
            System.arraycopy(built.probes(), 0, probes, base * 8, 8);
        }
        if (operatingMachines) installOperatingMachines(helper, fires, endTick, cycleTicks);
        BaseLifecycle lifecycle = endurance ? new BaseLifecycle(helper, origins[1], fires, probes, endTick / cycleTicks, busyLifecycle) : null;
        observedLifecycle = lifecycle;
        FHMain.LOGGER.info("THERMAL_TWO_BASE_START,sources=256,bases=2,separationBlocks={},endTick={},actions={},rootLimitBytes={},capacityTrial={}",
                separationBlocks, endTick, aOnly ? "A_only" : "both_bases", MinecraftThermalInput.thermalBudgetLimitBytes(), System.getenv("FH_CONTINUOUS_AIR_CAPACITY_TRIAL"));
        ContinuousAirProductionTiming.observe(helper, operatingMachines ? "two_dense_machine_bases_256"
                : aOnly ? "two_dense_bases_256_A_only" : "two_dense_bases_256", probes, endTick);
        ContinuousAirProductionTiming.observeTemperatureEnvelope(helper, fires, probes, endTick);
        FakePlayer[] receivers = new FakePlayer[100];
        for (int index = 0; index < receivers.length; index++) {
            String name = "twoBase" + index;
            receivers[index] = FakePlayerFactory.get(level, new GameProfile(
                    UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name));
        }
        var air = new ThermalEnvironmentSample();
        var material = new MaterialSample();
        int[] monitoringSince = {-1}, ageViolations = {0}, geometryViolations = {0}, receiverViolations = {0};
        int[] receiverWarmupGaps = {0};
        long[] receiverReadyAt = new long[receivers.length];
        java.util.Arrays.fill(receiverReadyAt, -1);
        int[] firstMismatch = new int[probes.length], lastCurrent = new int[probes.length];
        java.util.Arrays.fill(firstMismatch, -1);
        long[] workLimitedAtMonitoring = {-1}, queries = {0};
        ContinuousAirProductionTiming.eachTick(helper, endTick, elapsed -> {
                if (lifecycle != null) lifecycle.tick(elapsed);
                boolean allCurrent = true;
                boolean allFresh = true, allRecoveredWithinDeadline = true;
                int liveFloors = 0;
                for (int probeIndex = 0; probeIndex < probes.length; probeIndex++) {
                    if (lifecycle != null && lifecycle.remotePaused() && probeIndex >= 8) {
                        // Only the explicitly absent/recovering base is excluded. Base A stays monitored.
                        firstMismatch[probeIndex] = -1;
                        lastCurrent[probeIndex] = elapsed;
                        continue;
                    }
                    BlockPos probe = probes[probeIndex];
                    MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, probe, WorldTemperature.naturalAir(level, probe), air);
                    boolean fresh = air.airAvailable() && air.airSampleTick() >= 0 && level.getGameTime() - air.airSampleTick() <= 40;
                    boolean current = MinecraftThermalInput.describeAirRead(level, probe).startsWith("geometry_current");
                    allFresh &= fresh;
                    if (current) { firstMismatch[probeIndex] = -1; lastCurrent[probeIndex] = elapsed; }
                    else if (firstMismatch[probeIndex] < 0) firstMismatch[probeIndex] = elapsed;
                    boolean withinDeadline = current || elapsed - firstMismatch[probeIndex] <= 40;
                    allRecoveredWithinDeadline &= withinDeadline && elapsed - lastCurrent[probeIndex] <= 40;
                    if (monitoringSince[0] >= 0) {
                        if (!fresh && ageViolations[0]++ == 0)
                            FHMain.LOGGER.info("THERMAL_TWO_BASE_FIRST_STALE,elapsedTicks={},gameTick={},probe={},position={},sampleTick={},available={},read={},worker={}",
                                    elapsed, level.getGameTime(), probeIndex, probe, air.airSampleTick(), air.airAvailable(),
                                    MinecraftThermalInput.describeAirRead(level, probe), MinecraftThermalInput.describeWorkerProgress(level));
                        if (!withinDeadline) geometryViolations[0]++;
                        if (!current && elapsed - firstMismatch[probeIndex] == 41)
                            FHMain.LOGGER.info("THERMAL_TWO_BASE_GEOMETRY_DEADLINE,elapsedTicks={},probe={},firstMismatch={},sampleTick={},read={},invalidations={}",
                                    elapsed, probeIndex, firstMismatch[probeIndex], air.airSampleTick(),
                                    MinecraftThermalInput.describeAirRead(level, probe), MinecraftThermalInput.describeGeometryInvalidations(level));
                    }
                    allCurrent &= fresh && current && (monitoringSince[0] >= 0 || ContinuousAirProductionTiming.sourceField(air));
                    if (elapsed % 200 == 0 && MinecraftThermalInput.sampleMaterial(level, probe.below(2), material)
                            && material.source() == MaterialSample.Source.LIVE) liveFloors++;
                }
                boolean warming = monitoringSince[0] < 0;
                for (int index = elapsed % 20; index < receivers.length; index += 20) {
                    if (lifecycle != null && lifecycle.remotePaused() && index % 2 == 1) continue;
                    // Alternate bases so all 100 environment consumers share both fields.
                    BlockPos fire = fires[(index % 2) * 128 + index / 2];
                    receivers[index].setPos(fire.getX() + .5, fire.getY(), fire.getZ() + .5);
                    MinecraftThermalInput.gameplayPlayerEnvironment(receivers[index], WorldTemperature.naturalAir(level, fire), air);
                    queries[0]++;
                    boolean fresh = air.airAvailable() && air.airSampleTick() >= 0
                            && level.getGameTime() - air.airSampleTick() <= 40;
                    receiverReadyAt[index] = fresh ? level.getGameTime() : -1;
                    if (warming && !fresh) receiverWarmupGaps[0]++;
                    if (!warming && !fresh) {
                        receiverViolations[0]++;
                        if (receiverViolations[0] <= 8) {
                            var receiver = receivers[index];
                            var point = BlockPos.containing(receiver.getX(), receiver.getEyeY(), receiver.getZ());
                            var publication = MinecraftThermalInput.queryPublicationForObservation(level);
                            FHMain.LOGGER.info("THERMAL_TWO_BASE_RECEIVER_GAP,elapsedTicks={},gameTick={},receiver={},x={},eyeY={},z={},available={},sampleTick={},pointRead={},fieldRead={},block={},{}",
                                    elapsed, level.getGameTime(), index, receiver.getX(), receiver.getEyeY(), receiver.getZ(),
                                    air.airAvailable(), air.airSampleTick(), MinecraftThermalInput.describeAirRead(level, point),
                                    publication == null ? "missing" : publication.describeAirRead(receiver.getX(), receiver.getEyeY(), receiver.getZ()),
                                    level.getBlockState(point), MinecraftThermalInput.describeWorkerProgress(level));
                        }
                    }
                }
                boolean receiversReady = true;
                if (warming) for (long readyAt : receiverReadyAt)
                    receiversReady &= readyAt >= 0 && level.getGameTime() - readyAt <= 20;
                if (warming && allCurrent && receiversReady) {
                    monitoringSince[0] = elapsed;
                    workLimitedAtMonitoring[0] = MinecraftThermalInput.workLimitedCompletions(level);
                    FHMain.LOGGER.info("THERMAL_TWO_BASE_ALL_CONSUMERS_READY,elapsedTicks={},probes={},receivers={},warmupMissingReads={}",
                            elapsed, probes.length, receivers.length, receiverWarmupGaps[0]);
                }
                if (elapsed % 200 == 0) FHMain.LOGGER.info(
                        "THERMAL_TWO_BASE_STATE,elapsedTicks={},enabledSources={},monitoringSince={},liveFloors={},ageViolations={},geometryViolations={},receiverViolations={},queries={},workLimited={},rootChargedBytes={},worker={}",
                        elapsed, MinecraftThermalInput.enabledPhysicalSourceCount(level), monitoringSince[0], liveFloors, ageViolations[0], geometryViolations[0], receiverViolations[0], queries[0],
                        MinecraftThermalInput.workLimitedCompletions(level), MinecraftThermalInput.thermalBudgetUsedBytes(), MinecraftThermalInput.describeWorkerProgress(level));
                if (elapsed != endTick) return;
                helper.assertTrue(monitoringSince[0] >= 0 && monitoringSince[0] <= endTick - Math.min(800, endTick / 2)
                                && allFresh && allRecoveredWithinDeadline,
                        "Both real 128-source bases must enter and retain current Air; worker=" + MinecraftThermalInput.describeWorkerProgress(level));
                helper.assertTrue(liveFloors == 16, "Both bases' actual stone floors remain LIVE");
                helper.assertTrue(MinecraftThermalInput.enabledPhysicalSourceCount(level) == 256,
                        "All 256 actual campfires must be enabled in the physical source input");
                helper.assertTrue(ageViolations[0] == 0 && geometryViolations[0] == 0 && receiverViolations[0] == 0,
                        "Two-base freshness must remain valid; age=" + ageViolations[0] + ",geometry=" + geometryViolations[0] + ",receivers=" + receiverViolations[0]);
                helper.assertTrue(MinecraftThermalInput.workLimitedCompletions(level) == workLimitedAtMonitoring[0],
                        "Two-base candidate must not accumulate capacity refusals");
                FHMain.LOGGER.info("THERMAL_TWO_BASE_RESULT,sources=256,receivers=100,queries={},monitoringSince={},rootPeakChargedBytes={}",
                        queries[0], monitoringSince[0], MinecraftThermalInput.thermalBudgetPeakBytes());
                if (lifecycle == null) helper.succeed();
                else {
                    helper.assertTrue(lifecycle.completedCycles == endTick / cycleTicks && !lifecycle.remotePaused(),
                            "Every real remote-base unload/reload must finish and recover");
                    long[] maintenance = MinecraftThermalInput.observedMaintenanceSaves(level);
                    // Regional maintenance captures before writing and need not defer a save.
                    helper.assertTrue(maintenance[1] > 0 && lifecycle.savedCycles == endTick / cycleTicks,
                            "Observe actual native maintenance writes and a related save in every cycle; saves=" + maintenance[1]);
                    helper.assertTrue(lifecycle.savedWhilePending > 0,
                            "A real related save must overlap pending worker activity");
                    FHMain.LOGGER.info("THERMAL_TWO_BASE_LIFECYCLE_RESULT,cycles={},pausedRemoteBaseTicks={},relatedSaves={},relatedSavesWithPendingAtCall={},{}",
                            lifecycle.completedCycles, lifecycle.pausedTicks, lifecycle.savedCycles, lifecycle.savedWhilePending,
                            MinecraftThermalInput.observedMainWaits(level));
                    if ((busyLifecycle)) {
                        var checkpoints = MinecraftThermalInput.observedCheckpointStatistics(level);
                        helper.assertTrue(checkpoints.pendingSaveSnapshots() > 0 && checkpoints.pendingUnloadSnapshots() > 0,
                                "Observe actual pending-worker snapshot saves AND unloads; do not move waiting between callbacks");
                    }
                    if (MinecraftThermalInput.usesNativeCheckpointScheduling(level)) {
                        var checkpoints = MinecraftThermalInput.observedNativeCheckpoints(level);
                        helper.assertTrue(checkpoints.requests() > 0 && checkpoints.prepared() > 0,
                                "The actual native unload continuation must prepare thermal checkpoints");
                        helper.assertTrue(checkpoints.pending() == 0 && checkpoints.fallbacks() == 0,
                                "Native unload requests must finish without retaining chunks or falling back to blocking");
                        FHMain.LOGGER.info("THERMAL_NATIVE_CHECKPOINT_LIFECYCLE,requests={},deferred={},prepared={},cancelled={},fallbacks={},pending={},peakPending={},maximumAgeTicks={}",
                                checkpoints.requests(), checkpoints.deferred(), checkpoints.prepared(), checkpoints.cancelled(),
                                checkpoints.fallbacks(), checkpoints.pending(), checkpoints.peakPending(), checkpoints.maximumAgeTicks());
                    }
                    // All end-of-workload observers run before shutdown, irrespective of
                    // GameTest's ordering of callbacks scheduled for the same tick.
                    helper.runAfterDelay(1, () -> {
                        long beforeClose = MinecraftThermalInput.thermalBudgetUsedBytes();
                        observedLifecycle = null;
                        for (BlockPos fire : fires) ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
                        MinecraftThermalInput.closeActiveLevel(level);
                        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.thermalBudgetUsedBytes() == 0,
                                "The real two-base shutdown must release all thermal reservations"))
                                .thenExecute(() -> FHMain.LOGGER.info("THERMAL_TWO_BASE_RELEASE,rootBeforeBytes={},rootAfterBytes={}",
                                        beforeClose, MinecraftThermalInput.thermalBudgetUsedBytes())).thenSucceed();
                    });
                }
        });
        for (int delay = 400; delay < endTick; delay += 400) helper.runAfterDelay(delay, () -> {
            for (BlockPos fire : fires) {
                if (lifecycle != null && lifecycle.remotePaused() && fire.getX() >= origins[1].getX()) continue;
                var fuel = level.getBlockEntity(fire).getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(
                        () -> new IllegalStateException("Real two-base campfire lost its fuel capability"));
                fuel.insertItem(0, new ItemStack(Items.COAL), false);
            }
        });
        int affectedBases = aOnly ? 1 : 2, affectedFires = aOnly ? 128 : 256;
        for (int offset = 0; offset < endTick; offset += cycleTicks) {
            int cycleOffset = offset;
            if (endTick >= cycleOffset + 601) {
                for (int base = 0; base < affectedBases; base++) {
                    int index = base;
                    if (cycleOffset + 601 + base * 100 > endTick) continue;
                    helper.runAfterDelay(cycleOffset + 601 + base * 100, () -> {
                        for (int y = 1; y <= 2; y++) level.setBlockAndUpdate(origins[index].offset(24, y, 6), Blocks.STONE.defaultBlockState());
                        FHMain.LOGGER.info("THERMAL_TWO_BASE_ACTION,elapsedTicks={},action=close_passage,base={}", cycleOffset + 601 + index * 100, index);
                    });
                }
            }
            if (endTick >= cycleOffset + 901) {
                helper.runAfterDelay(cycleOffset + 901, () -> {
                    for (int base = 0; base < affectedBases; base++) for (int y = 1; y <= 2; y++)
                        level.setBlockAndUpdate(origins[base].offset(24, y, 6), Blocks.AIR.defaultBlockState());
                    FHMain.LOGGER.info("THERMAL_TWO_BASE_ACTION,elapsedTicks={},action=open_passage,basesAffected={}", cycleOffset + 901, affectedBases);
                });
            }
            if (endTick >= cycleOffset + 1201) {
                helper.runAfterDelay(cycleOffset + 1201, () -> {
                    for (int index = 0; index < affectedFires; index += 2) ContinuousAirProductionGameTests.useItem(helper, fires[index], Items.IRON_SHOVEL);
                    FHMain.LOGGER.info("THERMAL_TWO_BASE_ACTION,elapsedTicks={},action=extinguish_half,firesAffected={}", cycleOffset + 1201, affectedFires / 2);
                });
            }
            if (endTick >= cycleOffset + 1501) {
                helper.runAfterDelay(cycleOffset + 1501, () -> {
                    for (int index = 0; index < affectedFires; index += 2) ContinuousAirProductionGameTests.useItem(helper, fires[index], Items.FLINT_AND_STEEL);
                    FHMain.LOGGER.info("THERMAL_TWO_BASE_ACTION,elapsedTicks={},action=reignite_half,firesAffected={}", cycleOffset + 1501, affectedFires / 2);
                });
            }
            if (endTick >= cycleOffset + 1801) {
                helper.runAfterDelay(cycleOffset + 1801, () -> {
                    for (int index = 0; index < affectedFires; index++) ContinuousAirProductionGameTests.useItem(helper, fires[index], Items.IRON_SHOVEL);
                    FHMain.LOGGER.info("THERMAL_TWO_BASE_ACTION,elapsedTicks={},action=extinguish_all,firesAffected={}", cycleOffset + 1801, affectedFires);
                });
            }
            if (endTick >= cycleOffset + 2201) {
                helper.runAfterDelay(cycleOffset + 2201, () -> {
                    for (int index = 0; index < affectedFires; index++) ContinuousAirProductionGameTests.useItem(helper, fires[index], Items.FLINT_AND_STEEL);
                    FHMain.LOGGER.info("THERMAL_TWO_BASE_ACTION,elapsedTicks={},action=reignite_all,firesAffected={}", cycleOffset + 2201, affectedFires);
                });
            }
        }
    }

    static void installOperatingMachines(GameTestHelper helper, BlockPos[] fires, int endTick, int cycleTicks) {
        helper.assertTrue(endTick >= 2400, "The actual machine start/stop/restart scene requires at least 2400 ticks");
        var level = helper.getLevel();
        BlockPos[] machines = new BlockPos[fires.length];
        for (int index = 0; index < machines.length; index++) {
            BlockPos machine = (index & 1) == 0 ? fires[index].south() : fires[index].west();
            machines[index] = machine;
            helper.assertTrue(level.getBlockState(machine).isAir(), "A machine must occupy actual free room Air");
            level.setBlockAndUpdate(machine, Blocks.FURNACE.defaultBlockState());
            if (index % 5 == 0) level.setBlockAndUpdate(machine.above(), Blocks.IRON_BLOCK.defaultBlockState());
        }
        for (int offset = 0; offset + 2400 <= endTick; offset += cycleTicks) {
            int cycleOffset = offset;
            for (int action : new int[] {120, 1900}) {
                int delay = cycleOffset + action;
                helper.runAfterDelay(delay, () -> {
                    for (BlockPos machine : machines) {
                        var furnace = (AbstractFurnaceBlockEntity) level.getBlockEntity(machine);
                        helper.assertTrue(furnace != null, "The placed machine must have its actual furnace block entity");
                        furnace.setItem(0, new ItemStack(Items.RAW_IRON, 2));
                        furnace.setItem(1, new ItemStack(Items.COAL));
                        furnace.setChanged();
                    }
                    FHMain.LOGGER.info("THERMAL_MACHINE_BASE_ACTION,elapsedTicks={},action=load_actual_recipe_and_fuel,machines={}", delay, machines.length);
                });
            }
            for (int action : new int[] {400, 1800, 2200}) {
                int delay = cycleOffset + action;
                helper.runAfterDelay(delay, () -> {
                    int lit = 0;
                    for (BlockPos machine : machines)
                        if (level.getBlockState(machine).getValue(AbstractFurnaceBlock.LIT)) lit++;
                    helper.assertTrue(lit == (action == 1800 ? 0 : machines.length),
                            "Actual furnace ticking must start, exhaust fuel and restart all machines; tick=" + delay + ",lit=" + lit);
                    FHMain.LOGGER.info("THERMAL_MACHINE_BASE_STATE,elapsedTicks={},machines={},lit={}", delay, machines.length, lit);
                });
            }
        }
        int recipeRuns = 2 * ((endTick - 2400) / cycleTicks + 1);
        helper.runAfterDelay(endTick, () -> {
            int produced = 0;
            for (BlockPos machine : machines) {
                var furnace = (AbstractFurnaceBlockEntity) level.getBlockEntity(machine);
                helper.assertTrue(furnace != null && furnace.getItem(2).is(Items.IRON_INGOT)
                                && furnace.getItem(2).getCount() >= recipeRuns * 2,
                        "All actual recipe runs must produce iron in every furnace");
                produced += furnace.getItem(2).getCount();
            }
            FHMain.LOGGER.info("THERMAL_MACHINE_BASE_RESULT,machines={},producedIron={},actualRecipeRunsPerMachine={}", machines.length, produced, recipeRuns);
        });
    }

    static void forceBase(net.minecraft.server.level.ServerLevel level, BlockPos origin, boolean forced) {
        for (int x = (origin.getX() - 16) >> 4; x <= (origin.getX() + 66) >> 4; x++)
            for (int z = (origin.getZ() - 16) >> 4; z <= (origin.getZ() + 30) >> 4; z++)
                level.setChunkForced(x, z, forced);
    }

    /** Ordinary disk saves and ticket removal, while the other base remains under observation. */
    private static final class BaseLifecycle {
        final GameTestHelper helper;
        final BlockPos origin, target;
        final BlockPos[] probes;
        final BlockPos[] fires;
        final boolean busyLifecycle;
        final boolean[] saved;
        final boolean[] autosaved;
        final java.lang.reflect.Method saveChunk = net.minecraftforge.fml.util.ObfuscationReflectionHelper.findMethod(
                net.minecraft.server.level.ChunkMap.class, "m_140258_", net.minecraft.world.level.chunk.ChunkAccess.class);
        int phase, started, reloaded, completedCycles, pausedTicks, savedCycles, savedWhilePending;
        long reloadedGameTick;
        long checkpointWaitNanosAtUnload;
        volatile net.minecraft.world.level.chunk.ChunkAccess unloadedChunk;

        BaseLifecycle(GameTestHelper helper, BlockPos origin, BlockPos[] fires, BlockPos[] probes, int cycles, boolean busyLifecycle) {
            this.helper = helper; this.origin = origin; this.target = fires[255]; this.probes = probes;
            this.fires = fires; this.busyLifecycle = busyLifecycle;
            saved = new boolean[cycles];
            autosaved = new boolean[cycles];
            FHMain.LOGGER.info("THERMAL_TWO_BASE_LIFECYCLE_SETUP,savingDisabled={},targetChunkX={},targetChunkZ={}",
                    helper.getLevel().noSave(), target.getX() >> 4, target.getZ() >> 4);
        }

        boolean remotePaused() { return phase != 0; }

        void tick(int elapsed) {
            var level = helper.getLevel();
            int cycle = (elapsed - 1) / 4000, position = (elapsed - 1) % 4000 + 1;
            if (busyLifecycle && !autosaved[cycle] && position >= 800 && position <= 840
                    && (MinecraftThermalInput.observedWorkerPending(level) || position == 840)) {
                boolean pending = MinecraftThermalInput.observedWorkerPending(level);
                var before = MinecraftThermalInput.observedCheckpointStatistics(level);
                long began = System.nanoTime();
                level.getServer().saveEverything(true, false, false);
                var after = MinecraftThermalInput.observedCheckpointStatistics(level);
                long waited = after.saveWaitNanos() - before.saveWaitNanos();
                if (MinecraftThermalInput.usesNativeCheckpointScheduling(level))
                    helper.assertTrue(waited <= 5_000_000L, "Actual non-flushing autosave must not block on thermal work: " + waited);
                autosaved[cycle] = true;
                FHMain.LOGGER.info("THERMAL_NATIVE_AUTOSAVE,elapsedTicks={},workerPendingAtCall={},fullFlush=false,saveCallUs={},thermalWaitUs={}",
                        elapsed, pending, (System.nanoTime() - began) / 1000, waited / 1000);
            }
            if (!saved[cycle] && position >= 300 && position <= 600
                    && (MinecraftThermalInput.observedWorkerPending(level) || position == 600)) {
                var chunk = level.getChunkAt(helper.absolutePos(new BlockPos(7, 1, 9)));
                boolean pending = MinecraftThermalInput.observedWorkerPending(level);
                chunk.setUnsaved(true);
                long began = System.nanoTime();
                try {
                    if (busyLifecycle) level.getServer().saveEverything(true, true, true);
                    else helper.assertTrue((boolean) saveChunk.invoke(level.getChunkSource().chunkMap, chunk),
                            "The actual related chunk must be written by ChunkMap");
                }
                catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
                saved[cycle] = true; savedCycles++;
                if (pending) savedWhilePending++;
                FHMain.LOGGER.info("THERMAL_TWO_BASE_SAVE,elapsedTicks={},actualChunkSaved=true,workerPendingAtCall={},saveCallUs={},{}",
                        elapsed, pending, (System.nanoTime() - began) / 1000, MinecraftThermalInput.observedMainWaits(level));
                if (busyLifecycle) {
                    helper.assertTrue(MinecraftThermalInput.dormantState(chunk) != null,
                            "The actual forced save must capture the related thermal chunk");
                    FHMain.LOGGER.info("THERMAL_BUSY_LIFECYCLE_FORCE_SAVE,elapsedTicks={},workerPendingAtCall={},fullFlush=true", elapsed, pending);
                }
            }
            boolean unloadNow = busyLifecycle
                    ? phase == 0 && position >= 2360 && position <= 2400 && completedCycles == cycle
                            && Math.floorMod(level.getGameTime(), 20) == 19
                    : position == 2401;
            if (busyLifecycle && unloadNow) {
                // Real construction, not a worker delay or fabricated thermal state.
                int changed = 0;
                boolean build = (completedCycles & 1) == 0;
                for (int index = 128; index < fires.length; index++) {
                    BlockPos ceilingDetail = fires[index].above(3);
                    helper.assertTrue(build ? level.getBlockState(ceilingDetail).isAir()
                                    : level.getBlockState(ceilingDetail).is(Blocks.IRON_BLOCK),
                            "Repeated construction must toggle the actual previously placed ceiling blocks");
                    level.setBlockAndUpdate(ceilingDetail, build ? Blocks.IRON_BLOCK.defaultBlockState()
                            : Blocks.AIR.defaultBlockState());
                    changed++;
                }
                FHMain.LOGGER.info("THERMAL_BUSY_LIFECYCLE_GEOMETRY,elapsedTicks={},actualIronBlocks={},actualAirBlocks={}",
                        elapsed, build ? changed : 0, build ? 0 : changed);
            }
            if (unloadNow) {
                helper.assertTrue(phase == 0, "The preceding lifecycle must have completed");
                phase = 1; started = elapsed;
                var checkpoints = MinecraftThermalInput.observedCheckpointStatistics(level);
                checkpointWaitNanosAtUnload = checkpoints.saveWaitNanos() + checkpoints.unloadWaitNanos();
                unloadedChunk = null;
                helper.assertTrue(level.getChunkSource().getChunkNow(target.getX() >> 4, target.getZ() >> 4) != null,
                        "The lifecycle target must actually be loaded before ticket removal");
                forceBase(level, origin, false);
                FHMain.LOGGER.info("THERMAL_TWO_BASE_UNLOAD_REQUEST,elapsedTicks={},base=1,targetChunkX={},targetChunkZ={},rootBytes={}",
                        elapsed, target.getX() >> 4, target.getZ() >> 4, MinecraftThermalInput.thermalBudgetUsedBytes());
            }
            if (phase != 0) pausedTicks++;
            if (phase == 1 && (elapsed - started == 4 || elapsed - started == 400)) {
                var nearbyTarget = new net.minecraft.world.level.ChunkPos(helper.absolutePos(new BlockPos(212, 7, 18)));
                FHMain.LOGGER.info("THERMAL_TWO_BASE_CHUNK_STATUS,elapsedTicks={},targetStatus={},nearbyReferenceStatus={}", elapsed,
                        level.getChunkSource().chunkMap.getChunkDebugData(new net.minecraft.world.level.ChunkPos(target)),
                        level.getChunkSource().chunkMap.getChunkDebugData(nearbyTarget));
            }
            if (phase == 1 && unloadedChunk != null && level.getChunkSource().getChunkNow(target.getX() >> 4, target.getZ() >> 4) == null) {
                FHMain.LOGGER.info("THERMAL_TWO_BASE_UNLOADED,elapsedTicks={},base=1,forgeUnloadObserved=true,waitTicks={},rootBytes={},{}",
                        elapsed, elapsed - started, MinecraftThermalInput.thermalBudgetUsedBytes(), MinecraftThermalInput.observedMainWaits(level));
                forceBase(level, origin, true);
                phase = 2;
            }
            if (phase == 2) {
                var targetChunk = level.getChunkSource().getChunkNow(target.getX() >> 4, target.getZ() >> 4);
                boolean loaded = targetChunk != null && targetChunk != unloadedChunk;
                for (int index = 8; index < probes.length; index++) loaded &= level.getChunkSource().getChunkNow(
                        probes[index].getX() >> 4, probes[index].getZ() >> 4) != null;
                if (loaded) {
                    phase = 3; reloaded = elapsed; reloadedGameTick = level.getGameTime();
                    FHMain.LOGGER.info("THERMAL_TWO_BASE_RELOADED,elapsedTicks={},base=1,rootBytes={}", elapsed, MinecraftThermalInput.thermalBudgetUsedBytes());
                }
            }
            if (phase == 3) {
                var air = new ThermalEnvironmentSample();
                boolean current = true;
                for (int index = 8; index < probes.length; index++) {
                    BlockPos probe = probes[index];
                    MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, probe, WorldTemperature.naturalAir(level, probe), air);
                    current &= air.airAvailable() && air.airSampleTick() >= reloadedGameTick && level.getGameTime() - air.airSampleTick() <= 40
                            && MinecraftThermalInput.describeAirRead(level, probe).startsWith("geometry_current");
                }
                helper.assertTrue(current || elapsed - reloaded <= 40, "The actually reloaded base must recover current Air within 40 ticks");
                if (current) {
                    if ((busyLifecycle)) {
                        var checkpoints = MinecraftThermalInput.observedCheckpointStatistics(level);
                        long waitNanos = checkpoints.saveWaitNanos() + checkpoints.unloadWaitNanos() - checkpointWaitNanosAtUnload;
                        helper.assertTrue(waitNanos <= 5_000_000L,
                                "Actual busy save/unload/recovery must not retain or move a thermal wait over5ms: " + waitNanos);
                        FHMain.LOGGER.info("THERMAL_SNAPSHOT_LIFECYCLE,elapsedTicks={},cycle={},thermalWaitUs={},pendingSaveSnapshots={},pendingUnloadSnapshots={}",
                                elapsed, cycle + 1, waitNanos / 1000, checkpoints.pendingSaveSnapshots(), checkpoints.pendingUnloadSnapshots());
                    }
                    phase = 0; completedCycles++;
                    FHMain.LOGGER.info("THERMAL_TWO_BASE_RECOVERED,elapsedTicks={},base=1,recoveryTicks={},cycle={},rootBytes={}",
                            elapsed, elapsed - reloaded, completedCycles, MinecraftThermalInput.thermalBudgetUsedBytes());
                }
            }
            helper.assertTrue(phase == 0 || elapsed - started < 500,
                    "Actual ticket removal, unload, reload and recovery must complete; phase=" + phase
                    + ",noSave=" + level.noSave() + "," + MinecraftThermalInput.observedMainWaits(level));
        }
    }
}

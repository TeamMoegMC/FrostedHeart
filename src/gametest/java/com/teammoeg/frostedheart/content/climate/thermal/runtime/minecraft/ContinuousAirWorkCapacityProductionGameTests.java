/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalWorkBudget;
import com.teammoeg.frostedheart.content.climate.thermal.solver.WorkPolicyObservation;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Fixed real-world capacity contract, separate from the already passed 129-source loop. */
@GameTestHolder("frostedheart_work_capacity")
@PrefixGameTestTemplate(false)
public final class ContinuousAirWorkCapacityProductionGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 4400)
    public static void twoDenseBasesKeepAnIndependentFineRoomCurrent(GameTestHelper helper) throws java.io.IOException {
        helper.assertTrue(ThermalWorkBudget.automatic(), "Capacity qualification must use the automatic policy");
        ContinuousAirPrecisionProductionGameTests.restoreReferenceClimate(helper, true);
        var level = helper.getLevel();
        BlockPos small = ContinuousAirProductionGameTests.room(helper, helper.absolutePos(BlockPos.ZERO));
        ContinuousAirProductionGameTests.useItem(helper, small, Items.FLINT_AND_STEEL);
        BlockPos[] origins = {helper.absolutePos(new BlockPos(517, 0, 7)), helper.absolutePos(new BlockPos(1029, 0, 7))};
        for (var origin : origins) for (int dx = -1; dx <= 4; dx++) for (int dz = -1; dz <= 3; dz++)
            level.setChunkForced((origin.getX() >> 4) + dx, (origin.getZ() >> 4) + dz, true);
        var bases = new ContinuousAirLargeBaseProductionGameTests.BuiltBase[] {
                ContinuousAirLargeBaseProductionGameTests.buildBase(helper, origins[0]),
                ContinuousAirLargeBaseProductionGameTests.buildBase(helper, origins[1])};
        BlockPos[] fires = new BlockPos[256];
        for (int base = 0; base < bases.length; base++) System.arraycopy(bases[base].fires(), 0, fires, base * 128, 128);
        // Reuse the established real recipe/fuel/exhaustion/restart fixture.
        ContinuousAirTwoBaseProductionGameTests.installOperatingMachines(helper, fires, 2600, 4000);
        var consumers = new net.minecraftforge.common.util.FakePlayer[100];
        for (int index = 0; index < consumers.length; index++) {
            String name = "workCapacity" + index;
            consumers[index] = net.minecraftforge.common.util.FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(
                    java.util.UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)), name));
        }
        var chunks = new java.util.HashSet<net.minecraft.world.level.chunk.LevelChunk>();
        for (var base : bases) for (var source : base.fires()) chunks.add(level.getChunkAt(source));
        for (var chunk : chunks) MinecraftThermalInput.onChunkLoad(level, chunk);
        WorkPolicyObservation.arm(small.above(), bases[0].probes()[0], bases[1].probes()[0]);
        var sample = new ThermalEnvironmentSample();
        int[] phase = {0}, ageFailures = {0}, geometryFailures = {0}, qualityFailures = {0}, timeRegressions = {0};
        long[] phaseTick = {0}, maximumControlAge = {0}, lastTicks = {-1, -1};
        long[] maximumBaseAge = {0, 0};
        int[][] advances = new int[6][2], baseUnavailable = new int[6][2];
        long[] queries = {0}; int[] consumerAgeFailures = {0};
        String[] names = {"warmup", "online", "single_build", "single_sources", "both_build", "quiet"};
        java.util.function.IntConsumer begin = value -> {
            phase[0] = value; phaseTick[0] = level.getGameTime();
            WorkPolicyObservation.mark(names[value], phaseTick[0]);
        };
        ContinuousAirProductionTiming.eachTick(helper, 4350, elapsed -> {
            long tick = level.getGameTime(), inPhase = tick - phaseTick[0];
            if ((phase[0] == 2 || phase[0] == 4) && inPhase > 0 && inPhase % 40 == 0) {
                boolean present = inPhase % 80 == 0;
                pillars(helper, bases[0], present);
                if (phase[0] == 4) pillars(helper, bases[1], present);
                FHMain.LOGGER.info("THERMAL_WORK_CAPACITY_ACTION,phase={},gameTick={},pillarsPresent={}", names[phase[0]], tick, present);
            }
            if (phase[0] == 3 && inPhase == 120) {
                for (int index = 0; index < 4; index++) ContinuousAirProductionGameTests.useItem(helper, bases[0].fires()[index], Items.FLINT_AND_STEEL);
                FHMain.LOGGER.info("THERMAL_WORK_CAPACITY_ACTION,phase=single_sources,gameTick={},relitSources=4", tick);
            }
            if (elapsed % 300 == 0) for (var base : bases) for (var fire : base.fires())
                level.getBlockEntity(fire).getCapability(ForgeCapabilities.ITEM_HANDLER)
                        .orElseThrow(() -> new IllegalStateException("Actual fuel capability disappeared"))
                        .insertItem(0, new ItemStack(Items.COAL), false);
            if (phase[0] == 0) return;
            for (int index = elapsed % 20; index < consumers.length; index += 20) {
                BlockPos fire = fires[(index % 2) * 128 + index / 2];
                consumers[index].setPos(fire.getX() + .5, fire.getY(), fire.getZ() + .5);
                MinecraftThermalInput.gameplayPlayerEnvironment(consumers[index], WorldTemperature.naturalAir(level, fire), sample);
                queries[0]++;
                if (!sample.airAvailable() || sample.airSampleTick() < 0 || tick - sample.airSampleTick() > 40) consumerAgeFailures[0]++;
            }
            read(helper, small.above(), sample);
            long age = tick - sample.airSampleTick(); maximumControlAge[0] = Math.max(maximumControlAge[0], age);
            if (!ContinuousAirProductionTiming.sourceField(sample) || age > 40) ageFailures[0]++;
            if (!MinecraftThermalInput.describeAirRead(level, small.above()).startsWith("geometry_current")) geometryFailures[0]++;
            if (span(helper, small.above()) != 1) qualityFailures[0]++;
            for (int base = 0; base < bases.length; base++) {
                read(helper, bases[base].probes()[0], sample);
                if (!ContinuousAirProductionTiming.sourceField(sample)) { baseUnavailable[phase[0]][base]++; continue; }
                long acceptedTick = sample.airSampleTick();
                maximumBaseAge[base] = Math.max(maximumBaseAge[base], tick - acceptedTick);
                if (acceptedTick < lastTicks[base]) timeRegressions[0]++;
                if (acceptedTick > lastTicks[base]) advances[phase[0]][base]++;
                lastTicks[base] = Math.max(lastTicks[base], acceptedTick);
                if (elapsed % 100 == 0) FHMain.LOGGER.info("THERMAL_WORK_CAPACITY_SAMPLE,phase={},gameTick={},baseIndex={},sampleTick={},gridStep={},airC={},geometry={}",
                        names[phase[0]], tick, base, acceptedTick, span(helper, bases[base].probes()[0]), sample.airTemperatureC(),
                        MinecraftThermalInput.describeAirRead(level, bases[base].probes()[0]));
            }
            if (elapsed % 100 == 0) FHMain.LOGGER.info("THERMAL_WORK_CAPACITY_CONTROL,phase={},gameTick={},maxAgeTicks={},ageFailures={},geometryFailures={},qualityFailures={}",
                    names[phase[0]], tick, maximumControlAge[0], ageFailures[0], geometryFailures[0], qualityFailures[0]);
        });
        helper.startSequence().thenIdle(400).thenWaitUntil(() -> {
            helper.assertTrue(MinecraftThermalInput.enabledPhysicalSourceCount(level) == 257, "Use two 128-source bases and one real fine room");
            for (var point : new BlockPos[] {small.above(), bases[0].probes()[0], bases[1].probes()[0]}) {
                read(helper, point, sample);
                helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample)
                        && MinecraftThermalInput.describeAirRead(level, point).startsWith("geometry_current"), "Cold preparation must finish before online measurement");
            }
        }).thenExecute(() -> begin.accept(1)).thenIdle(240).thenExecute(() -> {
            begin.accept(2); pillars(helper, bases[0], true);
        }).thenIdle(480).thenExecute(() -> {
            pillars(helper, bases[0], false); begin.accept(3);
            for (int index = 0; index < 4; index++) ContinuousAirProductionGameTests.useItem(helper, bases[0].fires()[index], Items.IRON_SHOVEL);
            FHMain.LOGGER.info("THERMAL_WORK_CAPACITY_ACTION,phase=single_sources,gameTick={},extinguishedSources=4", level.getGameTime());
        }).thenIdle(240).thenExecute(() -> {
            begin.accept(4); for (var base : bases) pillars(helper, base, true);
        }).thenIdle(480).thenExecute(() -> {
            for (var base : bases) pillars(helper, base, false); begin.accept(5);
        }).thenIdle(840).thenExecute(() -> {
            for (int stage = 1; stage < names.length; stage++) FHMain.LOGGER.info(
                    "THERMAL_WORK_CAPACITY_WINDOW,phase={},base0Advances={},base1Advances={},base0UnavailableTicks={},base1UnavailableTicks={}",
                    names[stage], advances[stage][0], advances[stage][1], baseUnavailable[stage][0], baseUnavailable[stage][1]);
            FHMain.LOGGER.info("THERMAL_WORK_CAPACITY_RESULT,maxControlAgeTicks={},ageFailures={},geometryFailures={},qualityFailures={},maxBase0AgeTicks={},maxBase1AgeTicks={},maxImbalanceJ={},timeRegressions={},peakRootBytes={},maxRecoveryJumpC={},machines=256,consumers=100,consumerQueries={},consumerAgeFailures={}",
                    maximumControlAge[0], ageFailures[0], geometryFailures[0], qualityFailures[0], maximumBaseAge[0], maximumBaseAge[1],
                    WorkPolicyObservation.maximumImbalanceJ(), timeRegressions[0] + WorkPolicyObservation.timeRegressions(),
                    MinecraftThermalInput.thermalBudgetPeakBytes(), WorkPolicyObservation.maximumRecoveryJumpC(), queries[0], consumerAgeFailures[0]);
            helper.assertTrue(ageFailures[0] == 0 && geometryFailures[0] == 0 && qualityFailures[0] == 0,
                    "A distant overloaded base must not stale, invalidate or coarsen the independent fine room");
            for (int stage = 1; stage < names.length; stage++) for (int base = 0; base < bases.length; base++)
                helper.assertTrue(advances[stage][base] >= 2, "Each real base must deliver new physical states in every fixed window");
            for (var base : bases) for (var point : base.probes()) {
                read(helper, point, sample);
                helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample) && level.getGameTime() - sample.airSampleTick() <= 40
                        && MinecraftThermalInput.describeAirRead(level, point).startsWith("geometry_current"), "Quiet operation must drain every real base probe");
            }
            helper.assertTrue(timeRegressions[0] + WorkPolicyObservation.timeRegressions() == 0
                    && WorkPolicyObservation.maximumImbalanceJ() < 1e-6, "Real source receipts and physical time must remain consistent");
            helper.assertTrue(MinecraftThermalInput.enabledPhysicalSourceCount(level) == 257, "Real source churn must restore all 257 fires");
            phase[0] = 0; WorkPolicyObservation.stop(); MinecraftThermalInput.closeActiveLevel(level);
        }).thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.thermalBudgetUsedBytes() == 0, "Release all task, source and field backing"))
        .thenExecute(() -> {
            for (var origin : origins) for (int dx = -1; dx <= 4; dx++) for (int dz = -1; dz <= 3; dz++)
                level.setChunkForced((origin.getX() >> 4) + dx, (origin.getZ() >> 4) + dz, false);
        }).thenSucceed();
    }

    private static void pillars(GameTestHelper helper, ContinuousAirLargeBaseProductionGameTests.BuiltBase base, boolean present) {
        for (var fire : base.fires()) for (int y = 1; y <= 4; y++) helper.getLevel().setBlockAndUpdate(fire.west().above(y),
                present ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
    }
    private static void read(GameTestHelper helper, BlockPos point, ThermalEnvironmentSample sample) {
        MinecraftThermalInput.gameplayPlacedReservoirEnvironment(helper.getLevel(), point, WorldTemperature.naturalAir(helper.getLevel(), point), sample);
    }
    private static int span(GameTestHelper helper, BlockPos point) {
        var publication = MinecraftThermalInput.queryPublicationForObservation(helper.getLevel());
        var cursor = new QueryPublication.ReadCursor();
        try {
            return publication != null && publication.beginRead(cursor)
                    ? cursor.airAggregationSpanAt(point.getX() + .5, point.getY() + .5, point.getZ() + .5) : -1;
        } finally { cursor.clear(); }
    }
}

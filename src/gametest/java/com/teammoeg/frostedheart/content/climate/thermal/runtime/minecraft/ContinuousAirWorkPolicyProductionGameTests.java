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

@GameTestHolder("frostedheart_work_policy")
@PrefixGameTestTemplate(false)
public final class ContinuousAirWorkPolicyProductionGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 2600)
    public static void realFineAndCoarseCostsAndCurrentFieldRecovery(GameTestHelper helper) throws java.io.IOException {
        ContinuousAirPrecisionProductionGameTests.restoreReferenceClimate(helper, true);
        var level = helper.getLevel();
        BlockPos small = ContinuousAirProductionGameTests.room(helper, helper.absolutePos(BlockPos.ZERO));
        ContinuousAirProductionGameTests.useItem(helper, small, Items.FLINT_AND_STEEL);
        BlockPos remote = helper.absolutePos(new BlockPos(517, 0, 7));
        for (int dx = -1; dx <= 4; dx++) for (int dz = -1; dz <= 3; dz++)
            level.setChunkForced((remote.getX() >> 4) + dx, (remote.getZ() >> 4) + dz, true);
        var base = ContinuousAirLargeBaseProductionGameTests.buildBase(helper, remote);
        var chunks = new java.util.HashSet<net.minecraft.world.level.chunk.LevelChunk>();
        for (var fire : base.fires()) chunks.add(level.getChunkAt(fire));
        for (var chunk : chunks) MinecraftThermalInput.onChunkLoad(level, chunk);
        var sample = new ThermalEnvironmentSample();
        boolean[] monitoring = {false}; long[] maximumAge = {0}; int[] ageFailures = {0}, geometryFailures = {0};
        boolean[] autoBuilding = {false}; long[] autoStarted = {0}; int[] constructionActions = {0};
        WorkPolicyObservation.arm(small.above(), base.probes()[0]);
        for (int delay = 1; delay < 2550; delay++) {
            int elapsed = delay;
            helper.runAfterDelay(delay, () -> {
                if (autoBuilding[0] && level.getGameTime() > autoStarted[0]
                        && (level.getGameTime() - autoStarted[0]) % 40 == 0) {
                    boolean present = (level.getGameTime() - autoStarted[0]) % 80 == 0;
                    pillars(helper, base, present); constructionActions[0]++;
                    FHMain.LOGGER.info("THERMAL_WORK_POLICY_ACTION,gameTick={},pillarsPresent={},constructionActions={}",
                            level.getGameTime(), present, constructionActions[0]);
                }
                if (elapsed % 300 == 0) for (var fire : base.fires())
                    level.getBlockEntity(fire).getCapability(ForgeCapabilities.ITEM_HANDLER)
                            .orElseThrow(() -> new IllegalStateException("Real fuel capability disappeared"))
                            .insertItem(0, new ItemStack(Items.COAL), false);
                if (!monitoring[0]) return;
                read(helper, small.above(), sample);
                long age = level.getGameTime() - sample.airSampleTick(); maximumAge[0] = Math.max(maximumAge[0], age);
                if (!ContinuousAirProductionTiming.sourceField(sample) || age > 40 || span(helper, small.above()) != 1) ageFailures[0]++;
                if (!MinecraftThermalInput.describeAirRead(level, small.above()).startsWith("geometry_current")) geometryFailures[0]++;
            });
        }
        var sequence = helper.startSequence().thenIdle(400).thenWaitUntil(() -> {
            helper.assertTrue(MinecraftThermalInput.enabledPhysicalSourceCount(level) == 129, "Use 129 actual burning fires");
            for (var point : new BlockPos[] {small.above(), base.probes()[0]}) {
                read(helper, point, sample);
                helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample)
                        && (span(helper, point) == 1 || ThermalWorkBudget.automatic() && !point.equals(small.above()) && span(helper, point) == 0)
                        && MinecraftThermalInput.describeAirRead(level, point).startsWith("geometry_current"), "Actual fine fields must be current");
            }
        }).thenExecute(() -> {
            monitoring[0] = true;
        });
        if (ThermalWorkBudget.automatic()) sequence.thenExecute(() -> {
            autoBuilding[0] = true; autoStarted[0] = level.getGameTime();
            WorkPolicyObservation.mark("auto_build", level.getGameTime()); pillars(helper, base, true);
        }).thenIdle(480).thenExecute(() -> {
            autoBuilding[0] = false;
            WorkPolicyObservation.mark("auto_quiet", level.getGameTime()); pillars(helper, base, false);
            helper.assertTrue(WorkPolicyObservation.coarseCuts() >= 12,
                    "Fixed real construction window must deliver minimum-model cuts without a manual quality request");
        }).thenWaitUntil(() -> helper.assertTrue(span(helper, base.probes()[0]) == 1 && WorkPolicyObservation.recoveredCuts() > 0,
                "After actual construction stops, the policy must recover from current coarse values"));
        else sequence.thenExecute(() -> {
            WorkPolicyObservation.mark("fine_build", level.getGameTime()); pillars(helper, base, true);
        }).thenIdle(240).thenExecute(() -> {
            WorkPolicyObservation.mark("fine_remove", level.getGameTime()); pillars(helper, base, false);
        }).thenIdle(120).thenExecute(() -> {
            WorkPolicyObservation.mark("coarse_select", level.getGameTime()); WorkPolicyObservation.request(4);
        }).thenWaitUntil(() -> helper.assertTrue(span(helper, base.probes()[0]) == 0, "Actual whole region must install the bounded minimum model"))
        .thenIdle(100).thenExecute(() -> {
            WorkPolicyObservation.mark("coarse_build", level.getGameTime()); pillars(helper, base, true);
        }).thenIdle(240).thenExecute(() -> {
            WorkPolicyObservation.mark("coarse_remove", level.getGameTime()); pillars(helper, base, false);
        }).thenIdle(120).thenExecute(() -> {
            WorkPolicyObservation.mark("fine_recover", level.getGameTime()); WorkPolicyObservation.request(1);
        }).thenWaitUntil(() -> helper.assertTrue(span(helper, base.probes()[0]) == 1, "Recover from current coarse field"));
        sequence.thenIdle(ThermalWorkBudget.automatic() ? 360 : 240).thenExecute(() -> {
            monitoring[0] = false;
            FHMain.LOGGER.info("THERMAL_WORK_POLICY_RESULT,maxControlAgeTicks={},ageFailures={},geometryFailures={},coarseCuts={},recoveredCuts={},maxImbalanceJ={},peakRootBytes={},maxRecoveryJumpC={},recoveryProjectionSamples={},timeRegressions={}",
                    maximumAge[0], ageFailures[0], geometryFailures[0], WorkPolicyObservation.coarseCuts(), WorkPolicyObservation.recoveredCuts(),
                    WorkPolicyObservation.maximumImbalanceJ(), MinecraftThermalInput.thermalBudgetPeakBytes(),
                    WorkPolicyObservation.maximumRecoveryJumpC(), WorkPolicyObservation.recoveryProjectionSamples(), WorkPolicyObservation.timeRegressions());
            helper.assertTrue(ageFailures[0] == 0 && geometryFailures[0] == 0, "Independent fine room must remain current");
            helper.assertTrue(WorkPolicyObservation.coarseCuts() >= 12 && WorkPolicyObservation.recoveredCuts() >= 8
                    && WorkPolicyObservation.maximumImbalanceJ() < 1e-6, "Current field must advance in both qualities without losing source energy");
            helper.assertTrue(WorkPolicyObservation.recoveryProjectionSamples() > 0 && WorkPolicyObservation.maximumRecoveryJumpC() < 1e-4,
                    "Recovery must project the actually accepted current coarse field at the same physical time");
            read(helper, base.probes()[0], sample);
            helper.assertTrue(WorkPolicyObservation.timeRegressions() == 0 && span(helper, base.probes()[0]) == 1
                    && ContinuousAirProductionTiming.sourceField(sample) && level.getGameTime() - sample.airSampleTick() <= 40,
                    "Recovered fine time must stay monotonic and actually current");
            WorkPolicyObservation.stop(); MinecraftThermalInput.closeActiveLevel(level);
        }).thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.thermalBudgetUsedBytes() == 0, "Release all task/field backing"))
        .thenExecute(() -> {
            for (int dx = -1; dx <= 4; dx++) for (int dz = -1; dz <= 3; dz++)
                level.setChunkForced((remote.getX() >> 4) + dx, (remote.getZ() >> 4) + dz, false);
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

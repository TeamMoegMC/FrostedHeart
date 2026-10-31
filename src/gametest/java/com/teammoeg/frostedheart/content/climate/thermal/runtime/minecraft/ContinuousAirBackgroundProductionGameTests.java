/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.solver.RegionalGridSwitchObservation;
import com.teammoeg.frostedheart.content.climate.thermal.solver.SourceStepProductionObservation;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Real connected rooms crossing the Page baselines seen in the growth failure. */
@GameTestHolder("frostedheart_regional_background")
@PrefixGameTestTemplate(false)
public final class ContinuousAirBackgroundProductionGameTests {
    @GameTest(template = "phase0a_empty", batch = "regional_background", timeoutTicks = 2400)
    public static void differentNaturalBaselinesSurviveMinimumConstructionCoolingAndRecovery(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(72, 0, 0)), otherOrigin = origin.east(256);
        force(helper, origin, true); force(helper, otherOrigin, true);
        BlockPos fire = ContinuousAirProductionGameTests.room(helper, origin);
        BlockPos control = ContinuousAirProductionGameTests.room(helper, otherOrigin);
        for (int y = 1; y <= 16; y++) level.setBlockAndUpdate(fire.above(y), Blocks.AIR.defaultBlockState());
        BlockPos[] fires = {fire, fire.above(6).east(2), fire.above(12).east(2)};
        for (int index = 1; index < fires.length; index++) {
            var source = fires[index];
            level.setBlockAndUpdate(source.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(source, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
            level.getBlockEntity(source).getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(
                    () -> new IllegalStateException("Actual campfire fuel capability is missing"))
                    .insertItem(0, new ItemStack(Items.COAL, 4), false);
        }
        for (var source : fires) ContinuousAirProductionGameTests.useItem(helper, source, Items.FLINT_AND_STEEL);
        ContinuousAirProductionGameTests.useItem(helper, control, Items.FLINT_AND_STEEL);
        var sample = new ThermalEnvironmentSample();
        long[] coarseTick = {0};
        helper.startSequence().thenIdle(300).thenWaitUntil(() -> {
            read(helper, fire.above(), sample);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample) && span(helper, fire.above()) == 1,
                    "The real cross-baseline room starts with published fine Air");
            helper.assertTrue(span(helper, control.above()) == 1, "The independent control starts fine");
        }).thenExecute(() -> {
            RegionalGridSwitchObservation.arm(fire, control); RegionalGridSwitchObservation.request(4);
        }).thenWaitUntil(() -> {
            check(helper, 1);
            helper.assertTrue(span(helper, fire.above()) == 0 && span(helper, control.above()) == 1,
                    "Only the selected actual field enters the minimum network");
            helper.assertTrue(RegionalGridSwitchObservation.maximumBackgroundRangeC() > 8,
                    "Use genuinely different captured Page baselines, rather than a uniform synthetic field");
        }).thenExecute(() -> {
            SourceStepProductionObservation.arm(fire);
        }).thenWaitUntil(() -> {
            helper.assertTrue(SourceStepProductionObservation.failure() == null, SourceStepProductionObservation.summary());
            helper.assertTrue(SourceStepProductionObservation.completed(), "The actual minimum input cancels twice and publishes once");
        }).thenExecute(() -> {
            FHMain.LOGGER.info("THERMAL_BACKGROUND_INPUT_PRODUCTION,{}", SourceStepProductionObservation.summary());
            SourceStepProductionObservation.end();
            read(helper, fire.above(), sample); coarseTick[0] = sample.airSampleTick();
            for (int index = 1; index < fires.length; index++) ContinuousAirProductionGameTests.useItem(helper, fires[index], Items.IRON_SHOVEL);
            level.setBlockAndUpdate(fire.east(3).above(), Blocks.STONE.defaultBlockState());
        }).thenIdle(240).thenWaitUntil(() -> {
            read(helper, fire.above(), sample);
            helper.assertTrue(sample.airSampleTick() >= coarseTick[0] + 160
                            && level.getGameTime() - sample.airSampleTick() <= 40
                            && MinecraftThermalInput.describeAirRead(level, fire.above()).startsWith("geometry_current"),
                    "Minimum Air keeps advancing through real cooling and construction");
            helper.assertTrue(RegionalGridSwitchObservation.maximumCells() < 16384,
                    "This small real room must not accumulate tens of thousands of thermal bodies: " + RegionalGridSwitchObservation.summary());
        }).thenExecute(() -> RegionalGridSwitchObservation.request(1)).thenWaitUntil(() -> {
            check(helper, 2);
            helper.assertTrue(span(helper, fire.above()) == 1 && span(helper, control.above()) == 1,
                    "Fine reconstruction restores the current complete field while the control stays fine");
        }).thenExecute(() -> {
            FHMain.LOGGER.info("THERMAL_BACKGROUND_PRODUCTION,{}", RegionalGridSwitchObservation.summary());
            RegionalGridSwitchObservation.end();
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
            ContinuousAirProductionGameTests.useItem(helper, control, Items.IRON_SHOVEL);
            MinecraftThermalInput.closeActiveLevel(level);
        }).thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.thermalBudgetUsedBytes() == 0,
                "Background, readers, candidates and notices release their actual reservations"))
        .thenExecute(() -> { force(helper, origin, false); force(helper, otherOrigin, false); }).thenSucceed();
    }

    private static void check(GameTestHelper helper, int completed) {
        helper.assertTrue(RegionalGridSwitchObservation.failure() == null, RegionalGridSwitchObservation.summary());
        helper.assertTrue(RegionalGridSwitchObservation.completed() >= completed, "Wait for the actual quality publication");
    }
    private static void read(GameTestHelper helper, BlockPos point, ThermalEnvironmentSample sample) {
        MinecraftThermalInput.gameplayPlacedReservoirEnvironment(helper.getLevel(), point,
                WorldTemperature.naturalAir(helper.getLevel(), point), sample);
    }
    private static int span(GameTestHelper helper, BlockPos point) {
        var publication = MinecraftThermalInput.queryPublicationForObservation(helper.getLevel());
        var cursor = new QueryPublication.ReadCursor();
        try {
            if (publication == null || !publication.beginRead(cursor)) return -1;
            return cursor.airAggregationSpanAt(point.getX() + .5, point.getY() + .5, point.getZ() + .5);
        } finally { cursor.clear(); }
    }
    private static void force(GameTestHelper helper, BlockPos origin, boolean forced) {
        for (int x = (origin.getX() >> 4) - 1; x <= ((origin.getX() + 16) >> 4) + 1; x++)
            for (int z = (origin.getZ() >> 4) - 1; z <= ((origin.getZ() + 16) >> 4) + 1; z++)
                helper.getLevel().setChunkForced(x, z, forced);
    }
}

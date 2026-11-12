/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PreparationLifetimeProbe;
import com.teammoeg.frostedheart.content.climate.thermal.solver.RegionalAirRuntime;
import java.lang.ref.WeakReference;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_preparation_retention")
@PrefixGameTestTemplate(false)
public final class ThermalPreparationRetentionGameTests {
    @GameTest(template = "phase0a_empty", batch = "thermal_preparation_retention", timeoutTicks = 2200)
    public static void finishedPreparationsAreCollectableWhileHeatedRegionsStayLive(GameTestHelper helper) {
        var probe = new PreparationLifetimeProbe();
        var level = helper.getLevel();
        var first = helper.absolutePos(new BlockPos(80, 0, 0));
        var second = helper.absolutePos(new BlockPos(240, 0, 0));
        for (var origin : List.of(first, second))
            for (int x = (origin.getX() >> 4) - 1; x <= (origin.getX() >> 4) + 2; x++)
                for (int z = (origin.getZ() >> 4) - 1; z <= (origin.getZ() >> 4) + 2; z++) level.setChunkForced(x, z, true);
        var stableFire = ContinuousAirProductionGameTests.room(helper, first);
        var changingFire = ContinuousAirProductionGameTests.room(helper, second);
        ContinuousAirProductionGameTests.useItem(helper, stableFire, Items.FLINT_AND_STEEL);
        ContinuousAirProductionGameTests.useItem(helper, changingFire, Items.FLINT_AND_STEEL);
        var sample = new ThermalEnvironmentSample();
        @SuppressWarnings("unchecked")
        List<WeakReference<RegionalAirRuntime.RebuildPreparation>>[] retired = new List[1];
        helper.startSequence().thenIdle(200).thenWaitUntil(() -> assertHeated(helper, stableFire, sample))
                .thenExecute(() -> ContinuousAirProductionTiming.eachTick(helper, 640, elapsed -> {
                    if (elapsed % 80 == 40) level.setBlockAndUpdate(changingFire.below(),
                            ((elapsed / 80) & 1) == 0 ? Blocks.IRON_BLOCK.defaultBlockState() : Blocks.STONE.defaultBlockState());
                })).thenIdle(800).thenWaitUntil(() -> assertHeated(helper, stableFire, sample))
                .thenExecute(() -> {
                    retired[0] = probe.completedBefore(level.getGameTime() - 40);
                    helper.assertTrue(retired[0].size() >= 4, "Observe actual completed builders across real material changes");
                }).thenIdle(2).thenExecute(System::gc).thenIdle(5).thenExecute(System::gc).thenIdle(5)
                .thenWaitUntil(() -> {
                    long retained = retired[0].stream().filter(reference -> reference.get() != null).count();
                    helper.assertTrue(retained == 0, "Live region listeners must not retain finished rebuild preparation; retained=" + retained);
                }).thenExecute(() -> {
                    assertHeated(helper, stableFire, sample);
                    helper.assertTrue(MinecraftThermalInput.thermalBudgetUsedBytes() > 0, "Collect builders before closing the live thermal runtime");
                    FHMain.LOGGER.info("THERMAL_PREPARATION_RETENTION_RESULT,completedPreparations={},retainedPreparations=0,runtimeStillLive=true",
                            retired[0].size());
                    probe.close();
                    MinecraftThermalInput.closeActiveLevel(level);
                }).thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.thermalBudgetUsedBytes() == 0,
                        "Normal cleanup still returns all charged thermal state"))
                .thenExecute(() -> {
                    for (var origin : List.of(first, second))
                        for (int x = (origin.getX() >> 4) - 1; x <= (origin.getX() >> 4) + 2; x++)
                            for (int z = (origin.getZ() >> 4) - 1; z <= (origin.getZ() >> 4) + 2; z++) level.setChunkForced(x, z, false);
                }).thenSucceed();
    }

    private static void assertHeated(GameTestHelper helper, BlockPos fire, ThermalEnvironmentSample sample) {
        ThermalConvergenceScenes.read(helper, fire.above(), sample);
        helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample)
                        && helper.getLevel().getGameTime() - sample.airSampleTick() <= 40,
                "The unchanged heated region remains live while old construction state is collected");
    }
}

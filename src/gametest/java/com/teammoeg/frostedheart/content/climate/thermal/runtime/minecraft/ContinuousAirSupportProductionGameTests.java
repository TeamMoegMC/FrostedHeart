/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Real wall edits must publish the connected rooms and preserve heat when they separate again. */
@GameTestHolder("frostedheart_support_production")
@PrefixGameTestTemplate(false)
public final class ContinuousAirSupportProductionGameTests {
    private ContinuousAirSupportProductionGameTests() {}

    @GameTest(template = "phase0a_empty", timeoutTicks = 1400)
    public static void openingAndClosingWallRediscoversSourceSupport(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        BlockPos fire = ContinuousAirProductionGameTests.room(helper, origin);
        for (int y = 1; y < 8; y++) for (int z = 1; z < 16; z++)
            level.setBlockAndUpdate(origin.offset(10, y, z), Blocks.STONE.defaultBlockState());
        ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
        BlockPos near = fire.above();
        BlockPos beyond = origin.offset(12, 2, 8);
        for (int stage = 0; stage < 4; stage++) {
            int observedStage = stage;
            helper.runAfterDelay(300L * (stage + 1), () -> {
                var nearAir = new ThermalEnvironmentSample();
                var beyondAir = new ThermalEnvironmentSample();
                MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, near,
                        WorldTemperature.naturalAir(level, near), nearAir);
                MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, beyond,
                        WorldTemperature.naturalAir(level, beyond), beyondAir);
                boolean open = (observedStage & 1) != 0;
                FHMain.LOGGER.info("THERMAL_SUPPORT_WALL,stage={},open={},nearTerms={},beyondTerms={},nearRead={},beyondRead={},worker={}",
                        observedStage, open, nearAir.airBasisTerms(), beyondAir.airBasisTerms(),
                        MinecraftThermalInput.describeAirRead(level, near),
                        MinecraftThermalInput.describeAirRead(level, beyond),
                        MinecraftThermalInput.describeWorkerProgress(level));
                helper.assertTrue(ContinuousAirProductionTiming.sourceField(nearAir)
                                && level.getGameTime() - nearAir.airSampleTick() <= 40,
                        "Source room must retain fresh local Air after actual wall edits");
                boolean geometryQuiet = level.getGameTime()
                        - MinecraftThermalInput.lastGeometryInvalidationTick(level) > 40;
                if (geometryQuiet)
                    helper.assertTrue(MinecraftThermalInput.describeAirRead(level, near).startsWith("geometry_current"),
                            "Source room geometry must become current after mutations settle");
                if (open)
                    helper.assertTrue(ContinuousAirProductionTiming.sourceField(beyondAir),
                            "Opening the real doorway must publish the new room, stage=" + observedStage);
                // On reclosure the far room can retain its spatial temperature and residual heat.
                if (open) {
                    helper.assertTrue(level.getGameTime() - beyondAir.airSampleTick() <= 40,
                            "Newly opened branch must publish fresh Air");
                    if (geometryQuiet)
                        helper.assertTrue(MinecraftThermalInput.describeAirRead(level, beyond).startsWith("geometry_current"),
                                "Newly opened branch geometry must become current after mutations settle");
                }
                if (observedStage == 3) {
                    helper.assertTrue(MinecraftThermalInput.workLimitedCompletions(level) == 0,
                            "Repeated wall edits must fit the production budget");
                    helper.succeed();
                    return;
                }
                for (int y = 1; y <= 3; y++) for (int z = 7; z <= 9; z++)
                    level.setBlockAndUpdate(origin.offset(10, y, z),
                            (open ? Blocks.STONE : Blocks.AIR).defaultBlockState());
            });
        }
    }
}

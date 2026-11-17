/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameRules;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;

@GameTestHolder("frostedheart_retirement_room")
@PrefixGameTestTemplate(false)
public final class ThermalRetirementRoomGameTests {
    private ThermalRetirementRoomGameTests() { }

    @GameTest(template = "phase0a_empty", batch = "retirement_sky", timeoutTicks = 2400)
    public static void exposedSparseBricksCoolAtEverySectionHeight(GameTestHelper helper) {
        var level = helper.getLevel();
        boolean daylight = level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        BlockPos[] fires = new BlockPos[4];
        double[] rises = new double[4];
        var sample = new ThermalEnvironmentSample();
        for (int index = 0; index < fires.length; index++) {
            BlockPos fire = fires[index] = new BlockPos(8193 + index * 48, 194 + index * 4, 8193);
            ThermalConvergenceScenes.force(helper, fire, 0, true);
            level.setBlockAndUpdate(fire.below(), Blocks.STONE.defaultBlockState());
            ThermalConvergenceScenes.fire(helper, fire);
        }
        helper.startSequence().thenIdle(400).thenWaitUntil(() -> {
            for (int index = 0; index < fires.length; index++) {
                var fire = fires[index];
                helper.assertTrue(ThermalConvergenceScenes.fresh(helper, fire.above(), sample), "Every actual fire must have fresh Air");
                double natural = WorldTemperature.naturalAir(level, new BlockPos((fire.getX() & ~15) + 8,
                        (fire.getY() & ~15) + 8, (fire.getZ() & ~15) + 8));
                rises[index] = sample.airTemperatureC() - natural;
            }
        }).thenExecute(() -> {
            double low = java.util.Arrays.stream(rises).min().orElseThrow();
            double high = java.util.Arrays.stream(rises).max().orElseThrow();
            helper.assertTrue(low > .5 && high - low < 1,
                    "The exposed upper boundary must not depend on being the last Brick of a Section: " + java.util.Arrays.toString(rises));
            level.setBlockAndUpdate(fires[1].above(5), Blocks.STONE.defaultBlockState());
        }).thenIdle(160).thenWaitUntil(() -> {
            helper.assertTrue(ThermalConvergenceScenes.fresh(helper, fires[1].above(), sample), "Roof change must publish current geometry");
            double natural = WorldTemperature.naturalAir(level, new BlockPos((fires[1].getX() & ~15) + 8,
                    (fires[1].getY() & ~15) + 8, (fires[1].getZ() & ~15) + 8));
            helper.assertTrue(sample.airTemperatureC() - natural > rises[1] + .05,
                    "A new roof above a lower sparse Brick must actually reduce its sky cooling");
        }).thenExecute(() -> {
            level.setBlockAndUpdate(fires[1].above(5), Blocks.AIR.defaultBlockState());
            for (var fire : fires) ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
        }).thenWaitUntil(() -> {
            for (var fire : fires) helper.assertTrue(!MinecraftThermalInput.describeAirRead(level, fire.above()).startsWith("geometry_current"),
                    "Exposed fires must cool and retire at each of the four Section-relative heights");
        }).thenExecute(() -> {
            for (var fire : fires) ThermalConvergenceScenes.force(helper, fire, 0, false);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(daylight, level.getServer());
            FHMain.LOGGER.info("THERMAL_RETIREMENT_SKY,rises={},statistics={}", java.util.Arrays.toString(rises), DormantThermalMaintenance.statistics());
        }).thenSucceed();
    }

    @GameTest(template = "phase0a_empty", batch = "retirement_ground", timeoutTicks = 1800)
    public static void groundSoilContinuesReleasingLatentEnergyAfterTheFireStops(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = new BlockPos(4096, 80, 4096);
        ThermalConvergenceScenes.force(helper, origin, 8, true);
        for (int y = 0; y <= 4; y++) for (int z = 0; z <= 6; z++) for (int x = 0; x <= 6; x++)
            level.setBlockAndUpdate(origin.offset(x, y, z), (x == 0 || x == 6 || z == 0 || z == 6 || y == 0 || y == 4
                    ? Blocks.STONE : Blocks.AIR).defaultBlockState());
        BlockPos fire = origin.offset(3, 1, 3);
        // Put the phase material on the actual room boundary. Unrelated natural
        // ground is not required to become resident merely because a fire exists.
        for (int z = 2; z <= 4; z++) for (int x = 2; x <= 4; x++)
            level.setBlockAndUpdate(origin.offset(x, 0, z), Blocks.DIRT.defaultBlockState());
        ThermalConvergenceScenes.fire(helper, fire);
        var firstEnergy = new it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap();
        firstEnergy.defaultReturnValue(Double.NaN);
        var soil = new com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample();
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                        MinecraftThermalInput.sampleMaterial(level, origin.offset(2, 0, 2), soil)
                                && soil.law().transition(soil.branch()) != null,
                        "The deliberately placed ground must enter an actual phase publication"))
                .thenIdle(400).thenExecute(() -> ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL))
                .thenWaitUntil(() -> helper.assertTrue(findCoolingLatent(helper, origin, firstEnergy),
                        "Naturally freezing ground must release latent heat after the fire stops; live and stored bodies share this requirement"))
                .thenExecute(() -> ThermalConvergenceScenes.force(helper, origin, 8, false)).thenSucceed();
    }

    private static boolean findCoolingLatent(GameTestHelper helper, BlockPos origin,
            it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap firstEnergy) {
        var level = helper.getLevel();
        if (level.getGameTime() % 20 != 0) return false;
        var sample = new com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample();
        for (int x = (origin.getX() >> 4) - 1; x <= (origin.getX() >> 4) + 1; x++)
            for (int z = (origin.getZ() >> 4) - 1; z <= (origin.getZ() >> 4) + 1; z++) {
                var chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) continue;
                for (int dy = -4; dy <= 1; dy++) for (int dz = 0; dz < 16; dz += 2) for (int dx = 0; dx < 16; dx += 2) {
                    BlockPos p = new BlockPos(x * 16 + dx, origin.getY() + dy, z * 16 + dz);
                    if (!MinecraftThermalInput.sampleMaterial(level, p, sample)) continue;
                    var edge = sample.law().transition(sample.branch());
                    if (edge == null || edge.heating() || edge.progress(sample.enthalpyJ()) <= 0) continue;
                    double initial = firstEnergy.get(p.asLong());
                    if (Double.isNaN(initial)) { firstEnergy.put(p.asLong(), sample.enthalpyJ()); continue; }
                    if (sample.enthalpyJ() < initial - 1) {
                        FHMain.LOGGER.info("THERMAL_RETIREMENT_GROUND,position={},source={},releasedJ={},progress={},statistics={}",
                                p, sample.source(), initial - sample.enthalpyJ(), edge.progress(sample.enthalpyJ()), DormantThermalMaintenance.statistics());
                        return true;
                    }
                }
            }
        return false;
    }
}

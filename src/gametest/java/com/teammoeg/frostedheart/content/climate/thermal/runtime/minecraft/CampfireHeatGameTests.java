/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_campfire_heat")
@PrefixGameTestTemplate(false)
public final class CampfireHeatGameTests {
    @GameTest(template = "phase0a_empty", batch = "campfire_six_face_heat", timeoutTicks = 850)
    public static void blockedCampfireHeatsItsBodyAndAllSixSolidNeighbors(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // The west and upper neighbors cross Chunk and Section boundaries respectively.
        BlockPos fire = new BlockPos(20736, 95, 20744);
        int chunkX = fire.getX() >> 4;
        int chunkZ = fire.getZ() >> 4;
        level.setChunkForced(chunkX, chunkZ, true);
        level.setChunkForced(chunkX - 1, chunkZ, true);
        for (BlockPos position : BlockPos.betweenClosed(fire.offset(-1, -1, -1), fire.offset(1, 1, 1))) {
            level.setBlockAndUpdate(position, Blocks.STONE.defaultBlockState());
        }
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        ((ICampfireExtra) level.getBlockEntity(fire)).setLifeTime(2_000);
        ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);

        MaterialSample sample = new MaterialSample();
        double[] before = new double[1];
        long[] extinguishedAt = new long[1];
        helper.startSequence().thenIdle(320).thenWaitUntil(() -> {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = fire.relative(direction);
                helper.assertTrue(MinecraftThermalInput.sampleMaterialTemperature(level, neighbor, sample)
                                && sample.source() == MaterialSample.Source.LIVE
                                && sample.temperatureC() > WorldTemperature.naturalAir(level, neighbor) + 0.05,
                        "The campfire must warm the solid neighbor to the " + direction + ": " + sample.temperatureC());
            }
            helper.assertTrue(MinecraftThermalInput.sampleMaterialTemperature(level, fire, sample)
                            && sample.source() == MaterialSample.Source.LIVE
                            && sample.temperatureC() > WorldTemperature.naturalAir(level, fire) + 10,
                    "An enclosed campfire must have a hot material body with the upper Air outlet blocked");
            before[0] = sample.temperatureC();
            helper.assertTrue(Math.abs(WorldTemperature.getTempAtBlock(level, fire) - before[0]) < 1e-4,
                    "The block temperature probe must read the heated body");
        }).thenExecute(() -> {
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
            extinguishedAt[0] = level.getGameTime();
        }).thenIdle(100).thenWaitUntil(() -> {
            helper.assertTrue(MinecraftThermalInput.sampleMaterialTemperature(level, fire, sample)
                            && sample.source() == MaterialSample.Source.LIVE
                            && sample.sampleTick() >= extinguishedAt[0] + 80
                            && sample.temperatureC() < before[0] - 0.5
                            && sample.temperatureC() > WorldTemperature.naturalAir(level, fire) + 5,
                    "The extinguished body must retain heat and cool: " + sample.temperatureC() + " vs " + before[0]);
        }).thenExecute(() -> {
            level.setChunkForced(chunkX, chunkZ, false);
            level.setChunkForced(chunkX - 1, chunkZ, false);
        }).thenSucceed();
    }
}

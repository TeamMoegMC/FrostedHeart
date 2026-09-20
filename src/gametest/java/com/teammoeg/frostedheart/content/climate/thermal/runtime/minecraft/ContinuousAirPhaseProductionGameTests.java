/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ContinuousAirProductionGameTests.room;
import static com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ContinuousAirProductionGameTests.useItem;

/** Long real-time phase scenarios; selected separately from the short production suite. */
@GameTestHolder("frostedheart_production_slow")
@PrefixGameTestTemplate(false)
public final class ContinuousAirPhaseProductionGameTests {
    private ContinuousAirPhaseProductionGameTests() {}

    @GameTest(template = "phase0a_empty", batch = "thermal_material_production", timeoutTicks = 24000)
    public static void realCampfiresDriveMaterialEnthalpyThroughWorldPhaseChange(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = room(helper, helper.absolutePos(BlockPos.ZERO));
        BlockPos[] fires = {center.north(2), center.south(2), center.east(2), center.west(2)};
        for (BlockPos fire : fires) {
            level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
            level.getBlockEntity(fire).getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(
                    () -> new IllegalStateException("Campfire fuel capability is missing")).insertItem(0, new ItemStack(Items.COAL, 4), false);
            useItem(helper, fire, Items.FLINT_AND_STEEL);
        }
        BlockPos material = center.above(2);
        level.setBlockAndUpdate(material.below(), Blocks.STONE.defaultBlockState());
        MaterialSample sample = new MaterialSample();
        double[] initialH = {Double.NaN};
        long[] started = {0};
        helper.runAfterDelay(10000, () -> {
            for (BlockPos fire : fires) {
                var entity = level.getBlockEntity(fire);
                if (entity != null) entity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(
                        fuel -> fuel.insertItem(0, new ItemStack(Items.COAL, 4), false));
            }
        });
        helper.startSequence().thenIdle(300).thenExecute(() -> {
            level.setBlockAndUpdate(material, Blocks.POWDER_SNOW.defaultBlockState());
            started[0] = level.getGameTime();
        }).thenWaitUntil(() -> {
            helper.assertTrue(MinecraftThermalInput.sampleMaterial(level, material, sample)
                    && sample.source() == MaterialSample.Source.LIVE,
                    "Placed powder snow enters the actual material publication");
            initialH[0] = sample.enthalpyJ();
        }).thenWaitUntil(() -> {
            helper.assertTrue(MinecraftThermalInput.sampleMaterial(level, material, sample)
                    && sample.source() == MaterialSample.Source.LIVE
                    && sample.enthalpyJ() > initialH[0] + 10,
                    "Actual heat exchange must increase stored material H before conversion");
        }).thenWaitUntil(() -> {
            helper.assertTrue(level.getBlockState(material).isAir(), "Actual phase controller eventually melts powder snow into Air");
        }).thenExecute(() -> {
            for (BlockPos fire : fires) if (level.getBlockState(fire).getValue(CampfireBlock.LIT)) useItem(helper, fire, Items.IRON_SHOVEL);
            FHMain.LOGGER.info("THERMAL_PRODUCTION_PHASE,initialH={},conversionTicks={}", initialH[0], level.getGameTime() - started[0]);
        }).thenSucceed();
    }

}

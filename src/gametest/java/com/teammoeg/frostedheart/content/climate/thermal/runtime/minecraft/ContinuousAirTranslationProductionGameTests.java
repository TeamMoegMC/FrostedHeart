/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Identical real nine-fire rooms shifted relative to the four-block partition. */
@GameTestHolder("frostedheart_translation")
@PrefixGameTestTemplate(false)
public final class ContinuousAirTranslationProductionGameTests {
    private static final int END_TICK = 1000;
    private ContinuousAirTranslationProductionGameTests() {}

    @GameTest(template = "phase0a_empty", timeoutTicks = 1200)
    public static void translatedRoomsKeepCurrentHeatThroughIgnitionAndCooling(GameTestHelper helper) throws java.io.IOException {
        var level = helper.getLevel();
        level.setDayTime(6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        ContinuousAirPrecisionProductionGameTests.restoreReferenceClimate(helper, true);
        BlockPos[] centers = new BlockPos[4];
        var fires = new java.util.ArrayList<BlockPos>();
        var points = new java.util.ArrayList<double[]>();
        for (int group = 0; group < centers.length; group++) {
            BlockPos origin = helper.absolutePos(new BlockPos(5 + group * 64 + group, 0, 7));
            for (int cx = (origin.getX() - 16) >> 4; cx <= (origin.getX() + 48) >> 4; cx++)
                for (int cz = (origin.getZ() - 16) >> 4; cz <= (origin.getZ() + 48) >> 4; cz++)
                    level.setChunkForced(cx, cz, true);
            // A real world command equalizes biome-derived backgrounds; never
            // seed temperatures or material H. Each command stays within 32^3.
            int bx = (origin.getX() - 16) & ~3, bz = (origin.getZ() - 16) & ~3;
            int by = Math.max(level.getMinBuildHeight(), (origin.getY() - 16) & ~3);
            var commandSource = level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput();
            for (int sx = 0; sx < 2; sx++) for (int sz = 0; sz < 2; sz++) {
                int x0 = bx + sx * 32, z0 = bz + sz * 32;
                String command = "fillbiome " + x0 + " " + by + " " + z0 + " "
                        + (x0 + 31) + " " + (by + 31) + " " + (z0 + 31) + " minecraft:plains";
                helper.assertTrue(level.getServer().getCommands().performPrefixedCommand(commandSource, command) > 0,
                        "Actual biome command equalizes the translation-room environment");
            }
            BlockPos center = centers[group] = ContinuousAirProductionGameTests.room(helper, origin);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos fire = center.offset(dx, 0, dz);
                if (dx != 0 || dz != 0) {
                    level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
                    var inventory = level.getBlockEntity(fire).getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(
                            () -> new IllegalStateException("Actual translated campfire has no fuel inventory"));
                    helper.assertTrue(inventory.insertItem(0, new ItemStack(Items.COAL, 4), false).isEmpty(), "Actual translated source receives coal");
                }
                ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
                fires.add(fire);
                for (double height : new double[] {1.125, 1.5, 1.875})
                    points.add(new double[] {group, dx, height, dz});
            }
            for (int distance : new int[] {-4, -2, 2, 4}) for (double height : new double[] {1.125, 1.875, 3.5})
                points.add(new double[] {group, distance, height, 0});
            FHMain.LOGGER.info("THERMAL_TRANSLATION_START,group={},shift={},sourceXMod4={},sourceYMod4={},sourceZMod4={},center={}",
                    group, group, center.getX() & 3, center.getY() & 3, center.getZ() & 3, center);
        }
        ContinuousAirProductionTiming.observe(helper, "translated_nine_fire_rooms", centers, END_TICK);
        var sample = new QueryPublication.MutableSample();
        int[] readableWindows = {0};
        for (int delay = 20; delay <= END_TICK; delay += 20) {
            int elapsed = delay;
            helper.runAfterDelay(delay, () -> {
                var publication = MinecraftThermalInput.queryPublicationForObservation(level);
                boolean allCurrent = publication != null;
                for (int point = 0; point < points.size(); point++) {
                    double[] relative = points.get(point);
                    int group = (int) relative[0];
                    BlockPos center = centers[group];
                    double x = center.getX() + .5 + relative[1], y = center.getY() + relative[2], z = center.getZ() + .5 + relative[3];
                    BlockPos block = BlockPos.containing(x, y, z);
                    boolean available = publication != null && publication.tryReadAir(x, y, z, sample);
                    boolean current = MinecraftThermalInput.describeAirRead(level, block).startsWith("geometry_current");
                    boolean fresh = available && level.getGameTime() - sample.sampleTick() <= 40;
                    allCurrent &= current && fresh;
                    FHMain.LOGGER.info("THERMAL_TRANSLATION_POINT,elapsedTicks={},group={},point={},dx={},dy={},dz={},sampleTick={},naturalC={},airC={},available={},current={},lit={}",
                            elapsed, group, point % 39, relative[1], relative[2], relative[3], available ? sample.sampleTick() : -1,
                            WorldTemperature.naturalAir(level, block), available ? sample.temperatureC() : Double.NaN, available, current, elapsed <= 600);
                }
                if (allCurrent && elapsed >= 200) readableWindows[0]++;
                if (elapsed == 600) for (BlockPos fire : fires)
                    ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
                if (elapsed != END_TICK) return;
                helper.assertTrue(allCurrent && readableWindows[0] >= 30, "Translated physical rooms retain current Air through source changes");
                helper.assertTrue(MinecraftThermalInput.workLimitedCompletions(level) == 0, "Translation observation fits the unchanged budget");
                FHMain.LOGGER.info("THERMAL_TRANSLATION_RESULT,windows={},points={},workLimited={},rootPeakBytes={}",
                        readableWindows[0], points.size(), MinecraftThermalInput.workLimitedCompletions(level), MinecraftThermalInput.thermalBudgetPeakBytes());
                helper.succeed();
            });
        }
    }
}

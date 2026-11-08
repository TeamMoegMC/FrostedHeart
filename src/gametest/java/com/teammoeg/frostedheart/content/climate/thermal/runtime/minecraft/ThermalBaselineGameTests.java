/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Fixed real-world layouts and actions; logs measured fields rather than constructing thermal state. */
@GameTestHolder("frostedheart_thermal_baseline")
@PrefixGameTestTemplate(false)
public final class ThermalBaselineGameTests {
    private static final String[] SCENES = {"sealed", "door", "multiple_sources", "partition"};
    private static final String[] POINTS = {"near", "far", "high", "low", "subcell_low", "subcell_high"};
    private static final double[][] OFFSETS = {{.5, 1.5, .5}, {3.5, 1.5, .5}, {.5, 4.5, .5},
            {.5, 1.125, .5}, {.125, 1.5, .125}, {.875, 1.5, .875}};

    private ThermalBaselineGameTests() { }

    @GameTest(template = "phase0a_empty", timeoutTicks = 2700)
    public static void realRoomsRecordHeatingVentilationWallsAndCooling(GameTestHelper helper) throws java.io.IOException {
        var level = helper.getLevel();
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
        ContinuousAirPrecisionProductionGameTests.restoreReferenceClimate(helper, false);
        BlockPos[] origins = new BlockPos[SCENES.length], fires = new BlockPos[SCENES.length];
        var allFires = new java.util.ArrayList<BlockPos>();
        for (int scene = 0; scene < SCENES.length; scene++) {
            BlockPos origin = origins[scene] = new BlockPos(8192 + scene * 96, 192, 8192);
            ThermalConvergenceScenes.force(helper, origin, 16, true);
            BlockPos original = room(helper, origin);
            if (scene == 3) {
                level.setBlockAndUpdate(original, Blocks.AIR.defaultBlockState());
                for (int y = 1; y < 6; y++) for (int z = 1; z < 8; z++)
                    level.setBlockAndUpdate(origin.offset(4, y, z), Blocks.STONE.defaultBlockState());
                fires[scene] = origin.offset(2, 1, 4);
                level.setBlockAndUpdate(fires[scene], Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
                ThermalConvergenceScenes.fuel(helper, fires[scene], 4);
            } else fires[scene] = original;
            allFires.add(fires[scene]);
            if (scene == 2) for (BlockPos extra : new BlockPos[]{original.west(2), original.east(2), original.north(2)}) {
                level.setBlockAndUpdate(extra, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
                ThermalConvergenceScenes.fuel(helper, extra, 4); allFires.add(extra);
            }
        }
        WorldTemperature.naturalAir(level, fires[0]);
        String climateOutput = System.getenv("FH_THERMAL_BASELINE_CLIMATE_OUTPUT");
        if (climateOutput != null && !climateOutput.isBlank()) {
            var climate = new CompoundTag(); WorldClimate.get(level).save(climate, false);
            var data = new CompoundTag(); data.put("frostedheart:climate_data", climate);
            var root = new CompoundTag(); root.put("data", data);
            NbtIo.writeCompressed(root, new java.io.File(climateOutput));
        }
        ContinuousAirProductionTiming.observe(helper, "unified_temperature_curves", fires, 2600);
        long started = level.getGameTime();
        FHMain.LOGGER.info("THERMAL_BASELINE_START,seed={},startTick={},dayTime={},sources={},scenes=4,plannedTicks=2600",
                level.getSeed(), started, level.getDayTime(), allFires.size());
        var sample = new ThermalEnvironmentSample();
        var material = new MaterialSample();
        double[] peakRise = new double[SCENES.length], peakRadiation = new double[SCENES.length];
        int[] published = new int[SCENES.length], cooling = new int[SCENES.length];
        double[] nearC = new double[SCENES.length], maximumDoorDifference = {0}, maximumWallContrast = {0};
        for (int delay = 1; delay <= 2600; delay++) {
            int elapsed = delay;
            helper.runAfterDelay(delay, () -> {
                if (elapsed == 200 || elapsed == 1400) {
                    for (var fire : allFires) ContinuousAirProductionGameTests.useItem(helper, fire,
                            elapsed == 200 ? Items.FLINT_AND_STEEL : Items.IRON_SHOVEL);
                    FHMain.LOGGER.info("THERMAL_BASELINE_ACTION,elapsedTick={},worldTick={},action={}", elapsed,
                            level.getGameTime(), elapsed == 200 ? "ignite" : "extinguish");
                }
                if (elapsed == 800) {
                    for (int y = 1; y <= 3; y++)
                        level.setBlockAndUpdate(origins[1].offset(0, y, 4), Blocks.AIR.defaultBlockState());
                    FHMain.LOGGER.info("THERMAL_BASELINE_ACTION,elapsedTick={},worldTick={},action=open_door", elapsed, level.getGameTime());
                }
                if (elapsed % 20 != 0) return;
                for (int scene = 0; scene < SCENES.length; scene++) {
                    var fire = fires[scene];
                    for (int point = 0; point < POINTS.length; point++) {
                        double x = fire.getX() + OFFSETS[point][0], y = fire.getY() + OFFSETS[point][1],
                                z = fire.getZ() + OFFSETS[point][2];
                        BlockPos block = BlockPos.containing(x, y, z);
                        double natural = WorldTemperature.naturalAir(level, block);
                        ContinuousAirProductionGameTests.sampleAt(level, x, y, z, sample);
                        double air = sample.airAvailable() ? sample.airTemperatureC() :
                                MinecraftThermalInput.gameplayPassiveEnvironment(level, block, natural);
                        FHMain.LOGGER.info("THERMAL_BASELINE_AIR,scene={},point={},elapsedTick={},worldTick={},sampleTick={},x={},y={},z={},airC={},naturalC={},radiantWPerM2={},available={},minimum={},wind={}",
                                SCENES[scene], POINTS[point], elapsed, level.getGameTime(), sample.airSampleTick(), x, y, z,
                                air, natural, sample.radiantFluxWPerM2(), sample.airAvailable(), sample.airMinimumQuality(), WorldClimate.getWind(level));
                        helper.assertTrue(Double.isFinite(air), "Every public environment result is finite");
                        if (point == 0) nearC[scene] = air;
                        if (scene == 3 && point == 1 && elapsed >= 600 && elapsed <= 1400)
                            maximumWallContrast[0] = Math.max(maximumWallContrast[0], nearC[scene] - air);
                        if (point == 0 && elapsed >= 600 && elapsed <= 1400) {
                            helper.assertTrue(sample.airAvailable() && sample.airSampleTick() >= 0
                                            && level.getGameTime() - sample.airSampleTick() <= 40,
                                    "The source-side baseline field remains current: " + SCENES[scene]);
                            published[scene]++;
                            peakRise[scene] = Math.max(peakRise[scene], air - natural);
                            peakRadiation[scene] = Math.max(peakRadiation[scene], sample.radiantFluxWPerM2());
                        }
                        if (point == 0 && elapsed > 1400 && sample.airSampleTick() > started + 1400) cooling[scene]++;
                    }
                    BlockPos[] materials = {fire.below(), origins[scene].offset(4, 0, 3), origins[scene].offset(0, 2, 4)};
                    for (int point = 0; point < materials.length; point++) {
                        var block = materials[point]; material.clear();
                        boolean available = MinecraftThermalInput.sampleMaterial(level, block, material);
                        FHMain.LOGGER.info("THERMAL_BASELINE_MATERIAL,scene={},point={},elapsedTick={},worldTick={},sampleTick={},x={},y={},z={},available={},temperatureC={},enthalpyJ={},branch={},source={}",
                                SCENES[scene], point, elapsed, level.getGameTime(), material.sampleTick(), block.getX(), block.getY(), block.getZ(),
                                available, material.temperatureC(), material.enthalpyJ(), material.branch(), material.source());
                    }
                }
                if (elapsed > 840 && elapsed <= 1400)
                    maximumDoorDifference[0] = Math.max(maximumDoorDifference[0], Math.abs(nearC[0] - nearC[1]));
                if (elapsed != 2600) return;
                helper.assertTrue(MinecraftThermalInput.enabledPhysicalSourceCount(level) == 0, "All real sources remain extinguished");
                helper.assertTrue(maximumDoorDifference[0] > 1e-5, "The opened doorway must actually affect the measured thermal field");
                helper.assertTrue(maximumWallContrast[0] > .1, "The partition must preserve a measured near/far temperature difference");
                FHMain.LOGGER.info("THERMAL_BASELINE_GEOMETRY_RESULT,maxDoorDifferenceC={},maxWallContrastC={}",
                        maximumDoorDifference[0], maximumWallContrast[0]);
                for (int scene = 0; scene < SCENES.length; scene++) {
                    helper.assertTrue(published[scene] > 0 && peakRise[scene] > .05,
                            "Actual source power warms each baseline room: " + SCENES[scene] + ",rise=" + peakRise[scene]);
                    helper.assertTrue(peakRadiation[scene] > 0, "The real source also reaches the player radiation path");
                    FHMain.LOGGER.info("THERMAL_BASELINE_SCENE_RESULT,scene={},peakRiseC={},peakRadiantWPerM2={},publishedSamples={},postExtinguishSamples={}",
                            SCENES[scene], peakRise[scene], peakRadiation[scene], published[scene], cooling[scene]);
                    ThermalConvergenceScenes.force(helper, origins[scene], 16, false);
                }
                helper.succeed();
            });
        }
    }

    /** Small enough that real walls and the doorway participate in this observation window. */
    static BlockPos room(GameTestHelper helper, BlockPos origin) {
        var level = helper.getLevel();
        for (int y = 6; y >= 0; y--) for (int z = 0; z <= 8; z++) for (int x = 0; x <= 8; x++) {
            boolean wall = y == 0 || y == 6 || x == 0 || x == 8 || z == 0 || z == 8;
            level.setBlockAndUpdate(origin.offset(x, y, z), (wall ? Blocks.STONE : Blocks.AIR).defaultBlockState());
        }
        var fire = origin.offset(4, 1, 4);
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        ThermalConvergenceScenes.fuel(helper, fire, 4);
        return fire;
    }
}

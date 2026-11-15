/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.climate;

import com.teammoeg.frostedheart.content.climate.gamedata.climate.CoveredClimateHistory;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Natural temperature: one continuous long-term altitude gradient, climate isolation only from real cover. */
@GameTestHolder("frostedheart_climate_cover")
@PrefixGameTestTemplate(false)
public final class ClimateCoverGameTests {
    private static final String TEMPLATE = "phase0a_empty";
    private static final CoveredClimateHistory.LocalClimate GLOBAL = (hour, celsius) -> celsius;

    private ClimateCoverGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = "climate_cover", timeoutTicks = 100)
    public static void naturalTemperatureComposesTheClimateReachingEachPlace(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(!WorldTemperature.hasNoUnderground(level), "the test overworld is normal terrain");
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        float minimum = FHConfig.SERVER.CLIMATE.absoluteZeroCelsius.get().floatValue();
        for (int y : new int[]{-64, -55, -1, 0, 1, 63, 64, 65, 240, 241, 319}) {
            BlockPos pos = new BlockPos(origin.getX(), y, origin.getZ());
            float dimension = WorldTemperature.dimension(level);
            float biome = WorldTemperature.biome(level, pos);
            float altitude = WorldTemperature.altitude(level, pos);
            for (float climate : new float[]{-10000, -40, 0, 40}) {
                float expected = Math.max(minimum, BlockTemperatureModel.naturalTemperature(dimension, biome, altitude,
                        WorldTemperature.coveredClimate(level, pos, climate), WorldTemperature.blockClimateFactor()));
                helper.assertTrue(WorldTemperature.naturalBlock(level, pos, climate) == expected,
                        "natural block composes the covered climate at y=" + y + ", climate=" + climate);
            }
            helper.assertTrue(WorldTemperature.naturalAir(level, pos)
                            == Math.max(minimum, dimension + biome + altitude + WorldTemperature.coveredClimate(level, pos)),
                    "natural air takes all of the climate reaching it at y=" + y);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "climate_cover", timeoutTicks = 100)
    public static void altitudeIsOneContinuousLongTermGradient(GameTestHelper helper) {
        for (int y = -64; y < 320; y++) {
            float step = Math.abs(WorldTemperature.altitude(y + 1, false) - WorldTemperature.altitude(y, false));
            helper.assertTrue(step <= WorldTemperature.TEMPERATURE_CHANGE_PER_BLOCK_BELOW_REFERENCE + 1e-6F,
                    "altitude moves by at most one gradient step from y=" + y + ", got " + step);
        }
        helper.assertTrue(WorldTemperature.altitude(WorldTemperature.ALTITUDE_REFERENCE_LEVEL, false) == 0,
                "the reference level has no altitude term");
        near(helper, WorldTemperature.altitude(320, false), -1.536, "0.6 °C colder per 100 blocks up to y=320");
        near(helper, WorldTemperature.altitude(-64, false), 3.84, "3 °C warmer per 100 blocks down to y=-64");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "climate_cover", timeoutTicks = 100)
    public static void superflatHasNoGeothermalHeat(GameTestHelper helper) {
        // The test server has no superflat dimension, so detection is checked on a real superflat generator and the
        // rule through the same functions the level queries use.
        ServerLevel level = helper.getLevel();
        var registries = level.registryAccess();
        var superflat = new net.minecraft.world.level.levelgen.FlatLevelSource(
                net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings.getDefault(
                        registries.lookupOrThrow(net.minecraft.core.registries.Registries.BIOME),
                        registries.lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET),
                        registries.lookupOrThrow(net.minecraft.core.registries.Registries.PLACED_FEATURE)));
        helper.assertTrue(WorldTemperature.hasNoUnderground(superflat)
                        && !WorldTemperature.hasNoUnderground(level.getChunkSource().getGenerator()),
                "a superflat generator has no underground; the normal test overworld does");
        for (int y : new int[]{-64, -60, -58, -55, -1, 0, 1, 63, 64})
            helper.assertTrue(WorldTemperature.altitude(y, true) == 0,
                    "superflat ground has no geothermal heat at y=" + y);
        for (int y : new int[]{65, 160, 240, 241, 319})
            helper.assertTrue(WorldTemperature.altitude(y, true) == WorldTemperature.altitude(y, false),
                    "superflat matches normal terrain above the reference level at y=" + y);
        BlockPos deep = helper.absolutePos(new BlockPos(2, 2, 2)).atY(-60);
        float expected = (WorldTemperature.ALTITUDE_REFERENCE_LEVEL + 60)
                * WorldTemperature.TEMPERATURE_CHANGE_PER_BLOCK_BELOW_REFERENCE;
        helper.assertTrue(WorldTemperature.altitude(-60, false) == expected
                        && WorldTemperature.altitude(level, deep) == expected,
                "normal terrain keeps its gentle geothermal gradient at y=-60");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "climate_cover", timeoutTicks = 100)
    public static void coverCountsOpaqueFullBlocksAboveAndFollowsEdits(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos low = helper.absolutePos(new BlockPos(5, 1, 5));
        LevelChunk chunk = level.getChunkAt(low);
        ClimateCoverColumns columns = ((ClimateCoverChunk) chunk).frostedheart$getClimateCover();
        int limit = 64, x = low.getX(), z = low.getZ();
        int base = columns.coverAbove(chunk, x, low.getY(), z, limit);
        // Stone counts; glass, leaves and ice let the climate through.
        helper.setBlock(new BlockPos(5, 3, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 4, 5), Blocks.GLASS);
        helper.setBlock(new BlockPos(5, 5, 5), Blocks.OAK_LEAVES);
        helper.setBlock(new BlockPos(5, 6, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 7, 5), Blocks.ICE);
        int built = columns.coverAbove(chunk, x, low.getY(), z, limit);
        helper.assertTrue(built == Math.min(limit, base + 2),
                "two stone blocks add two cover, got " + built + " over " + base);
        int between = columns.coverAbove(chunk, x, helper.absolutePos(new BlockPos(5, 4, 5)).getY(), z, limit);
        helper.assertTrue(between == Math.min(limit, base + 1), "only the stone above y=4 counts there, got " + between);
        helper.setBlock(new BlockPos(5, 6, 5), Blocks.AIR);
        helper.assertTrue(columns.coverAbove(chunk, x, low.getY(), z, limit) == Math.min(limit, base + 1),
                "removing a stone block drops it from the cached cover");
        helper.setBlock(new BlockPos(5, 2, 5), Blocks.DIRT);
        helper.assertTrue(columns.coverAbove(chunk, x, low.getY(), z, limit) == Math.min(limit, base + 2),
                "a new opaque block in the column is counted");
        helper.assertTrue(ClimateCover.coverAbove(level, low) == Math.min(ClimateCover.limit(), base + 2),
                "the public query is the same count capped by the configured limit");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "climate_cover", timeoutTicks = 100)
    public static void coveredClimateDampsShortChangesButPassesLastingOnes(GameTestHelper helper) {
        int limit = ClimateCover.limit();
        CoveredClimateHistory history = new CoveredClimateHistory();
        history.recordThrough(1000, limit, hour -> -10);
        history.recordThrough(2000, limit, hour -> -10);
        // A six-hour cold pulse, 30 °C below a long calm.
        history.recordThrough(2006, limit, hour -> hour > 2000 ? -40 : -10);
        near(helper, history.covered(0, limit, 2006, Long.MIN_VALUE, 0, GLOBAL), -40, "open air feels the pulse at once");
        float tenBlocks = history.covered(10, limit, 2006, Long.MIN_VALUE, 0, GLOBAL);
        near(helper, tenBlocks, -10 - 30 * (1 - Math.exp(-6.0 / CoveredClimateHistory.timeConstantHours(10))), 0.3,
                "ten blocks of cover pass only about 1 - e^(-6/τ) of a six-hour pulse");
        float previous = -40;
        for (int cover = 1; cover <= limit; cover++) {
            float value = history.covered(cover, limit, 2006, Long.MIN_VALUE, 0, GLOBAL);
            helper.assertTrue(value >= previous - 1e-4F, "deeper cover keeps more of the pulse out at cover " + cover);
            previous = value;
        }
        // Lasting cold reaches every depth unchanged.
        long later = 2006 + 4000;
        history.recordThrough(later, limit, hour -> -40);
        for (int cover = 0; cover <= limit; cover++)
            near(helper, history.covered(cover, limit, later, Long.MIN_VALUE, 0, GLOBAL), -40,
                    "lasting climate passes through cover " + cover);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "climate_cover", timeoutTicks = 100)
    public static void coveredClimateSurvivesSaveAndLoad(GameTestHelper helper) {
        int limit = ClimateCover.limit();
        CoveredClimateHistory history = new CoveredClimateHistory();
        history.recordThrough(500, limit, hour -> -5);
        history.recordThrough(700, limit, hour -> hour > 650 ? -35 : -5);
        CoveredClimateHistory loaded = new CoveredClimateHistory();
        loaded.load(history.save());
        for (int cover = 0; cover <= limit; cover++)
            helper.assertTrue(loaded.covered(cover, limit, 700, Long.MIN_VALUE, 0, GLOBAL)
                            == history.covered(cover, limit, 700, Long.MIN_VALUE, 0, GLOBAL),
                    "a reloaded history filters identically at cover " + cover);
        helper.assertTrue(Float.isNaN(new CoveredClimateHistory().covered(3, limit, 700, Long.MIN_VALUE, 0, GLOBAL)),
                "an empty history defers to the open-air climate");
        helper.succeed();
    }

    private static void near(GameTestHelper helper, double actual, double expected, String message) {
        near(helper, actual, expected, 1e-3, message);
    }

    private static void near(GameTestHelper helper, double actual, double expected, double tolerance, String message) {
        helper.assertTrue(Math.abs(actual - expected) <= tolerance, message + ": expected " + expected + ", got " + actual);
    }
}

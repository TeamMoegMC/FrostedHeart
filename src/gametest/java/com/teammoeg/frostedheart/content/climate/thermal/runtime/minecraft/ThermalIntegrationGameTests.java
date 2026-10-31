/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.data.StateTransitionData;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.minecraft.BlockRadiationIndex;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalFieldKey;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Short checks for the actual vanilla branches changed by integration cleanup. */
@GameTestHolder("frostedheart_thermal_integration")
@PrefixGameTestTemplate(false)
public final class ThermalIntegrationGameTests {
    private static final ThermalFieldKey FIELD = new ThermalFieldKey(
            new ResourceLocation("frostedheart", "integration_test"), 0, 0, 0);

    @GameTest(template = "phase0a_empty", batch = "integration_owned_random", timeoutTicks = 160)
    public static void climateSamplesPhaseOnlySectionsWithoutChangingVanillaQueries(GameTestHelper helper) {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        var chunk = level.getChunkAt(helper.absolutePos(new BlockPos(1024, 2, 8)));
        int sectionIndex = chunk.getSections().length - 3;
        var previous = chunk.getSections()[sectionIndex];
        var section = new net.minecraft.world.level.chunk.LevelChunkSection(level.registryAccess().registryOrThrow(Registries.BIOME));
        for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++)
            section.setBlockState(x, y, z, Blocks.PACKED_ICE.defaultBlockState());
        BlockPos center = new BlockPos(chunk.getPos().getMinBlockX() + 8,
                chunk.getSectionYFromSectionIndex(sectionIndex) * 16 + 8, chunk.getPos().getMinBlockZ() + 8);
        try {
            helper.assertTrue(!section.isRandomlyTicking() && !Blocks.PACKED_ICE.defaultBlockState().isRandomlyTicking(),
                    "fixture must have custom eligibility only, with vanilla counts unchanged");
            StateTransitionData.updateCache(level.getRecipeManager());
            var phase = StateTransitionData.getData(Blocks.PACKED_ICE.defaultBlockState());
            helper.assertTrue(phase != null && phase.hasRandomTransitions(), "actual packed-ice data must be loaded");
            chunk.getSections()[sectionIndex] = section;
            MinecraftThermalInput.onSectionIdentityReplaced(level, chunk, sectionIndex, previous);
            MinecraftGameplayFields.upsertSphere(level, FIELD, 10_000, ThermalAnalyticField.CombineMode.OVERRIDE,
                    center.getX(), center.getY(), center.getZ(), 32, 42);
            level.tickChunk(chunk, 0);
            int[] packed = {0};
            section.getStates().count((state, count) -> { if (state.is(Blocks.PACKED_ICE)) packed[0] += count; });
            helper.assertTrue(packed[0] == 4096, "zero random speed must not process the phase-only Section");
            level.tickChunk(chunk, 1);
            packed[0] = 0;
            section.getStates().count((state, count) -> { if (state.is(Blocks.PACKED_ICE)) packed[0] += count; });
            helper.assertTrue(packed[0] < 4096, "the actual climate loop must sample and transition a non-vanilla-random Section after reload");
            helper.assertTrue(!Blocks.PACKED_ICE.defaultBlockState().isRandomlyTicking(), "climate handling must preserve the vanilla query result");
        } finally {
            chunk.getSections()[sectionIndex] = previous;
            MinecraftThermalInput.onSectionIdentityReplaced(level, chunk, sectionIndex, section);
            MinecraftGameplayFields.remove(level, FIELD);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "integration_radiation_dependencies", timeoutTicks = 160)
    public static void radiationCacheTracksCrossSectionShapeDependencies(GameTestHelper helper) {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos fire = helper.absolutePos(new BlockPos(1152, 2, 8));
        level.setBlockAndUpdate(fire.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        ((ICampfireExtra) level.getBlockEntity(fire)).setLifeTime(20_000);
        var input = ThermalLoadedWorldGameTests.owner(level, fire).input();
        BlockRadiationIndex cache = ThermalLoadedWorldGameTests.read(input, "blockRadiation");
        helper.assertTrue(cache != null, "fixture needs the configured real static-radiation index");
        BlockPos lava = new BlockPos((fire.getX() & ~15) + 15, level.getMaxBuildHeight() - 33, (fire.getZ() & ~15) + 8);
        BlockPos neighbor = lava.east(), upper = neighbor.above();
        level.getChunkAt(neighbor);
        var full = Blocks.LAVA.defaultBlockState();
        var low = full.setValue(LiquidBlock.LEVEL, 7);
        try {
            level.setBlock(lava.above(), Blocks.AIR.defaultBlockState(), 18);
            level.setBlock(lava.below(), Blocks.AIR.defaultBlockState(), 18);
            level.setBlock(lava, full, 18);
            level.setBlock(neighbor, low, 18);
            level.setBlock(upper, full, 18);
            long[] key = {Long.MIN_VALUE};
            radiationPower(cache, lava, key);
            cache.tick(0);
            double covered = radiationPower(cache, lava, key);
            helper.assertTrue(covered > 0, "the actual lava Brick must be captured before dependency changes");
            level.setBlock(upper, Blocks.AIR.defaultBlockState(), 18);
            cache.tick(20);
            helper.assertTrue(radiationPower(cache, lava, key) > covered, "a horizontal neighbor's upper block in a diagonal Section must invalidate the emitting Brick");
            level.setBlock(upper, full, 18);
            cache.tick(40);
            helper.assertTrue(Math.abs(radiationPower(cache, lava, key) - covered) <= covered * 1e-6,
                    "restoring the same geometry must restore the same aggregate");
            level.setBlock(neighbor, Blocks.STONE.defaultBlockState(), 18);
            cache.tick(60);
            double wall = radiationPower(cache, lava, key);
            level.setBlock(neighbor, Blocks.AIR.defaultBlockState(), 18);
            cache.tick(80);
            double open = radiationPower(cache, lava, key);
            helper.assertTrue(open > wall, "a non-radiating wall opening must invalidate neighbor emission");
            level.setBlock(lava, full.setValue(LiquidBlock.LEVEL, 4), 18);
            cache.tick(100);
            helper.assertTrue(radiationPower(cache, lava, key) < open, "height changes with the same radiation profile must refresh exposed area");
        } finally {
            for (BlockPos position : new BlockPos[]{lava, neighbor, upper, fire})
                level.setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
            MinecraftThermalInput.closeActiveLevel(level);
        }
        helper.succeed();
    }

    private static double radiationPower(BlockRadiationIndex cache, BlockPos lava, long[] key) {
        double[] power = {0};
        cache.visitNearby(lava.getX() + .5, lava.getY() + .5, lava.getZ() + .5, 128,
                (id, revision, x, y, z, watts, bound) -> {
                    if (key[0] == Long.MIN_VALUE && Math.floor(x / 4) == (lava.getX() >> 2)
                            && Math.floor(y / 4) == (lava.getY() >> 2) && Math.floor(z / 4) == (lava.getZ() >> 2)) key[0] = id;
                    if (id == key[0]) power[0] += watts;
                    return true;
                });
        return power[0];
    }

    @GameTest(template = "phase0a_empty", batch = "integration_weather_snow", timeoutTicks = 120)
    public static void actualChunkTicksKeepExtraSnowAndGroundRules(GameTestHelper helper) throws ReflectiveOperationException {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        var chunk = level.getChunkAt(helper.absolutePos(new BlockPos(128, 2, 8)));
        var oldBiomes = java.util.Arrays.stream(chunk.getSections()).map(section -> section.getBiomes()).toList();
        var weather = (ServerLevelData) level.getLevelData();
        boolean raining = weather.isRaining(), thundering = weather.isThundering();
        int clearTime = weather.getClearWeatherTime(), rainTime = weather.getRainTime(), thunderTime = weather.getThunderTime();
        boolean enabled = FHConfig.SERVER.WORLDGEN.enableSnowAccumulationDuringWeather.get();
        int difficulty = FHConfig.SERVER.WORLDGEN.snowAccumulationDifficulty.get();
        var randomPosition = Level.class.getDeclaredField("randValue");
        randomPosition.setAccessible(true);
        int oldRandomPosition = randomPosition.getInt(level);
        float rainLevel = level.getRainLevel(1);
        var thunderLevelField = Level.class.getDeclaredField("thunderLevel");
        thunderLevelField.setAccessible(true);
        float thunderLevel = thunderLevelField.getFloat(level);
        BlockPos position = null;
        try {
            var snowyBiome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.SNOWY_PLAINS);
            chunk.fillBiomesFromNoise((x, y, z, sampler) -> snowyBiome, level.getChunkSource().randomState().sampler());
            randomPosition.setInt(level, -1024);
            var column = level.getBlockRandomPos(chunk.getPos().getMinBlockX(), 0, chunk.getPos().getMinBlockZ(), 15);
            position = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
            if (position.getY() <= level.getMinBuildHeight())
                position = position.atY(level.getMinBuildHeight() + 1);
            helper.assertTrue(level.getBiome(position).value().coldEnoughToSnow(position), "fixture must have a real snowy biome");
            for (int branch = 0; branch < 6; branch++) {
                int layers = branch >= 4 ? 5 : 3;
                double temperature = WorldTemperature.SNOW_REACHES_GROUND + (branch == 2 ? 10 : -10);
                FHConfig.SERVER.WORLDGEN.enableSnowAccumulationDuringWeather.set(branch != 0);
                FHConfig.SERVER.WORLDGEN.snowAccumulationDifficulty.set(1);
                level.setWeatherParameters(0, 1000, branch != 1, false);
                level.setRainLevel(branch != 1 ? 1 : 0);
                level.setThunderLevel(0);
                helper.assertTrue(level.isRaining() == (branch != 1), "fixture needs current rain intensity, not only future weather flags");
                MinecraftGameplayFields.upsertSphere(level, FIELD, 10_000, ThermalAnalyticField.CombineMode.OVERRIDE,
                        position.getX() + .5, position.getY() + .5, position.getZ() + .5, 8, temperature);
                level.setBlockAndUpdate(position.below(), Blocks.DIRT.defaultBlockState());
                level.setBlockAndUpdate(position, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers));
                if (branch == 5)
                    level.setBlock(position.below(), Blocks.GRASS_BLOCK.defaultBlockState()
                            .setValue(BlockStateProperties.SNOWY, false), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                helper.assertTrue(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, position).equals(position),
                        "the actual sampled column must point at the surface snow, expected=" + position
                                + ",actual=" + level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, position));
                helper.assertTrue(Math.abs(WorldTemperature.block(level, position) - temperature) < .001,
                        "fixture must expose the requested side of the real snow threshold");
                helper.assertTrue(level.getBrightness(LightLayer.BLOCK, position) < 10,
                        "fixture must satisfy snow's actual block-light bound");
                randomPosition.setInt(level, -1024);
                // Exercise the transformed vanilla entry; no direct call to the private snow helper.
                level.tickChunk(chunk, 0);
                helper.assertTrue(level.getBlockState(position).getValue(SnowLayerBlock.LAYERS) == (branch == 3 ? 4 : layers),
                        "actual chunk ticks must respect enabled/rain/temperature/layer bounds, branch=" + branch);
                if (branch >= 3) {
                    var ground = level.getBlockState(position.below());
                    helper.assertTrue(ground.is(branch == 5 ? Blocks.GRASS_BLOCK : FHBlocks.DIRT_PERMAFROST.get())
                                    && ground.getValue(BlockStateProperties.SNOWY),
                            "weather snow must persist both replacement and existing ground's SNOWY state, branch=" + branch);
                }
            }
        } finally {
            FHConfig.SERVER.WORLDGEN.enableSnowAccumulationDuringWeather.set(enabled);
            FHConfig.SERVER.WORLDGEN.snowAccumulationDifficulty.set(difficulty);
            weather.setRaining(raining);
            weather.setThundering(thundering);
            weather.setClearWeatherTime(clearTime);
            weather.setRainTime(rainTime);
            weather.setThunderTime(thunderTime);
            level.setRainLevel(rainLevel);
            level.setThunderLevel(thunderLevel);
            randomPosition.setInt(level, oldRandomPosition);
            for (int i = 0; i < chunk.getSections().length; i++)
                ThermalLoadedWorldGameTests.write(chunk.getSections()[i], "biomes", oldBiomes.get(i));
            MinecraftGameplayFields.remove(level, FIELD);
            if (position != null) level.setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "integration_ice_break", timeoutTicks = 100)
    public static void iceKeepsVanillaDropsAndOnlyGatesWater(GameTestHelper helper) {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos position = helper.absolutePos(new BlockPos(4, 2, 4));
        var player = FakePlayerFactory.getMinecraft(level);
        ItemStack ordinary = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack silk = new ItemStack(Items.DIAMOND_PICKAXE);
        silk.enchant(Enchantments.SILK_TOUCH, 1);
        level.setBlockAndUpdate(position.below(), Blocks.STONE.defaultBlockState());
        try {
            for (int branch = 0; branch < 3; branch++) {
                double temperature = branch == 0 ? -10 : 42;
                MinecraftGameplayFields.upsertSphere(level, FIELD, 10_000, ThermalAnalyticField.CombineMode.OVERRIDE,
                        position.getX() + .5, position.getY() + .5, position.getZ() + .5, 4, temperature);
                level.setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
                helper.assertTrue(Math.abs(WorldTemperature.block(level, position) - temperature) < .001,
                        "the actual temperature query must expose the branch fixture");
                // playerDestroy is called after removal in the normal mining path.
                Blocks.ICE.playerDestroy(level, player, position, Blocks.ICE.defaultBlockState(), null,
                        branch == 2 ? silk : ordinary);
                helper.assertTrue(level.getBlockState(position).is(branch == 1 ? Blocks.WATER : Blocks.AIR),
                        "cold and Silk Touch keep Air; warm ordinary mining retains vanilla water");
            }
            helper.assertTrue(!level.getEntitiesOfClass(ItemEntity.class, new AABB(position).inflate(2),
                    item -> item.getItem().is(Items.ICE)).isEmpty(), "vanilla Silk Touch drops must still execute");
        } finally {
            MinecraftGameplayFields.remove(level, FIELD);
            level.setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "integration_maintenance_save", timeoutTicks = 300)
    public static void maintenanceSaveSkipsCleanAndRefusesUnpreparedWrites(GameTestHelper helper) {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos fire = helper.absolutePos(new BlockPos(128, 2, 8));
        level.setChunkForced(fire.getX() >> 4, fire.getZ() >> 4, true);
        level.setBlockAndUpdate(fire.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(fire.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        ((ICampfireExtra) level.getBlockEntity(fire)).setLifeTime(20_000);
        var input = ThermalLoadedWorldGameTests.owner(level, fire).input();
        helper.runAfterDelay(40, () -> {
            var chunk = level.getChunkAt(fire);
            var map = level.getChunkSource().chunkMap;
            try {
                ChunkHolder holder = null;
                for (var candidate : map.getChunks()) if (candidate.getPos().equals(chunk.getPos())) holder = candidate;
                helper.assertTrue(holder != null && holder.wasAccessibleSinceLastSave(), "real forced holder must be save eligible");
                var save = ChunkMap.class.getDeclaredMethod("saveChunkIfNeeded", ChunkHolder.class);
                save.setAccessible(true);
                Long2LongMap cooldowns = ThermalLoadedWorldGameTests.read(map, "chunkSaveCooldowns");
                cooldowns.remove(chunk.getPos().toLong());
                long[] before = MinecraftThermalInput.observedCheckpointCalls(level);
                long writes = MinecraftThermalInput.observedMaintenanceSaves(level)[1];
                chunk.setUnsaved(false);
                helper.assertTrue(!(boolean) save.invoke(map, holder), "clean chunk must not be saved");
                helper.assertTrue(MinecraftThermalInput.observedCheckpointCalls(level)[0] == before[0],
                        "clean save attempt must not create a thermal checkpoint");
                chunk.setUnsaved(true);
                ThermalLoadedWorldGameTests.write(input, "checkpointInProgress", true);
                helper.assertTrue(!(boolean) save.invoke(map, holder) && chunk.isUnsaved(),
                        "busy preparation must defer and leave the real dirty chunk intact");
                helper.assertTrue(MinecraftThermalInput.observedMaintenanceSaves(level)[1] == writes,
                        "rejected preparation must not report a write");
                ThermalLoadedWorldGameTests.write(input, "checkpointInProgress", false);
                helper.assertTrue((boolean) save.invoke(map, holder), "the same dirty holder must save after preparation is available");
                helper.assertTrue(MinecraftThermalInput.observedMaintenanceSaves(level)[1] == writes + 1,
                        "one real successful write must be observed");
                helper.assertTrue(ThermalLoadedWorldGameTests.read(input, "preparedMaintenanceSave") == null,
                        "prepared save scope must end after the actual vanilla write");
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError(failure);
            } finally {
                ThermalLoadedWorldGameTests.write(input, "checkpointInProgress", false);
                level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
                MinecraftThermalInput.closeActiveLevel(level);
                level.setChunkForced(fire.getX() >> 4, fire.getZ() >> 4, false);
            }
            helper.succeed();
        });
    }

    @GameTest(template = "phase0a_empty", batch = "integration_snow_and_eligibility", timeoutTicks = 100)
    public static void directSnowTicksKeepPhaseAndLayerRules(GameTestHelper helper) {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos position = helper.absolutePos(new BlockPos(4, 2, 4));
        level.setBlockAndUpdate(position.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(position.east(), Blocks.GLOWSTONE.defaultBlockState());
        helper.runAfterDelay(10, () -> {
            try {
                helper.assertTrue(Blocks.WHEAT.defaultBlockState().isRandomlyTicking(), "vanilla random eligibility must remain true");
                helper.assertTrue(!Blocks.WATER.defaultBlockState().isRandomlyTicking(), "water must retain surface sampling");
                helper.assertTrue(level.getBrightness(LightLayer.BLOCK, position) > 11, "real block light must reach the snow");
                MinecraftGameplayFields.upsertSphere(level, FIELD, 10_000, ThermalAnalyticField.CombineMode.OVERRIDE,
                        position.getX() + .5, position.getY() + .5, position.getZ() + .5, 4, 1);
                var snow = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 3);
                level.setBlockAndUpdate(position, snow);
                snow.randomTick(level, position, level.random);
                helper.assertTrue(level.getBlockState(position).getValue(SnowLayerBlock.LAYERS) == 2,
                        "ordinary direct snow ticks must lose one layer rather than the whole block");
                MinecraftGameplayFields.upsertSphere(level, FIELD, 10_000, ThermalAnalyticField.CombineMode.OVERRIDE,
                        position.getX() + .5, position.getY() + .5, position.getZ() + .5, 4, 42);
                level.setBlockAndUpdate(position, snow);
                snow.randomTick(level, position, level.random);
                helper.assertTrue(level.getBlockState(position).isAir(), "direct warm snow ticks must still apply material phase rules");
                helper.assertTrue(!level.getBiome(position).value().shouldFreeze(level, position, false),
                        "vanilla freezing must remain globally disabled");
            } finally {
                MinecraftGameplayFields.remove(level, FIELD);
                level.setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(position.east(), Blocks.AIR.defaultBlockState());
            }
            helper.succeed();
        });
    }
}

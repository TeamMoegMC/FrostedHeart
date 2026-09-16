/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.data.StateTransitionData;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPhaseController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.Map;

@GameTestHolder(FHMain.MODID)
@PrefixGameTestTemplate(false)
public final class ThermalTransitionDataGameTests {
    @GameTest(template = "phase0a_empty", batch = "thermal_transition_data", timeoutTicks = 100)
    public static void allShippedEdgesShareTargetEnergyReferences(GameTestHelper helper) {
        int checked = 0;
        var profiles = MinecraftThermalProfiles.prepare();
        int dirtProfile = profiles.signatures().materialProfileId(profiles.states().signatureId(Blocks.DIRT.defaultBlockState()));
        int stoneProfile = profiles.signatures().materialProfileId(profiles.states().signatureId(Blocks.STONE.defaultBlockState()));
        near(helper, 1.0, profiles.materials().profileOrNull(dirtProfile).faceConductanceWPerK(),
                "dirt retains earth conductance despite its phase transitions");
        near(helper, 1.4, profiles.materials().profileOrNull(stoneProfile).faceConductanceWPerK(),
                "stone retains masonry conductance");
        for (BlockState state : Block.BLOCK_STATE_REGISTRY) {
            var data = StateTransitionData.getData(state);
            if (data == null) continue;
            var law = MinecraftThermalProfiles.materialLaw(state);
            helper.assertTrue(law != null, "declared material must have a body: " + state);
            int id = profiles.signatures().materialProfileId(profiles.states().signatureId(state));
            for (boolean hot : new boolean[]{true, false}) {
                var definition = hot ? data.heating() : data.cooling();
                var edge = hot ? law.heating() : law.cooling();
                helper.assertTrue((definition == null) == (edge == null), "only explicit directions compile: " + state);
                if (edge == null) continue;
                helper.assertTrue(edge.heating() == hot, "latent energy has the declared sign: " + state);
                helper.assertTrue(profiles.transitionData(id, hot).equals(definition), "profile retains submission metadata");
                helper.assertTrue(Block.BLOCK_STATE_REGISTRY.byId(edge.targetStateId()) == definition.target(), "compiled target identity");
                var target = MinecraftThermalProfiles.materialLaw(definition.target());
                double expected = target == null ? law.enthalpyAtTemperature(definition.temperatureC())
                        + (hot ? definition.latentHeatJ() : -definition.latentHeatJ())
                        : target.enthalpyAtTemperature(definition.temperatureC());
                near(helper, expected, edge.targetEnthalpyJ(), "target reference: " + state);
                if (target != null) near(helper, edge.transitionTemperatureC(), target.temperatureC(edge.targetEnthalpyJ(), (byte) 0),
                        "successful conversion has no temperature jump: " + state);
                checked++;
            }
        }
        helper.assertTrue(checked > 50, "test must inspect the loaded content, not an empty cache");
        FHMain.LOGGER.info("Verified {} declared material edges and their target energy references", checked);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "thermal_transition_custom_data", timeoutTicks = 100)
    public static void dataOnlyDirectionsAndConditionsNeedNoMaterialBranches(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        var cacheField = StateTransitionData.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var previous = (Map<BlockState, StateTransitionData>) cacheField.get(null);
        int speed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockState original = level.getBlockState(pos);
        try {
            var definitions = new HashMap<>(previous);
            var stone = decode("""
                    {"block":{"Name":"minecraft:stone"}, "capacity_j_per_k":100,
                     "heating":{"target":{"Name":"minecraft:glass"}, "temperature_c":40,
                                "world_conditions":{"min_y_exclusive":-1000}},
                     "cooling":{"target":{"Name":"minecraft:dirt"}, "temperature_c":-10}}
                    """);
            definitions.put(stone.block(), stone);
            definitions.put(Blocks.COBBLESTONE.defaultBlockState(), decode("""
                    {"block":{"Name":"minecraft:cobblestone"}, "capacity_j_per_k":100,
                     "heating":{"target":{"Name":"minecraft:glass"}, "temperature_c":40,
                                "world_conditions":{"min_y_exclusive":320}},
                     "cooling":{"target":{"Name":"minecraft:dirt"}, "temperature_c":-10}}
                    """));
            definitions.put(Blocks.GLASS.defaultBlockState(), decode("""
                    {"block":{"Name":"minecraft:glass"}, "capacity_j_per_k":100,"enthalpy_offset_j":1000,
                     "conductance_w_per_k":3.25}
                    """));
            definitions.put(Blocks.DIRT.defaultBlockState(), decode("""
                    {"block":{"Name":"minecraft:dirt"}, "capacity_j_per_k":100,"enthalpy_offset_j":-1000}
                    """));
            cacheField.set(null, Map.copyOf(definitions));
            MinecraftThermalProfiles.invalidate();
            var profiles = MinecraftThermalProfiles.prepare();
            int first = profiles.signatures().materialProfileId(profiles.states().signatureId(stone.block()));
            int second = profiles.signatures().materialProfileId(profiles.states().signatureId(Blocks.COBBLESTONE.defaultBlockState()));
            helper.assertTrue(first != second, "same physical law with different world constraints must not merge");
            near(helper, 1.4, profiles.materials().profileOrNull(first).faceConductanceWPerK(),
                    "adding phase transitions does not override stone conductance");
            int glassProfile = profiles.signatures().materialProfileId(profiles.states().signatureId(Blocks.GLASS.defaultBlockState()));
            near(helper, 3.25, profiles.materials().profileOrNull(glassProfile).faceConductanceWPerK(),
                    "explicit recipe conductance overrides the material default");
            var law = MinecraftThermalProfiles.materialLaw(stone.block());
            near(helper, 1000, law.heating().targetEnthalpyJ() - law.heating().sourceEnthalpyJ(), "explicit heating gap");
            near(helper, -1000, law.cooling().targetEnthalpyJ() - law.cooling().sourceEnthalpyJ(), "independent cooling gap");
            helper.assertTrue(MinecraftThermalProfiles.materialLaw(Blocks.GLASS.defaultBlockState()).cooling() == null,
                    "incoming heating does not invent a reciprocal cooling edge");
            helper.assertTrue(decode(StateTransitionData.CODEC.encodeStart(JsonOps.INSTANCE, stone).result().orElseThrow().toString()).equals(stone),
                    "new data schema round-trips without losing conditions");
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(3, level.getServer());
            level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
            helper.assertTrue(!MinecraftPhaseController.applyDormantTransition(level, pos, level.getBlockState(pos), law.heating(),
                    law.heating().targetEnthalpyJ(), level.getGameTime()), "data height condition blocks submission");
            level.setBlockAndUpdate(pos, stone.block());
            helper.assertTrue(MinecraftPhaseController.applyDormantTransition(level, pos, stone.block(), law.heating(),
                    law.heating().targetEnthalpyJ(), level.getGameTime()), "ordinary stone converts using data only");
            helper.assertTrue(level.getBlockState(pos).is(Blocks.GLASS), "declared target is applied");
        } finally {
            cacheField.set(null, previous);
            MinecraftThermalProfiles.invalidate();
            level.setBlockAndUpdate(pos, original);
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(speed, level.getServer());
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "thermal_transition_fluid_boundary", timeoutTicks = 100)
    public static void waterInteriorWaitsForAnEdge(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var water = Blocks.WATER.defaultBlockState();
        var edge = MinecraftThermalProfiles.materialLaw(water).cooling();
        int speed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        // This test varies fluid boundaries, not the generated biome. Deep-dark and frozen-ocean
        // placements legitimately forbid freezing, so isolate that independent data condition.
        var cacheField = StateTransitionData.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var previousDefinitions = (Map<BlockState, StateTransitionData>) cacheField.get(null);
        var waterData = StateTransitionData.getData(water);
        var cooling = waterData.cooling();
        var conditions = cooling.worldConditions();
        var boundaryOnly = new StateTransitionData.Edge(cooling.target(), cooling.temperatureC(),
                cooling.latentHeatJ(), new StateTransitionData.WorldConditions(
                        conditions.minYExclusive(), conditions.fluidBoundary(), null), cooling.effect());
        var definitions = new HashMap<>(previousDefinitions);
        definitions.put(water, new StateTransitionData(waterData.block(), waterData.allStates(),
                waterData.capacityJPerK(), waterData.offsetJ(), waterData.conductanceWPerK(),
                waterData.heating(), boundaryOnly));
        var originals = new HashMap<BlockPos, BlockState>();
        originals.put(pos, level.getBlockState(pos));
        for (Direction direction : Direction.Plane.HORIZONTAL) originals.put(pos.relative(direction), level.getBlockState(pos.relative(direction)));
        try {
            cacheField.set(null, Map.copyOf(definitions));
            MinecraftThermalProfiles.invalidate();
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(3, level.getServer());
            for (BlockPos block : originals.keySet()) level.setBlock(block, water, 2);
            helper.assertTrue(!MinecraftPhaseController.applyDormantTransition(level, pos, water, edge, edge.targetEnthalpyJ(), level.getGameTime()),
                    "interior water retains its completed energy while boundary is blocked");
            level.setBlock(pos.west(), Blocks.STONE.defaultBlockState(), 2);
            helper.assertTrue(MinecraftPhaseController.applyDormantTransition(level, pos, water, edge, edge.targetEnthalpyJ(), level.getGameTime()),
                    "opening a horizontal boundary allows the same completed transition; loaded="
                            + level.isAreaLoaded(pos, 1) + ", biome=" + level.getBiome(pos).unwrapKey()
                            + ", floor=" + MinecraftGameplayFields.guaranteedFloor(level, pos)
                            + ", state=" + level.getBlockState(pos));
        } finally {
            originals.forEach((block, state) -> level.setBlock(block, state, 2));
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(speed, level.getServer());
            cacheField.set(null, previousDefinitions);
            MinecraftThermalProfiles.invalidate();
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "thermal_transition_lava_height", timeoutTicks = 100)
    public static void lavaHeightRemainsADataSubmissionCondition(GameTestHelper helper) {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        var lava = Blocks.LAVA.defaultBlockState();
        helper.assertTrue(StateTransitionData.getData(lava).cooling().worldConditions().minYExclusive() == -55,
                "source lava retains the declared -55 boundary");
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 2, 2));
        var edge = MinecraftThermalProfiles.materialLaw(lava).cooling();
        int speed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        try {
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(3, level.getServer());
            for (int y : new int[]{-55, -54}) {
                BlockPos pos = new BlockPos(anchor.getX(), y, anchor.getZ());
                BlockState original = level.getBlockState(pos);
                try {
                    level.setBlock(pos, lava, 2);
                    boolean applied = MinecraftPhaseController.applyDormantTransition(level, pos, lava, edge,
                            edge.targetEnthalpyJ(), level.getGameTime());
                    helper.assertTrue(applied == (y > -55), "lava height condition at Y=" + y);
                } finally {
                    level.setBlock(pos, original, 2);
                }
            }
        } finally {
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(speed, level.getServer());
        }
        helper.succeed();
    }

    private static StateTransitionData decode(String json) {
        return StateTransitionData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).result().orElseThrow();
    }

    @GameTest(template = "phase0a_empty", batch = "thermal_water_tick_eligibility", timeoutTicks = 100)
    public static void waterKeepsItsLawWithoutAddingBlockRandomTicks(GameTestHelper helper) {
        var section = new net.minecraft.world.level.chunk.LevelChunkSection(helper.getLevel().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.BIOME));
        for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            var water = Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, y);
            helper.assertTrue(!water.isRandomlyTicking(), "water amount " + y + " must not add random eligibility");
            section.setBlockState(x, y, z, water);
        }
        // Vanilla mutation counts nonempty fluids, even water; preserve that original behavior.
        helper.assertTrue(!section.isRandomlyTickingBlocks(), "water must add no block random-tick count");
        section.recalcBlockCounts();
        helper.assertTrue(!section.isRandomlyTicking(), "recount must keep water sections skipped");
        section.setBlockState(0, 0, 0, Blocks.ICE.defaultBlockState());
        helper.assertTrue(section.isRandomlyTicking(), "other phase materials retain random eligibility");
        section.setBlockState(0, 0, 0, Blocks.WATER.defaultBlockState());
        helper.assertTrue(!section.isRandomlyTicking(), "removing the last eligible body disables section sampling");
        helper.assertTrue(MinecraftThermalProfiles.materialLaw(Blocks.WATER.defaultBlockState()).cooling() != null,
                "sampling policy must not remove water's physical freezing law");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "thermal_water_surface_tick", timeoutTicks = 100)
    public static void realChunkTicksFreezeOnlyTheScheduledSurfaceSample(GameTestHelper helper) {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        var chunk = level.getChunkAt(helper.absolutePos(new BlockPos(2, 2, 2)));
        var corner = new BlockPos(chunk.getPos().getMinBlockX(), level.getMaxBuildHeight() - 17, chunk.getPos().getMinBlockZ());
        var originals = new HashMap<BlockPos, BlockState>();
        var surfaceWater = new java.util.ArrayList<BlockPos>();
        var coveredWater = new java.util.ArrayList<BlockPos>();
        var key = new com.teammoeg.frostedheart.content.climate.thermal.field.ThermalFieldKey(
                new net.minecraft.resources.ResourceLocation(FHMain.MODID, "surface_tick_test"), 0, corner.asLong(), 0);
        long originalTime = level.getGameTime();
        var levelData = (net.minecraft.world.level.storage.ServerLevelData) level.getLevelData();
        int speed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        int interval = com.teammoeg.frostedheart.infrastructure.config.FHConfig.SERVER.CLIMATE.tempBlockstateUpdateIntervalTicks.get();
        try {
            for (int y = -2; y <= 1; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                BlockPos pos = corner.offset(x, y, z);
                originals.put(pos, level.getBlockState(pos));
                boolean water = (y == 0 || y == -2) && x > 0 && x < 15 && z > 0 && z < 15 && (x & 1) == 0;
                level.setBlock(pos, water ? Blocks.WATER.defaultBlockState() : y == 1
                        ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState(), 2);
                if (water) (y == 0 ? surfaceWater : coveredWater).add(pos);
            }
            // An eligible material keeps this Section's normal random loop running.
            level.setBlock(corner.offset(1, -1, 1), Blocks.ICE.defaultBlockState(), 2);
            MinecraftGameplayFields.upsertSphere(level, key, 0,
                    com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField.CombineMode.ADD_DELTA,
                    corner.getX() + 8, corner.getY(), corner.getZ() + 8, 32, -1000);
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(3, level.getServer());
            long scheduled = originalTime + Math.floorMod(-originalTime - chunk.getPos().x - chunk.getPos().z, interval);
            levelData.setGameTime(scheduled + 1);
            for (int attempt = 0; attempt < 512; attempt++) level.tickChunk(chunk, 3);
            for (BlockPos pos : surfaceWater) helper.assertTrue(level.getBlockState(pos).is(Blocks.WATER),
                    "normal Section ticks must not freeze water between surface updates");
            levelData.setGameTime(scheduled);
            level.tickChunk(chunk, 0);
            for (BlockPos pos : surfaceWater) helper.assertTrue(level.getBlockState(pos).is(Blocks.WATER),
                    "randomTickSpeed zero disables surface freezing too");
            int converted = 0;
            for (int attempt = 0; attempt < 256 && converted == 0; attempt++) {
                level.tickChunk(chunk, 3);
                for (BlockPos pos : surfaceWater) if (!level.getBlockState(pos).is(Blocks.WATER)) converted++;
                helper.assertTrue(converted <= 1, "one scheduled chunk call may change at most one surface water block");
            }
            helper.assertTrue(converted == 1, "surface sampling must still freeze an exposed cold water edge");
            for (BlockPos pos : coveredWater) helper.assertTrue(level.getBlockState(pos).is(Blocks.WATER),
                    "surface sampling cannot reach covered water");
        } finally {
            MinecraftGameplayFields.remove(level, key);
            levelData.setGameTime(originalTime);
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(speed, level.getServer());
            originals.forEach((pos, state) -> level.setBlock(pos, state, 2));
        }
        helper.succeed();
    }

    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) < 0.001, message + ": " + expected + " != " + actual);
    }
}

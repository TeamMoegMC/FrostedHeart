/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.mojang.authlib.GameProfile;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.SinglePlayerTeam;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.multiblock.CMultiblockHelper;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.bootstrap.common.FHMultiblocks;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorState;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorData;
import com.teammoeg.frostedheart.content.climate.data.StateTransitionData;
import com.teammoeg.frostedheart.content.climate.thermal.consumer.TownThermalProjection;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField.CombineMode;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalFieldKey;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.world.entities.CuriosityEntity;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.UUID;

import static com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ThermalLoadedWorldGameTests.*;

@GameTestHolder(FHMain.MODID)
@PrefixGameTestTemplate(false)
public final class ThermalGameplayFieldGameTests {
    private static final String TEMPLATE = "phase0a_empty";
    private static final ResourceLocation TEST_PROVIDER = new ResourceLocation("frostedheart", "gametest_field");

    @GameTest(template = TEMPLATE, batch = "thermal_fields_command", timeoutTicks = 100)
    public static void commandFieldsSurviveRuntimeReloadAndReachEveryConsumer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos position = helper.absolutePos(new BlockPos(2, 3, 2));
        MinecraftThermalInput.closeActiveLevel(level);
        try {
            command(level, "set", position, " 8 42 sphere");
            helper.assertTrue(owner(level, position) == null, "command publication must not start a runtime");
            near(helper, 42, WorldTemperature.block(level, position), "real block temperature");
            TownThermalProjection projection = new TownThermalProjection();
            projection.include(position);
            near(helper, 42, MinecraftThermalInput.gameplayTownEnvironment(level, projection, -40), "town temperature");
            level.setBlockAndUpdate(position.above(2), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(position.below(), Blocks.FARMLAND.defaultBlockState());
            level.setBlockAndUpdate(position, Blocks.WHEAT.defaultBlockState());
            command(level, "set", position, " 8 20 sphere");
            helper.assertTrue(WorldTemperature.checkPlantStatus(level, position, Blocks.WHEAT).canGrow(),
                    "real wheat growth decision must observe the warm field");
            command(level, "set", position, " 8 -40 sphere");
            helper.assertTrue(!WorldTemperature.checkPlantStatus(level, position, Blocks.WHEAT).canGrow(),
                    "real wheat growth decision must observe the cold field");
            command(level, "set", position, " 8 42 sphere");
            var fields = MinecraftGameplayFields.existing(level);
            var player = FakePlayerFactory.getMinecraft(level);
            player.setPos(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
            near(helper, 42, MinecraftThermalInput.gameplayPlayerEnvironment(player, -40,
                    new ThermalEnvironmentSample()), "player temperature");
            helper.assertTrue(read(owner(level, position).input(), "analyticFields") == fields,
                    "runtime must cache the exact same index");
            MinecraftThermalInput.invalidateGameplayProfilesForRecipeReload();
            helper.assertTrue(MinecraftGameplayFields.existing(level) == fields, "profile reload must retain field authority");
            near(helper, 42, WorldTemperature.block(level, position), "runtime-absent block temperature after reload");
            command(level, "remove", position, "");
            helper.assertTrue(!MinecraftThermalInput.hasGameplayAnalyticFieldAt(level, position), "command removal");
            helper.succeed();
        } finally {
            command(level, "remove", position, "");
            level.setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(position.above(2), Blocks.AIR.defaultBlockState());
            MinecraftThermalInput.closeActiveLevel(level);
        }
    }

    @GameTest(template = TEMPLATE, batch = "thermal_fields_fixed_time", timeoutTicks = 60)
    public static void fixedTimeDimensionsHaveIndependentFields(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel nether = overworld.getServer().getLevel(Level.NETHER);
        BlockPos position = helper.absolutePos(new BlockPos(2, 3, 2));
        try {
            command(overworld, "set", position, " 4 25 sphere");
            command(nether, "set", position, " 4 -25 sphere");
            near(helper, 25, MinecraftThermalInput.gameplayPassiveEnvironment(overworld, position, 10), "overworld field");
            near(helper, -25, MinecraftThermalInput.gameplayPassiveEnvironment(nether, position, 10), "fixed-time field");
            command(overworld, "remove", position, "");
            near(helper, -25, MinecraftThermalInput.gameplayPassiveEnvironment(nether, position, 10), "dimension isolation");
            helper.succeed();
        } finally {
            command(overworld, "remove", position, "");
            command(nether, "remove", position, "");
        }
    }

    @GameTest(template = TEMPLATE, batch = "thermal_fields_boss", timeoutTicks = 150)
    public static void realBossAiRestoresItsFieldAndDoesNotCollideWithCommands(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(2, 3, 2));
        MinecraftThermalInput.closeActiveLevel(level);
        CuriosityEntity boss = FHEntityTypes.CURIOSITY.get().create(level);
        CompoundTag tag = new CompoundTag();
        tag.putString("phase", "RISING");
        tag.putLong("arenaCenter", center.asLong());
        tag.putInt("stateTimer", 10_000);
        boss.readAdditionalSaveData(tag);
        boss.setPos(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        level.addFreshEntity(boss);
        command(level, "set", center, " 8 50 sphere");
        helper.runAfterDelay(5, () -> {
            try {
                var fields = MinecraftThermalInput.gameplayAnalyticFieldsAt(level, center);
                helper.assertTrue(fields.size() == 2, "boss AI and command at the same center must coexist");
                double cold = fields.stream().filter(f -> f.combineMode() == CombineMode.ADD_DELTA)
                        .findFirst().orElseThrow().temperatureC();
                near(helper, 50 + cold, WorldTemperature.block(level, center), "boss delta after command override");
                MinecraftThermalInput.invalidateGameplayProfilesForRecipeReload();
                near(helper, 50 + cold, WorldTemperature.block(level, center), "coldApplied survives worker reload");
                command(level, "remove", center, "");
                helper.assertTrue(MinecraftThermalInput.gameplayAnalyticFieldsAt(level, center).size() == 1,
                        "command removal must not delete the boss field");
                boss.discard();
                helper.runAfterDelay(3, () -> {
                    helper.assertTrue(!MinecraftThermalInput.hasGameplayAnalyticFieldAt(level, center), "boss removal must clean up");
                    boss.revive();
                    level.addFreshEntity(boss);
                    helper.runAfterDelay(5, () -> {
                        try {
                            helper.assertTrue(MinecraftThermalInput.hasGameplayAnalyticFieldAt(level, center),
                                    "same-object readdition must republish through normal AI");
                            helper.succeed();
                        } finally {
                            boss.discard();
                        }
                    });
                });
            } catch (Throwable failure) {
                boss.discard();
                command(level, "remove", center, "");
                throw failure;
            }
        });
    }

    @GameTest(template = TEMPLATE, batch = "thermal_fields_generator_authority", timeoutTicks = 160)
    public static void rebindAndTeamTransferRemoveOnlyTheObsoleteGenerator(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TeamDataHolder firstTeam = team(level);
        TeamDataHolder secondTeam = team(level);
        BlockPos first = form(helper, FHMultiblocks.GENERATOR_T1, helper.absolutePos(new BlockPos(1, 2, 1)));
        BlockPos second = form(helper, FHMultiblocks.GENERATOR_T1, first.east(40));
        GeneratorState oldTower = (GeneratorState) CMultiblockHelper.getBEHelper(level, first).getState();
        GeneratorState newTower = (GeneratorState) CMultiblockHelper.getBEHelper(level, second).getState();
        oldTower.setOwner(firstTeam.getId());
        newTower.setOwner(firstTeam.getId());
        oldTower.regist(level, first, (short) 1);
        heat(firstTeam);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(MinecraftThermalInput.hasGameplayAnalyticFieldAt(level, first), "first binding publication");
            newTower.regist(level, second, (short) 1);
            helper.assertTrue(!MinecraftThermalInput.hasGameplayAnalyticFieldAt(level, first), "rebind removes old field immediately");
            heat(firstTeam);
            helper.runAfterDelay(3, () -> {
                level.destroyBlock(first, false);
                helper.assertTrue(second.equals(firstTeam.getData(FHSpecialDataTypes.GENERATOR_DATA).actualPos),
                        "old tower disassembly must not clear the new authoritative binding");
                helper.assertTrue(MinecraftThermalInput.hasGameplayAnalyticFieldAt(level, second), "new floor survives old disassembly");
                CTeamDataManager.INSTANCE.transfer(secondTeam.getTeam().getId(), firstTeam.getTeam());
                helper.runAfterDelay(3, () -> {
                    helper.assertTrue(!MinecraftThermalInput.hasGameplayAnalyticFieldAt(level, second),
                            "completed team enumeration must remove the deleted holder's field");
                    level.destroyBlock(second, false);
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = TEMPLATE, batch = "thermal_fields_unloaded_generator", timeoutTicks = 900)
    public static void unloadedTowerRetainsTeamGameplayHeatWithoutReloadingItsChunk(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TeamDataHolder team = team(level);
        BlockPos origin = helper.absolutePos(new BlockPos(1, 2, 1)).east(1024);
        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;
        level.setChunkForced(chunkX, chunkZ, true);
        BlockPos master = form(helper, FHMultiblocks.GENERATOR_T1, origin);
        GeneratorState tower = (GeneratorState) CMultiblockHelper.getBEHelper(level, master).getState();
        tower.setOwner(team.getId());
        tower.regist(level, master, (short) 1);
        heat(team);
        helper.runAfterDelay(10, () -> level.setChunkForced(chunkX, chunkZ, false));
        helper.startSequence().thenWaitUntil(() -> {
            helper.assertTrue(level.getChunkSource().getChunkNow(chunkX, chunkZ) == null, "tower chunk must actually unload");
        }).thenExecute(() -> {
            try {
                near(helper, -10, MinecraftThermalInput.gameplayPassiveEnvironment(level, master, -40),
                        "unloaded authoritative team floor");
                helper.assertTrue(level.getChunkSource().getChunkNow(chunkX, chunkZ) == null,
                        "field query must not reload its source chunk");
            } finally {
                GeneratorData.unregister(level, master);
            }
        }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "thermal_fields_phase", timeoutTicks = 200)
    public static void analyticBoundsControlRealIceTransitionsInsidePhysicalOwnership(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 3, 2));
        BlockPos ice = new BlockPos((anchor.getX() & ~3) + 1, (anchor.getY() & ~3) + 1,
                (anchor.getZ() & ~3) + 1);
        BlockPos fire = ice.east();
        int randomTickSpeed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, level.getServer());
        level.setBlockAndUpdate(ice, Blocks.ICE.defaultBlockState());
        level.setBlockAndUpdate(fire.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState());
        ((ICampfireExtra) level.getBlockEntity(fire)).setLifeTime(20_000);
        start(level, ice);
        helper.runAfterDelay(50, () -> {
            ThermalFieldKey key = new ThermalFieldKey(TEST_PROVIDER, 0, ice.asLong(), 0);
            try {
                StateTransitionData data = StateTransitionData.getData(level.getBlockState(ice));
                helper.assertTrue(data != null && MinecraftThermalInput.ownsGameplayHeatingTransition(
                        level, ice, level.getBlockState(ice), data), "real ice must be owned by the physical phase path");
                MinecraftThermalInput.upsertGameplayAnalyticField(level,
                        new ThermalAnalyticField(key, 0, CombineMode.ADD_DELTA,
                                ice.getX() + 0.5, ice.getY() + 0.5, ice.getZ() + 0.5, 3, 50));
                attemptTransition(level, ice, 100);
                helper.assertTrue(level.getBlockState(ice).is(Blocks.ICE), "delta-only field must not bypass latent heat");
                command(level, "set", ice, " 3 50 sphere");
                MinecraftThermalInput.upsertGameplayAnalyticField(level,
                        new ThermalAnalyticField(key, 0, CombineMode.ADD_DELTA,
                                ice.getX() + 0.5, ice.getY() + 0.5, ice.getZ() + 0.5, 3, -100));
                attemptTransition(level, ice, 100);
                helper.assertTrue(level.getBlockState(ice).is(Blocks.ICE), "cold control must lower the analytic bound");
                MinecraftThermalInput.removeGameplayAnalyticField(level, key);
                attemptTransition(level, ice, 100);
                BlockState expected = data.heatingTransition(Blocks.ICE.defaultBlockState()).targetBlock();
                helper.assertTrue(level.getBlockState(ice) == expected,
                        "explicit bound must apply the configured ice warming stage: " + level.getBlockState(ice));
                BlockPos unowned = ice.east(160);
                level.setBlockAndUpdate(unowned, Blocks.ICE.defaultBlockState());
                try {
                    helper.assertTrue(!MinecraftThermalInput.ownsGameplayHeatingTransition(
                            level, unowned, Blocks.ICE.defaultBlockState(), data), "comparison ice must be outside physical ownership");
                    command(level, "set", unowned, " 3 50 sphere");
                    attemptTransition(level, unowned, 100);
                    helper.assertTrue(level.getBlockState(unowned) == expected,
                            "same explicit bound must apply the same stage outside physical ownership");
                } finally {
                    command(level, "remove", unowned, "");
                    level.setBlockAndUpdate(unowned, Blocks.AIR.defaultBlockState());
                }
                helper.succeed();
            } finally {
                command(level, "remove", ice, "");
                MinecraftThermalInput.removeGameplayAnalyticField(level, key);
                level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(ice, Blocks.AIR.defaultBlockState());
                level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(randomTickSpeed, level.getServer());
                MinecraftThermalInput.closeActiveLevel(level);
            }
        });
    }

    @GameTest(template = TEMPLATE, batch = "thermal_fields_query_cost", timeoutTicks = 80)
    public static void sampleProductionQueryCostWithoutStartingRuntime(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos position = helper.absolutePos(new BlockPos(2, 3, 2));
        MinecraftThermalInput.closeActiveLevel(level);
        var allocation = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long thread = Thread.currentThread().getId();
        try {
            for (int count : new int[]{1, 16, 128}) {
                for (int i = 0; i < count; i++) {
                    MinecraftGameplayFields.upsertSphere(level, new ThermalFieldKey(TEST_PROVIDER, 1, i, 0), 0,
                            CombineMode.FLOOR_FROM_NATURAL, position.getX() + 0.5, position.getY() + 0.5,
                            position.getZ() + 0.5, 4 + i, 30);
                }
                for (int i = 0; i < 20_000; i++) MinecraftThermalInput.gameplayPassiveEnvironment(level, position, -40);
                long bytes = allocation.getThreadAllocatedBytes(thread);
                long begin = System.nanoTime();
                double sum = 0;
                for (int i = 0; i < 20_000; i++) sum += MinecraftThermalInput.gameplayPassiveEnvironment(level, position, -40);
                long elapsed = System.nanoTime() - begin;
                long allocated = allocation.getThreadAllocatedBytes(thread) - bytes;
                near(helper, -200_000, sum, "production floor query result");
                FHMain.LOGGER.info("Analytic production query sample: fields={}, queries=20000, ns/query={}, allocatedBytes={}",
                        count, elapsed / 20_000.0, allocated);
            }
            helper.assertTrue(owner(level, position) == null, "analytic queries must leave physical runtime absent");
            helper.succeed();
        } finally {
            for (int i = 0; i < 128; i++) MinecraftGameplayFields.remove(level, new ThermalFieldKey(TEST_PROVIDER, 1, i, 0));
        }
    }

    @GameTest(template = TEMPLATE, batch = "thermal_fields_enclosure", timeoutTicks = 900)
    public static void generatorPhysicalHeatCanExceedItsAnalyticFloorInAnEnclosure(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 2, 1));
        int height = FHMultiblocks.Registration.GENERATOR_T1.size(level).getY();
        // Exercise the real tower and its exhaust in the same Brick.
        BlockPos origin = new BlockPos(anchor.getX(), ((anchor.getY() + height + 3) & ~3) + 1 - height, anchor.getZ());
        BlockPos enclosed = form(helper, FHMultiblocks.GENERATOR_T1, origin);
        BlockPos outdoorColumn = origin.east(40);
        int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                outdoorColumn.getX() + 1, outdoorColumn.getZ() + 1);
        BlockPos openOrigin = new BlockPos(outdoorColumn.getX(), Math.max(origin.getY(), surfaceY + 1), outdoorColumn.getZ());
        level.setChunkForced(openOrigin.getX() >> 4, openOrigin.getZ() >> 4, true);
        BlockPos open = form(helper, FHMultiblocks.GENERATOR_T1, openOrigin);
        TeamDataHolder enclosedTeam = team(level);
        TeamDataHolder openTeam = team(level);
        GeneratorState enclosedTower = (GeneratorState) CMultiblockHelper.getBEHelper(level, enclosed).getState();
        GeneratorState openTower = (GeneratorState) CMultiblockHelper.getBEHelper(level, open).getState();
        enclosedTower.setOwner(enclosedTeam.getId());
        openTower.setOwner(openTeam.getId());
        enclosedTower.regist(level, enclosed, (short) 1);
        openTower.regist(level, open, (short) 1);
        heat(enclosedTeam);
        heat(openTeam);
        enclosedTeam.getData(FHSpecialDataTypes.GENERATOR_DATA).isActive = true;
        openTeam.getData(FHSpecialDataTypes.GENERATOR_DATA).isActive = true;
        var multiblock = CMultiblockHelper.getBEHelper(level, enclosed).getMultiblock();
        int exhaustOffset = multiblock.size(level).getY() - multiblock.masterPosInMB().getY();
        BlockPos enclosedAir = enclosed.above(exhaustOffset);
        BlockPos openAir = open.above(exhaustOffset);
        helper.assertTrue(level.getBlockState(enclosedAir).isAir(), "actual exhaust target must be Air");
        helper.assertTrue(level.getBlockState(openAir).isAir() && level.canSeeSky(openAir),
                "outdoor exhaust must actually be Air exposed to the sky");
        for (Direction face : Direction.values()) {
            BlockPos wall = enclosedAir.relative(face);
            if (level.getBlockState(wall).isAir()) level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
        }
        MinecraftThermalInput input = start(level, enclosedAir);
        com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.PhysicalSourceSpatialIndex physicalSources = read(input, "physicalSources");
        it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap sourceSlots = read(physicalSources, "slotsById");
        BlockPos openSource = open.below(multiblock.masterPosInMB().getY());
        helper.startSequence().thenWaitUntil(() -> {
            double natural = WorldTemperature.naturalBlock(level, enclosedAir);
            double raw = rawAir(input, enclosedAir);
            helper.assertTrue(Double.isFinite(raw) && raw > natural + 30.25,
                    "normal generator exhaust must heat enclosed Air above its floor: raw=" + raw + ", natural=" + natural);
            helper.assertTrue(Double.isFinite(rawAir(input, openAir)),
                    "outdoor physical target unavailable: source=" + sourceSlots.containsKey(openSource.asLong())
                            + ", temperatureLevel=" + openTower.getTempLevel()
                            + ", active=" + openTeam.getData(FHSpecialDataTypes.GENERATOR_DATA).isActive
                            + ", targetState=" + level.getBlockState(openAir)
                            + ", page=" + owner(level, openAir).page());
        }).thenExecute(() -> {
            try {
                double raw = rawAir(input, enclosedAir);
                double natural = WorldTemperature.naturalBlock(level, enclosedAir);
                near(helper, raw, WorldTemperature.block(level, enclosedAir), "physical enhancement above the floor");
                var dropped = new ItemEntity(level,
                        enclosedAir.getX() + 0.5, enclosedAir.getY(), enclosedAir.getZ() + 0.5,
                        new ItemStack(FHItems.warm_stone.get()));
                dropped.setPos(dropped.getX(), enclosedAir.getY() + 0.5 - dropped.getBbHeight() * 0.5, dropped.getZ());
                ThermalEnvironmentSample itemSample = new ThermalEnvironmentSample();
                double itemNatural = WorldTemperature.naturalAir(level, enclosedAir);
                MinecraftThermalInput.gameplayItemEnvironment(dropped, itemNatural, itemSample);
                near(helper, Math.max(raw, itemNatural + enclosedTeam.getData(FHSpecialDataTypes.GENERATOR_DATA).getTempMod()),
                        itemSample.airTemperatureC(), "dropped reservoir must not add the tower floor to physical heat");
                dropped.discard();
                double outdoorNatural = WorldTemperature.naturalBlock(level, openAir);
                double outdoorRaw = rawAir(input, openAir);
                double outdoor = WorldTemperature.block(level, openAir);
                helper.assertTrue(outdoor >= outdoorNatural + 30 - 0.001, "outdoor floor must hold despite dissipation");
                FHMain.LOGGER.info("Generator production enclosure: natural={}, floor={}, raw={}, composed={}; outdoor natural={}, raw={}, composed={}",
                        natural, natural + 30, raw, WorldTemperature.block(level, enclosedAir), outdoorNatural, outdoorRaw, outdoor);
            } finally {
                level.destroyBlock(enclosed, false);
                level.destroyBlock(open, false);
                level.setChunkForced(openOrigin.getX() >> 4, openOrigin.getZ() >> 4, false);
                MinecraftThermalInput.closeActiveLevel(level);
            }
        }).thenSucceed();
    }

    private static TeamDataHolder team(ServerLevel level) {
        prepareProfileCache(level.getServer());
        UUID id = UUID.randomUUID();
        level.getServer().getProfileCache().add(new GameProfile(id, "Fields" + id.toString().substring(0, 8)));
        return CTeamDataManager.INSTANCE.get(new SinglePlayerTeam(id));
    }

    private static void heat(TeamDataHolder team) {
        var data = team.getData(FHSpecialDataTypes.GENERATOR_DATA);
        data.RLevel = 1;
        data.TLevel = 3;
        data.townProcessedTicks = 10_000;
    }

    private static void command(ServerLevel level, String action, BlockPos position, String suffix) {
        level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack()
                        .withLevel(level).withSuppressedOutput(),
                "heat_adjust " + action + " " + position.getX() + " " + position.getY() + " " + position.getZ() + suffix);
    }

    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) < 0.001, message + ": expected=" + expected + ", actual=" + actual);
    }

    private static void attemptTransition(ServerLevel level, BlockPos position, int attempts) {
        try {
            Method method = ServerLevel.class.getDeclaredMethod("frostedHeart$updateBlockBasedOnTemperature",
                    LevelChunk.class, ServerLevel.class, BlockPos.class, BlockState.class, StateTransitionData.class, float.class);
            method.setAccessible(true);
            for (int i = 0; i < attempts && level.getBlockState(position).is(Blocks.ICE); i++) {
                BlockState state = level.getBlockState(position);
                method.invoke(level, level.getChunkAt(position), level, position, state,
                        StateTransitionData.getData(state), WorldTemperature.climate(level, position));
            }
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static double rawAir(MinecraftThermalInput input, BlockPos position) {
        try {
            Method method = MinecraftThermalInput.class.getDeclaredMethod("sampleAir",
                    double.class, double.class, double.class, long.class, int.class, ThermalEnvironmentSample.class);
            method.setAccessible(true);
            ThermalEnvironmentSample out = new ThermalEnvironmentSample();
            ServerLevel level = read(input, "level");
            method.invoke(input, position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5,
                    level.getGameTime(), 40, out);
            return out.airTemperatureC();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

}

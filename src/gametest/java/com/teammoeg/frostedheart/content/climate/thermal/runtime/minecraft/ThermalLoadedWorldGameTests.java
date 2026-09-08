/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import blusunrize.immersiveengineering.api.multiblocks.TemplateMultiblock;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.SinglePlayerTeam;
import com.teammoeg.chorda.multiblock.CMultiblockHelper;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHMultiblocks;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorState;
import com.teammoeg.frostedheart.content.climate.block.radiator.RadiatorState;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPageManager;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftThermalSectionAttachment;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.PhysicalSourceSpatialIndex;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import com.teammoeg.frostedheart.content.steamenergy.fountain.FountainTileEntity;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;

@GameTestHolder(FHMain.MODID)
@PrefixGameTestTemplate(false)
public final class ThermalLoadedWorldGameTests {
    private static final String TEMPLATE = "phase0a_empty";

    @GameTest(template = TEMPLATE, batch = "thermal_pending_be", timeoutTicks = 120)
    public static void pendingCampfireIsDiscoveredWithoutInstantiatingItsEntity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos fire = helper.absolutePos(new BlockPos(2, 2, 2));
        campfire(level, fire, true);
        LevelChunk chunk = level.getChunkAt(fire);
        CompoundTag stored = chunk.getBlockEntityNbtForSaving(fire);
        level.removeBlockEntity(fire);
        chunk.setBlockEntityNbt(stored);
        MinecraftThermalInput input = start(level, fire);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(present(input, fire), "pending NBT position must restore the lit source");
            helper.assertTrue(!chunk.getBlockEntities().containsKey(fire),
                    "source discovery must not instantiate the pending block entity");
            level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
            MinecraftThermalInput.closeActiveLevel(level);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "thermal_zero_and_radiation", timeoutTicks = 160)
    public static void zeroPowerAndPureRadiationDoNotSeedConvection(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos fire = helper.absolutePos(new BlockPos(2, 2, 2));
        campfire(level, fire, true);
        double power = FHConfig.COMMON.THERMAL_RUNTIME.campfirePowerW.get();
        double share = FHConfig.COMMON.THERMAL_RUNTIME.campfireRadiationShare.get();
        FHConfig.COMMON.THERMAL_RUNTIME.campfirePowerW.set(0.0);
        MinecraftThermalInput.invalidateGameplayProfilesForRecipeReload();
        MinecraftThermalInput input = start(level, fire);
        helper.runAfterDelay(40, () -> {
            try {
                helper.assertTrue(!present(input, fire), "zero-power fire must not occupy a source slot");
            } finally {
                FHConfig.COMMON.THERMAL_RUNTIME.campfirePowerW.set(power);
                FHConfig.COMMON.THERMAL_RUNTIME.campfireRadiationShare.set(1.0);
                MinecraftThermalInput.invalidateGameplayProfilesForRecipeReload();
            }
            MinecraftThermalInput radiation = start(level, fire);
            helper.runAfterDelay(40, () -> {
                try {
                    PhysicalSourceSpatialIndex sources = read(radiation, "physicalSources");
                    Long2IntOpenHashMap slots = read(sources, "slotsById");
                    helper.assertTrue(present(radiation, fire), "pure radiation must retain its source");
                    byte[] targets = read(sources, "targetCount");
                    helper.assertTrue(targets[slots.get(fire.asLong())] == 0,
                            "zero-share AIR_FACE must not create convection targets");
                    helper.succeed();
                } finally {
                    level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
                    FHConfig.COMMON.THERMAL_RUNTIME.campfireRadiationShare.set(share);
                    MinecraftThermalInput.invalidateGameplayProfilesForRecipeReload();
                }
            });
        });
    }

    @GameTest(template = TEMPLATE, batch = "thermal_loaded_world_reload", timeoutTicks = 260)
    public static void loadedCampfiresRecoverAndLaterMutationSurvivesReload(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos fire = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos later = fire.east(2);
        campfire(level, fire, true);
        campfire(level, later, false);
        MinecraftThermalInput first = start(level, fire);
        MinecraftPageManager.SectionOwner firstOwner = owner(level, fire);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(present(first, fire) && !present(first, later),
                    "startup must discover the existing lit fire without retaining the unlit fire");
            MinecraftThermalInput.invalidateGameplayProfilesForRecipeReload();
            MinecraftThermalInput second = start(level, fire);
            helper.assertTrue(second != first && owner(level, fire) != firstOwner,
                    "full reload must attach fresh owners to already loaded sections");
            first.close();
            helper.assertTrue(owner(level, fire).input() == second,
                    "old close must not detach the new runtime");
            helper.runAfterDelay(60, () -> {
                helper.assertTrue(present(second, fire), "unchanged lit fire must recover after reload");
                MinecraftPageManager.SectionOwner before = owner(level, later);
                campfire(level, later, true);
                MinecraftThermalInput.onChunkLoad(level, level.getChunkAt(later));
                helper.assertTrue(owner(level, later) == before, "repeat load must retain pending mutation");
                helper.runAfterDelay(40, () -> {
                    helper.assertTrue(present(second, later), "later ignition must reach the new owner");
                    level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(later, Blocks.AIR.defaultBlockState());
                    helper.runAfterDelay(40, () -> {
                        helper.assertTrue(!present(second, fire) && !present(second, later),
                                "removed fires must release their slots");
                        MinecraftThermalInput.closeActiveLevel(level);
                        helper.succeed();
                    });
                });
            });
        });
    }

    @GameTest(template = TEMPLATE, batch = "thermal_loaded_world_capacity", timeoutTicks = 600)
    public static void rejectedMutationRecoversThroughTheRuntimeQueue(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos first = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos second = first.east(2);
        campfire(level, first, false);
        campfire(level, second, false);
        MinecraftThermalInput input = start(level, first);
        helper.startSequence().thenWaitUntil(() -> {
            Long2ObjectLinkedOpenHashMap<LevelChunk> pending = read(input, "pendingSourceChunks");
            helper.assertTrue(pending.isEmpty(), "initial discovery must settle before constraining capacity");
        }).thenExecute(() -> {
            PhysicalSourceSpatialIndex sources = read(input, "physicalSources");
            int highWater = read(sources, "highWaterMark");
            write(sources, "maximumSources", highWater + 1);
            campfire(level, first, true);
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(present(input, first), "first fire must occupy the available slot");
                campfire(level, second, true);
                helper.runAfterDelay(30, () -> {
                    helper.assertTrue(!present(input, second), "second fire must be refused at capacity");
                    Long2ObjectLinkedOpenHashMap<LevelChunk> pending = read(input, "pendingSourceChunks");
                    helper.assertTrue(pending.containsKey(level.getChunkAt(second).getPos().toLong()),
                            "mutation refusal must retain its exact chunk");
                    level.setBlockAndUpdate(first, Blocks.AIR.defaultBlockState());
                    helper.runAfterDelay(60, () -> {
                        helper.assertTrue(!present(input, first) && present(input, second),
                                "normal removal and cut must free capacity for the queued fire: first="
                                        + present(input, first) + ", second=" + present(input, second)
                                        + ", pending=" + pending.size()
                                        + ", slots=" + read(sources, "slotsById")
                                        + ", state=" + level.getBlockState(second));
                        level.setBlockAndUpdate(second, Blocks.AIR.defaultBlockState());
                        MinecraftThermalInput.closeActiveLevel(level);
                        helper.succeed();
                    });
                });
            });
        });
    }

    @GameTest(template = TEMPLATE, batch = "thermal_loaded_world_owner", timeoutTicks = 160)
    public static void sectionReplacementAndAsyncMutationUseTheCurrentOwner(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos fire = helper.absolutePos(new BlockPos(2, 2, 2));
        campfire(level, fire, false);
        MinecraftThermalInput input = start(level, fire);
        LevelChunk chunk = level.getChunkAt(fire);
        int index = chunk.getSectionIndex(fire.getY());
        LevelChunkSection previous = chunk.getSections()[index];
        LevelChunkSection replacement = new LevelChunkSection(level.registryAccess().registryOrThrow(
                net.minecraft.core.registries.Registries.BIOME));
        // Preserve the real section contents while changing its identity.
        for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            replacement.setBlockState(x, y, z, previous.getBlockState(x, y, z));
        }
        chunk.getSections()[index] = replacement;
        MinecraftThermalInput.onSectionIdentityReplaced(level, chunk, index, previous);
        MinecraftPageManager.SectionOwner current = owner(level, fire);
        helper.assertTrue(((MinecraftThermalSectionAttachment) (Object) previous)
                .frostedheart$getThermalInputOwner() == null, "replaced section must be detached");
        CompletableFuture.runAsync(() -> {
            replacement.setBlockState(fire.getX() & 15, fire.getY() & 15, fire.getZ() & 15,
                    Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
            current.recordFullResync(ThermalPageHandle.GeometryResyncReason.EXPLICIT_INVALIDATION);
        }).join();
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(present(input, fire), "off-thread mutation/full resync must be consumed");
            level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(!present(input, fire), "owner must enqueue again after its first drain");
                MinecraftThermalInput.closeActiveLevel(level);
                helper.succeed();
            });
        });
    }

    @GameTest(template = TEMPLATE, batch = "thermal_machine_t1", timeoutTicks = 340)
    public static void formedT1RecoversFromItsNormalProductionTick(GameTestHelper helper) {
        generator(helper, FHMultiblocks.GENERATOR_T1);
    }

    @GameTest(template = TEMPLATE, batch = "thermal_machine_t2", timeoutTicks = 340)
    public static void formedT2RecoversFromItsNormalProductionTick(GameTestHelper helper) {
        generator(helper, FHMultiblocks.GENERATOR_T2);
    }

    private static void generator(GameTestHelper helper, TemplateMultiblock structure) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos origin = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos master = form(helper, structure, origin);
        GeneratorState state = (GeneratorState) CMultiblockHelper.getBEHelper(level, master).getState();
        var player = FakePlayerFactory.getMinecraft(level);
        prepareProfileCache(level.getServer());
        level.getServer().getProfileCache().add(player.getGameProfile());
        var team = CTeamDataManager.INSTANCE.get(new SinglePlayerTeam(player.getUUID()));
        state.setOwner(team.getId());
        state.regist(level, master, (short) structure.getMasterFromOriginOffset().getY());
        var data = state.getData(master).orElseThrow();
        data.isWorking = true;
        data.isActive = true;
        data.TLevel = 10;
        data.RLevel = 1;
        data.heated = 1_000;
        data.process = 10_000;
        data.townProcessedTicks = 1_000;
        BlockPos source = master.below(structure.getMasterFromOriginOffset().getY());
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(data.townProcessedTicks < 1_000 && state.getTempLevel() == 10,
                    "formed machine must tick through the town-processed branch before runtime starts");
            int fuel = data.process;
            MinecraftThermalInput input = start(level, source);
            helper.assertTrue(data.process == fuel, "bootstrap must not consume generator fuel");
            helper.runAfterDelay(50, () -> {
                helper.assertTrue(present(input, source), "normal generator tick must restore source");
                MinecraftThermalInput.invalidateGameplayProfilesForRecipeReload();
                MinecraftThermalInput next = start(level, source);
                helper.runAfterDelay(50, () -> {
                    helper.assertTrue(present(next, source) && data.process == fuel,
                            "unchanged production must recover without duplicate fuel consumption");
                    data.isActive = false;
                    helper.runAfterDelay(40, () -> {
                        helper.assertTrue(!present(next, source), "inactive generator must release its source");
                        data.isActive = true;
                        helper.runAfterDelay(40, () -> {
                            helper.assertTrue(present(next, source), "generator must register after restart");
                            level.destroyBlock(master, false);
                            helper.runAfterDelay(40, () -> {
                                helper.assertTrue(!present(next, source), "disassembly must remove the source ID");
                                MinecraftThermalInput.closeActiveLevel(level);
                                helper.succeed();
                            });
                        });
                    });
                });
            });
        });
    }

    @GameTest(template = TEMPLATE, batch = "thermal_machine_radiator", timeoutTicks = 200)
    public static void formedRadiatorRestoresAndStopsThroughHeatConsumption(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos master = form(helper, FHMultiblocks.RADIATOR,
                helper.absolutePos(new BlockPos(1, 2, 1)));
        RadiatorState state = (RadiatorState) CMultiblockHelper.getBEHelper(level, master).getState();
        HeatEndpoint endpoint = read(state, "network");
        endpoint.setTempLevel(10);
        endpoint.setHeat(400);
        BlockPos source = master.below(FHMultiblocks.RADIATOR.getMasterFromOriginOffset().getY());
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(endpoint.getHeat() < 400 && state.isActive(), "real radiator tick must consume heat");
            float before = endpoint.getHeat();
            MinecraftThermalInput input = start(level, source);
            helper.assertTrue(endpoint.getHeat() == before, "bootstrap must not drain extra heat");
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(present(input, source), "radiator normal tick must publish its source");
                MinecraftThermalInput.invalidateGameplayProfilesForRecipeReload();
                MinecraftThermalInput next = start(level, source);
                helper.runAfterDelay(30, () -> {
                    helper.assertTrue(present(next, source), "radiator must recover after reload");
                    endpoint.setHeat(0);
                    helper.runAfterDelay(40, () -> {
                        helper.assertTrue(!present(next, source), "empty radiator must release its source");
                        level.destroyBlock(master, false);
                        MinecraftThermalInput.closeActiveLevel(level);
                        helper.succeed();
                    });
                });
            });
        });
    }

    @GameTest(template = TEMPLATE, batch = "thermal_machine_fountain", timeoutTicks = 200)
    public static void fountainRestoresFromItsNormalBlockEntityTick(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos source = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(source, FHBlocks.FOUNTAIN_BASE.get().defaultBlockState());
        level.setBlockAndUpdate(source.above(), FHBlocks.FOUNTAIN_NOZZLE.get().defaultBlockState());
        FountainTileEntity fountain = (FountainTileEntity) level.getBlockEntity(source);
        CompoundTag stored = new CompoundTag();
        stored.putFloat("power", 400);
        stored.putInt("height", 1);
        fountain.readCustomNBT(stored, false);
        HeatEndpoint endpoint = read(fountain, "network");
        endpoint.setTempLevel(10);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(fountain.isWorking() && (float) read(fountain, "power") < 400,
                    "existing fountain must run before runtime starts");
            MinecraftThermalInput input = start(level, source);
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(present(input, source), "normal fountain tick must restore heat");
                MinecraftThermalInput.invalidateGameplayProfilesForRecipeReload();
                MinecraftThermalInput next = start(level, source);
                helper.runAfterDelay(40, () -> {
                    helper.assertTrue(present(next, source), "fountain must recover after reload");
                    level.destroyBlock(source, false);
                    helper.runAfterDelay(40, () -> {
                        helper.assertTrue(!present(next, source), "fountain removal must release heat");
                        MinecraftThermalInput.closeActiveLevel(level);
                        helper.succeed();
                    });
                });
            });
        });
    }

    private static BlockPos form(GameTestHelper helper, TemplateMultiblock structure, BlockPos origin) {
        ServerLevel level = helper.getLevel();
        for (var block : structure.getStructure(level)) {
            level.setBlockAndUpdate(origin.offset(block.pos()), block.state());
        }
        helper.assertTrue(structure.createStructure(level, origin.offset(structure.getTriggerOffset()),
                Direction.SOUTH, FakePlayerFactory.getMinecraft(level)), "production multiblock must form");
        return origin.offset(structure.getMasterFromOriginOffset());
    }

    private static void prepareProfileCache(MinecraftServer server) {
        if (server.getProfileCache() != null) return;
        // GameTestServer omits this standard server service; real team data needs it.
        try {
            Field field = MinecraftServer.class.getDeclaredField("services");
            field.setAccessible(true);
            Services original = (Services) field.get(server);
            GameProfileCache cache = new GameProfileCache(original.profileRepository(),
                    server.getWorldPath(LevelResource.ROOT).resolve("usercache.json").toFile());
            field.set(server, new Services(original.sessionService(), original.servicesKeySet(),
                    original.profileRepository(), cache));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void campfire(ServerLevel level, BlockPos position, boolean lit) {
        level.setBlockAndUpdate(position.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(position, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, lit));
        ((ICampfireExtra) level.getBlockEntity(position)).setLifeTime(20_000);
    }

    private static MinecraftThermalInput start(ServerLevel level, BlockPos center) {
        var player = FakePlayerFactory.getMinecraft(level);
        player.setPos(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5);
        long begin = System.nanoTime();
        MinecraftThermalInput.gameplayPlayerEnvironment(player, WorldTemperature.naturalAir(level, center),
                new ThermalEnvironmentSample());
        FHMain.LOGGER.info("Thermal production test startup: {} ms", (System.nanoTime() - begin) / 1_000_000.0);
        return owner(level, center).input();
    }

    private static MinecraftPageManager.SectionOwner owner(ServerLevel level, BlockPos position) {
        LevelChunk chunk = level.getChunkAt(position);
        return ((MinecraftThermalSectionAttachment) (Object) chunk.getSection(chunk.getSectionIndex(position.getY())))
                .frostedheart$getThermalInputOwner();
    }

    private static boolean present(MinecraftThermalInput input, BlockPos source) {
        PhysicalSourceSpatialIndex sources = read(input, "physicalSources");
        Long2IntOpenHashMap slots = read(sources, "slotsById");
        return slots.containsKey(source.asLong());
    }

    @SuppressWarnings("unchecked")
    private static <T> T read(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void write(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}

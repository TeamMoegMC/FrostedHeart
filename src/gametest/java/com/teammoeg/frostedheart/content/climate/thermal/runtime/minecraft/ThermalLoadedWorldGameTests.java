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
import com.teammoeg.frostedheart.content.climate.data.BiomeTempData;
import com.teammoeg.frostedheart.content.climate.data.WorldTempData;
import com.teammoeg.frostedheart.infrastructure.data.FHRecipeCachingReloadListener;
import com.teammoeg.frostedheart.content.climate.thermal.consumer.TownThermalProjection;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorState;
import com.teammoeg.frostedheart.content.climate.block.radiator.RadiatorState;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;

@GameTestHolder(FHMain.MODID)
@PrefixGameTestTemplate(false)
public final class ThermalLoadedWorldGameTests {
    private static final String TEMPLATE = "phase0a_empty";

    @GameTest(template = TEMPLATE, batch = "thermal_complete_tag_reload", timeoutTicks = 300)
    public static void completeReloadRebuildsMaterialProfilesAfterTagsBind(GameTestHelper helper) throws IOException {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos fire = helper.absolutePos(new BlockPos(2, 2, 2));
        campfire(level, fire, true);
        MinecraftThermalInput original = owner(level, fire).input();
        var before = MinecraftThermalProfiles.prepare();
        int stoneMaterial = before.signatures().materialProfileId(before.states().signatureId(Blocks.STONE.defaultBlockState()));
        int woolMaterial = before.signatures().materialProfileId(before.states().signatureId(Blocks.WHITE_WOOL.defaultBlockState()));
        helper.assertTrue(stoneMaterial != woolMaterial, "the fixture must change a real material classification");
        var selected = new ArrayList<>(server.getPackRepository().getSelectedIds());
        Path packs = Files.createDirectories(server.getWorldPath(LevelResource.DATAPACK_DIR));
        Path pack = Files.createTempDirectory(packs, "thermal-tags-");
        try {
            Files.writeString(pack.resolve("pack.mcmeta"),
                    "{\"pack\":{\"pack_format\":15,\"description\":\"Thermal GameTest tag reload\"}}");
            Path tags = Files.createDirectories(pack.resolve("data/minecraft/tags/blocks"));
            Files.writeString(tags.resolve("wool.json"), "{\"replace\":false,\"values\":[\"minecraft:stone\"]}");
            server.getPackRepository().reload();
            String packId = "file/" + pack.getFileName();
            helper.assertTrue(server.getPackRepository().getAvailableIds().contains(packId),
                    "the server must discover the real test datapack");
            var withFixture = new ArrayList<>(selected);
            withFixture.add(packId);
            server.reloadResources(withFixture).join();
            helper.assertTrue(Blocks.STONE.defaultBlockState().is(net.minecraft.tags.BlockTags.WOOL),
                    "the complete server reload must bind the new wool tag");
            helper.assertTrue(owner(level, fire) != null && owner(level, fire).input() != original,
                    "the loaded campfire must recover without a player query after tags bind");
            var after = MinecraftThermalProfiles.prepare();
            helper.assertTrue(after.signatures().materialProfileId(after.states().signatureId(Blocks.STONE.defaultBlockState()))
                            == woolMaterial,
                    "the restarted runtime must use the newly bound material tag, not the previous snapshot");
        } finally {
            try {
                server.reloadResources(selected).join();
            } finally {
                level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
                MinecraftThermalInput.closeActiveLevel(level);
                try (var paths = Files.walk(pack)) {
                    for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
                }
                server.getPackRepository().reload();
            }
        }
        var restored = MinecraftThermalProfiles.prepare();
        helper.assertTrue(restored.signatures().materialProfileId(restored.states().signatureId(Blocks.STONE.defaultBlockState()))
                        == stoneMaterial, "removing the test pack must restore the original material table");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "thermal_recipe_temperature_reload", timeoutTicks = 100)
    public static void recipeRebuildInvalidatesCachedNaturalTemperatures(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var biome = level.getBiome(pos).value();
        var biomeKey = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getKey(biome);
        var oldBiomes = BiomeTempData.cacheList;
        var oldWorlds = WorldTempData.cacheList;
        float expectedBiome = BiomeTempData.getBiomeTemp(level, biome);
        float expectedWorld = WorldTempData.getWorldTemp(level);
        try {
            BiomeTempData.cacheList = new HashMap<>(oldBiomes);
            BiomeTempData.cacheList.put(biomeKey, new BiomeTempData(biomeKey, expectedBiome + 30));
            WorldTempData.cacheList = new HashMap<>(oldWorlds);
            WorldTempData.cacheList.put(level.dimension().location(), new WorldTempData(level.dimension().location(), expectedWorld + 40));
            WorldTemperature.clear();
            helper.assertTrue(WorldTemperature.biome(level, pos) == expectedBiome + 30
                            && WorldTemperature.dimension(level) == expectedWorld + 40,
                    "old temperature tables must populate the real lookup caches");
            FHRecipeCachingReloadListener.buildRecipeLists(level.getRecipeManager());
            helper.assertTrue(WorldTemperature.biome(level, pos) == expectedBiome
                            && WorldTemperature.dimension(level) == expectedWorld,
                    "production recipe rebuild must expose the loaded recipes without manual cache clearing");
            FHRecipeCachingReloadListener.buildRecipeLists(new net.minecraft.world.item.crafting.RecipeManager());
            helper.assertTrue(BiomeTempData.cacheList.isEmpty() && WorldTempData.cacheList.isEmpty()
                            && WorldTemperature.biome(level, pos) == 0,
                    "an empty recipe set must remove previous temperature recipes and cached results");
            helper.succeed();
        } finally {
            FHRecipeCachingReloadListener.buildRecipeLists(level.getRecipeManager());
        }
    }

    @GameTest(template = TEMPLATE, batch = "thermal_natural_refresh_backlog", timeoutTicks = 140)
    public static void overdueNaturalRefreshWaitsAFullIntervalAfterSampling(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos fire = helper.absolutePos(new BlockPos(2, 2, 2));
        campfire(level, fire, true);
        helper.runAfterDelay(40, () -> {
            MinecraftThermalInput input = owner(level, fire).input();
            Object environment = read(input, "environment");
            PriorityQueue<?> queue = read(environment, "naturalQueue");
            helper.assertTrue(!queue.isEmpty(), "a real source Page must be tracked for natural refresh");
            Object entry = queue.peek();
            long delayedAt = level.getGameTime();
            // Emulate a delayed head entry in the real production queue.
            write(entry, "nextRefreshTick", delayedAt - 990L);
            helper.runAfterDelay(2, () -> {
                try {
                    long next = read(entry, "nextRefreshTick");
                    helper.assertTrue(next >= delayedAt + 200 && next <= level.getGameTime() + 200,
                            "current-world sampling must schedule a full interval, not catch up old deadlines");
                    helper.succeed();
                } finally {
                    level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
                    MinecraftThermalInput.closeActiveLevel(level);
                }
            });
        });
    }

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
        MinecraftThermalInput.closeActiveLevel(level);
        MinecraftThermalInput.onChunkLoad(level, chunk);
        helper.assertTrue(owner(level, fire) != null, "loaded pending campfire must bootstrap without player queries");
        MinecraftThermalInput input = owner(level, fire).input();
        // Assert at the production discovery boundary. Vanilla may promote
        // pending block entities while the chunk ticks afterward.
        PhysicalSourceSpatialIndex sources = read(input, "physicalSources");
        helper.assertTrue(sources.discoverChunk(chunk), "pending source discovery must complete");
        helper.assertTrue(present(input, fire), "pending NBT position must restore the lit source");
        helper.assertTrue(!chunk.getBlockEntities().containsKey(fire),
                "source discovery must not instantiate the pending block entity");
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(present(input, fire), "pending NBT position must restore the lit source");
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

    @GameTest(template = TEMPLATE, batch = "thermal_loaded_world_reload", timeoutTicks = 600)
    public static void loadedCampfiresRecoverAndLaterMutationSurvivesReload(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos fire = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos later = fire.east(2);
        level.setChunkForced(fire.getX() >> 4, fire.getZ() >> 4, true);
        campfire(level, fire, true);
        campfire(level, later, false);
        helper.assertTrue(owner(level, fire) != null, "ignition must start runtime without a player query");
        MinecraftThermalInput first = owner(level, fire).input();
        MinecraftPageManager.SectionOwner firstOwner = owner(level, fire);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(present(first, fire) && !present(first, later),
                    "startup must discover the existing lit fire without retaining the unlit fire");
            level.getServer().reloadResources(level.getServer().getPackRepository().getSelectedIds()).join();
            helper.assertTrue(owner(level, fire) != null, "reload must rediscover loaded lit sources without players");
            MinecraftThermalInput second = owner(level, fire).input();
            helper.assertTrue(second != first && owner(level, fire) != firstOwner,
                    "full reload must attach fresh owners to already loaded sections");
            first.close();
            helper.assertTrue(owner(level, fire).input() == second,
                    "old close must not detach the new runtime");
            helper.startSequence().thenWaitUntil(() -> {
                var loaded = level.getChunkSource().getChunkNow(fire.getX() >> 4, fire.getZ() >> 4);
                Long2ObjectLinkedOpenHashMap<LevelChunk> pending = read(second, "pendingSourceChunks");
                helper.assertTrue(present(second, fire), "unchanged lit fire must recover after reload: chunk="
                        + (loaded == null ? "unloaded" : loaded.getBlockState(fire))
                        + ", pending=" + pending.size() + ", runtimeClosed=" + read(second, "closed"));
            }).thenExecute(() -> {
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
                        level.setChunkForced(fire.getX() >> 4, fire.getZ() >> 4, false);
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
        ArrayList<BlockPos> capacityFillers = new ArrayList<>();
        campfire(level, first, false);
        campfire(level, second, false);
        MinecraftThermalInput input = start(level, first);
        helper.startSequence().thenWaitUntil(() -> {
            Long2ObjectLinkedOpenHashMap<LevelChunk> pending = read(input, "pendingSourceChunks");
            helper.assertTrue(pending.isEmpty(), "initial discovery must settle before constraining capacity");
        }).thenExecute(() -> {
            PhysicalSourceSpatialIndex sources = read(input, "physicalSources");
            campfire(level, first, true);
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(present(input, first), "first fire must occupy the available slot");
                int highWater = read(sources, "highWaterMark");
                write(sources, "maximumSources", highWater);
                // Earlier world activity may have recycled slots below highWater.
                // Fill those with real fueled campfires before testing refusal.
                while (sources.hasAvailableCapacity()) {
                    BlockPos filler = first.above(4 + 2 * capacityFillers.size());
                    campfire(level, filler, true);
                    capacityFillers.add(filler);
                    helper.assertTrue(sources.resyncBlock(filler.getX(), filler.getY(), filler.getZ(),
                            level.getBlockState(filler)), "capacity filler must occupy a recycled slot");
                }
                FHMain.LOGGER.info("Source capacity production fixture: limit={}, recycled slots filled={}",
                        highWater, capacityFillers.size());
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
                        for (BlockPos filler : capacityFillers) {
                            level.setBlockAndUpdate(filler, Blocks.AIR.defaultBlockState());
                            level.setBlockAndUpdate(filler.below(), Blocks.AIR.defaultBlockState());
                        }
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
                    "formed machine must tick through the town-processed branch");
            BlockPos query = source.east(5);
            double natural = WorldTemperature.naturalBlock(level, query);
            double floor = natural + data.getTempMod();
            helper.assertTrue(owner(level, source) != null,
                    "active generator production must start runtime without a player query");
            helper.assertTrue(Math.abs(WorldTemperature.block(level, query) - floor) < 0.001,
                    "formed generator must supply its gameplay temperature floor");
            TownThermalProjection projection = new TownThermalProjection();
            projection.include(query);
            helper.assertTrue(Math.abs(MinecraftThermalInput.gameplayTownEnvironment(level, projection, natural)
                    - floor) < 0.001, "town queries must receive the same regional floor");
            var field = MinecraftThermalInput.gameplayAnalyticFieldsAt(level, query).get(0);
            int fuel = data.process;
            MinecraftThermalInput input = owner(level, source).input();
            helper.assertTrue(data.process == fuel, "bootstrap must not consume generator fuel");
            helper.runAfterDelay(50, () -> {
                helper.assertTrue(present(input, source), "normal generator tick must restore source");
                helper.assertTrue(MinecraftThermalInput.gameplayAnalyticFieldsAt(level, query).contains(field),
                        "unchanged reports must retain the same field definition");
                MinecraftThermalInput.invalidateGameplayProfilesForRecipeReload();
                helper.assertTrue(MinecraftThermalInput.gameplayAnalyticFieldsAt(level, query).contains(field),
                        "profile reload must preserve the world-owned field");
                helper.runAfterDelay(50, () -> {
                    helper.assertTrue(owner(level, source) != null, "generator tick must restart runtime after reload");
                    MinecraftThermalInput next = owner(level, source).input();
                    helper.assertTrue(present(next, source) && data.process == fuel,
                            "unchanged production must recover without duplicate fuel consumption");
                    data.isActive = false;
                    helper.runAfterDelay(40, () -> {
                        helper.assertTrue(!present(next, source), "inactive generator must release its source");
                        helper.assertTrue(WorldTemperature.block(level, query) >= floor - 0.001,
                                "inactive power must retain positive-level gameplay afterheat");
                        data.isActive = true;
                        helper.runAfterDelay(40, () -> {
                            helper.assertTrue(present(next, source), "generator must register after restart");
                            level.destroyBlock(master, false);
                            helper.assertTrue(MinecraftThermalInput.gameplayAnalyticFieldsAt(level, query).isEmpty(),
                                    "authoritative disassembly must immediately remove the floor");
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
            helper.assertTrue(owner(level, source) != null, "radiator tick must bootstrap without players");
            MinecraftThermalInput input = owner(level, source).input();
            helper.assertTrue(endpoint.getHeat() == before, "bootstrap must not drain extra heat");
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(present(input, source), "radiator normal tick must publish its source");
                MinecraftThermalInput.invalidateGameplayProfilesForRecipeReload();
                helper.runAfterDelay(30, () -> {
                    helper.assertTrue(owner(level, source) != null, "radiator tick must restart runtime after reload");
                    MinecraftThermalInput next = owner(level, source).input();
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
            helper.assertTrue(owner(level, source) != null, "fountain tick must bootstrap without players");
            MinecraftThermalInput input = owner(level, source).input();
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(present(input, source), "normal fountain tick must restore heat");
                MinecraftThermalInput.invalidateGameplayProfilesForRecipeReload();
                helper.runAfterDelay(40, () -> {
                    helper.assertTrue(owner(level, source) != null, "fountain tick must restart runtime after reload");
                    MinecraftThermalInput next = owner(level, source).input();
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

    static BlockPos form(GameTestHelper helper, TemplateMultiblock structure, BlockPos origin) {
        ServerLevel level = helper.getLevel();
        for (var block : structure.getStructure(level)) {
            level.setBlockAndUpdate(origin.offset(block.pos()), block.state());
        }
        helper.assertTrue(structure.createStructure(level, origin.offset(structure.getTriggerOffset()),
                Direction.SOUTH, FakePlayerFactory.getMinecraft(level)), "production multiblock must form");
        return origin.offset(structure.getMasterFromOriginOffset());
    }

    static void prepareProfileCache(MinecraftServer server) {
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

    static MinecraftThermalInput start(ServerLevel level, BlockPos center) {
        var player = FakePlayerFactory.getMinecraft(level);
        player.setPos(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5);
        long begin = System.nanoTime();
        MinecraftThermalInput.gameplayPlayerEnvironment(player, WorldTemperature.naturalAir(level, center),
                new ThermalEnvironmentSample());
        FHMain.LOGGER.info("Thermal production test startup: {} ms", (System.nanoTime() - begin) / 1_000_000.0);
        return owner(level, center).input();
    }

    static MinecraftPageManager.SectionOwner owner(ServerLevel level, BlockPos position) {
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
    static <T> T read(Object target, String name) {
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

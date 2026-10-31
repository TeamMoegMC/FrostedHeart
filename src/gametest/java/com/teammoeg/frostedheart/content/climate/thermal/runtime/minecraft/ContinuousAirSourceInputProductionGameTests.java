/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.PhysicalSourceSpatialIndex;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Actual ignition, extinction and replacement with passive build-input observation. */
@GameTestHolder("frostedheart_source_input")
@PrefixGameTestTemplate(false)
public final class ContinuousAirSourceInputProductionGameTests {
    @GameTest(template = "phase0a_empty", batch = "source_owned_lifecycle", timeoutTicks = 160)
    public static void capturedRemovalAndSectionReplacementKeepSourceOwnership(GameTestHelper helper) {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos fire = helper.absolutePos(new BlockPos(768, 2, 8));
        level.setChunkForced(fire.getX() >> 4, fire.getZ() >> 4, true);
        var chunk = level.getChunkAt(fire);
        level.setBlockAndUpdate(fire.below(), Blocks.STONE.defaultBlockState());
        var lit = Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
        level.setBlockAndUpdate(fire, lit);
        ((ICampfireExtra) level.getBlockEntity(fire)).setLifeTime(20_000);
        var input = ThermalLoadedWorldGameTests.owner(level, fire).input();
        PhysicalSourceSpatialIndex sources = ThermalLoadedWorldGameTests.read(input, "physicalSources");
        int snapshots = level.capturedBlockSnapshots.size();
        try {
            level.captureBlockSnapshots = true;
            level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
            helper.assertTrue(sourcePresent(sources, fire), "a tentative removal must not prematurely retire its owned source");
            level.captureBlockSnapshots = false;
            helper.assertTrue(level.capturedBlockSnapshots.get(snapshots).restore(true, false), "real snapshot rollback must succeed");
            level.capturedBlockSnapshots.subList(snapshots, level.capturedBlockSnapshots.size()).clear();
            sources.reconcileCampfireChanges();
            helper.assertTrue(sourcePresent(sources, fire) && chunk.getBlockState(fire).getValue(CampfireBlock.LIT),
                    "rollback must retain the burning source");
            level.captureBlockSnapshots = true;
            level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
        } finally {
            level.captureBlockSnapshots = false;
            level.capturedBlockSnapshots.subList(snapshots, level.capturedBlockSnapshots.size()).clear();
        }
        helper.startSequence().thenIdle(2).thenExecute(() -> {
            helper.assertTrue(!sourcePresent(sources, fire), "confirmed removal without a campfire onPlace must retire at the real level tick");
            level.setBlockAndUpdate(fire, lit);
            var old = (net.minecraft.world.level.block.entity.CampfireBlockEntity) level.getBlockEntity(fire);
            level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(fire, lit);
            ((ICampfireExtra) level.getBlockEntity(fire)).setLifeTime(20_000);
            old.onLoad();
            helper.assertTrue(sourcePresent(sources, fire), "a late old BE callback must not remove the replacement's source");
            var raw = chunk.getSection(chunk.getSectionIndex(fire.getY()));
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                raw.setBlockState(fire.getX() & 15, fire.getY() & 15, fire.getZ() & 15, Blocks.AIR.defaultBlockState());
                MinecraftThermalInput.onRawBlockContainerReplaced(raw);
            }).join();
        }).thenIdle(2).thenExecute(() -> {
            helper.assertTrue(!sourcePresent(sources, fire), "background container replacement must hand source/cache invalidation back to the level thread");
            level.setBlockAndUpdate(fire, lit);
            ((ICampfireExtra) level.getBlockEntity(fire)).setLifeTime(20_000);
            int sectionIndex = chunk.getSectionIndex(fire.getY());
            var previous = chunk.getSections()[sectionIndex];
            var replacement = new net.minecraft.world.level.chunk.LevelChunkSection(level.registryAccess().registryOrThrow(
                    net.minecraft.core.registries.Registries.BIOME));
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++)
                replacement.setBlockState(x, y, z, previous.getBlockState(x, y, z));
            replacement.setBlockState(fire.getX() & 15, fire.getY() & 15, fire.getZ() & 15, Blocks.AIR.defaultBlockState());
            chunk.getSections()[sectionIndex] = replacement;
            MinecraftThermalInput.onSectionIdentityReplaced(level, chunk, sectionIndex, previous);
        }).thenIdle(2).thenExecute(() -> {
            helper.assertTrue(!sourcePresent(sources, fire), "identity replacement must recheck existing sources without scanning the Section");
            level.removeBlockEntity(fire);
            MinecraftThermalInput.closeActiveLevel(level);
            level.setChunkForced(fire.getX() >> 4, fire.getZ() >> 4, false);
        }).thenSucceed();
    }

    private static boolean sourcePresent(PhysicalSourceSpatialIndex sources, BlockPos position) {
        it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap slots = ThermalLoadedWorldGameTests.read(sources, "slotsById");
        byte[] flags = ThermalLoadedWorldGameTests.read(sources, "sourceFlags");
        int slot = slots.get(position.asLong());
        return slot >= 0 && (flags[slot] & 1) != 0;
    }

    @GameTest(template = "phase0a_empty", batch = "source_confirmed_placement", timeoutTicks = 120)
    public static void capturedPlacementWaitsForCampfireLifecycleConfirmation(GameTestHelper helper) {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos fire = helper.absolutePos(new BlockPos(640, 2, 8));
        level.setChunkForced(fire.getX() >> 4, fire.getZ() >> 4, true);
        var chunk = level.getChunkAt(fire);
        level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(fire.below(), Blocks.STONE.defaultBlockState());
        var state = Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
        boolean previousCapture = level.captureBlockSnapshots;
        int previousSnapshots = level.capturedBlockSnapshots.size();
        java.util.Map<net.minecraft.server.level.ServerLevel, ?> active =
                ThermalLoadedWorldGameTests.read(MinecraftThermalInput.class, "ACTIVE");
        try {
            level.captureBlockSnapshots = true;
            level.setBlockAndUpdate(fire, state);
            helper.assertTrue(!active.containsKey(level),
                    "a captured write must not start a heat source through the public chunk callback");
            level.captureBlockSnapshots = false;
            helper.assertTrue(level.capturedBlockSnapshots.get(previousSnapshots).restore(true, false),
                    "the actual captured placement must be reversible");
            helper.assertTrue(!active.containsKey(level), "rolled back placement must not leave a runtime or phantom source");
            helper.assertTrue(chunk.getBlockState(fire).isAir(),
                    "rollback must restore the fixture's original Air, actual=" + chunk.getBlockState(fire));
            level.capturedBlockSnapshots.subList(previousSnapshots, level.capturedBlockSnapshots.size()).clear();
            level.captureBlockSnapshots = true;
            level.setBlockAndUpdate(fire, state);
            helper.assertTrue(!active.containsKey(level), "a second captured write must still wait for confirmation");
            level.captureBlockSnapshots = false;
            // Forge confirms a captured placement by invoking its actual onPlace lifecycle.
            state.onPlace(level, fire, Blocks.AIR.defaultBlockState(), false);
            helper.assertTrue(active.containsKey(level), "campfire's own confirmed placement must start its runtime");
            PhysicalSourceSpatialIndex sources = ThermalLoadedWorldGameTests.read(active.get(level), "physicalSources");
            it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap slots = ThermalLoadedWorldGameTests.read(sources, "slotsById");
            helper.assertTrue(slots.containsKey(fire.asLong()), "confirmed placement must register the actual source");
        } finally {
            level.captureBlockSnapshots = previousCapture;
            level.capturedBlockSnapshots.subList(previousSnapshots, level.capturedBlockSnapshots.size()).clear();
            level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
            MinecraftThermalInput.closeActiveLevel(level);
            level.setChunkForced(fire.getX() >> 4, fire.getZ() >> 4, false);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "source_direct_ignition", timeoutTicks = 900)
    public static void litPlacementPublishesWithoutReceiverAdmissionOrChunkReload(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos fire = helper.absolutePos(new BlockPos(128, 2, 8));
        level.setChunkForced(fire.getX() >> 4, fire.getZ() >> 4, true);
        level.getChunkAt(fire);
        var sample = new ThermalEnvironmentSample();
        helper.startSequence().thenIdle(60).thenExecute(() -> {
            for (int y = -1; y <= 7; y++) for (int z = -8; z <= 8; z++) for (int x = -8; x <= 8; x++) {
                boolean wall = y == -1 || y == 7 || Math.abs(x) == 8 || Math.abs(z) == 8;
                level.setBlockAndUpdate(fire.offset(x, y, z), (wall ? Blocks.STONE : Blocks.AIR).defaultBlockState());
            }
            level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
            var fuel = level.getBlockEntity(fire).getCapability(ForgeCapabilities.ITEM_HANDLER)
                    .orElseThrow(() -> new IllegalStateException("New campfire lost its actual fuel capability"));
            helper.assertTrue(fuel.insertItem(0, new ItemStack(Items.COAL, 4), false).isEmpty(),
                    "The new real campfire accepts ordinary fuel");
        }).thenIdle(40).thenWaitUntil(() -> {
            double natural = WorldTemperature.naturalAir(level, fire.above());
            MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, fire.above(), natural, sample);
            double far = MinecraftThermalInput.gameplayPassiveEnvironment(level, fire.above().east(4),
                    WorldTemperature.naturalAir(level, fire.above().east(4)));
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample)
                            && MinecraftThermalInput.enabledPhysicalSourceCount(level) > 0
                            && sample.airTemperatureC() - far > 0.05
                            && MinecraftThermalInput.describeAirRead(level, fire.above()).startsWith("geometry_current"),
                    "Lit placement must publish current Air and the existing same-height heating contrast; near="
                            + sample.airTemperatureC() + ",far=" + far + ",tick=" + sample.airSampleTick()
                            + ",read=" + MinecraftThermalInput.describeAirRead(level, fire.above()));
        }).thenExecute(() -> {
            FHMain.LOGGER.info("THERMAL_DIRECT_IGNITION_PRODUCTION,sampleTick={},airC={},enabledSources={}",
                    sample.airSampleTick(), sample.airTemperatureC(), MinecraftThermalInput.enabledPhysicalSourceCount(level));
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
            level.setChunkForced(fire.getX() >> 4, fire.getZ() >> 4, false);
        }).thenSucceed();
    }

    @GameTest(template = "phase0a_empty", batch = "source_pending_chunk", timeoutTicks = 120)
    public static void pendingCampfireIsDiscoveredWithoutInstantiatingItsEntity(GameTestHelper helper) {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos fire = helper.absolutePos(new BlockPos(256, 2, 8));
        level.setChunkForced(fire.getX() >> 4, fire.getZ() >> 4, true);
        var chunk = level.getChunkAt(fire);
        helper.assertTrue(chunk.getBlockEntitiesPos().isEmpty(), "fixture chunk must start without block entities");
        level.setBlockAndUpdate(fire.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        ((ICampfireExtra) level.getBlockEntity(fire)).setLifeTime(20_000);
        helper.assertTrue(chunk.getBlockEntities().containsKey(fire), "fixture must contain the live campfire");
        var stored = chunk.getBlockEntityNbtForSaving(fire);
        level.removeBlockEntity(fire);
        helper.assertTrue(chunk.getBlockEntitiesPos().isEmpty(), "removed entity must leave no positions");
        chunk.setBlockEntityNbt(stored);
        helper.assertTrue(chunk.getBlockEntitiesPos().contains(fire), "packed NBT alone must remain discoverable");
        MinecraftThermalInput.closeActiveLevel(level);
        MinecraftThermalInput.onChunkLoad(level, chunk);
        var owner = ThermalLoadedWorldGameTests.owner(level, fire);
        helper.assertTrue(owner != null, "loaded pending campfire must bootstrap without player queries");
        var input = owner.input();
        PhysicalSourceSpatialIndex sources = ThermalLoadedWorldGameTests.read(input, "physicalSources");
        var cursor = new PhysicalSourceSpatialIndex.DiscoveryCursor(chunk);
        helper.assertTrue(sources.discoverChunk(cursor, 32) == 1 && cursor.complete(),
                "bounded discovery must restore the sole packed position");
        it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap slots = ThermalLoadedWorldGameTests.read(sources, "slotsById");
        helper.assertTrue(slots.containsKey(fire.asLong()), "pending NBT position must restore the lit source");
        helper.assertTrue(!chunk.getBlockEntities().containsKey(fire),
                "discovery must not instantiate the pending block entity");
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(slots.containsKey(fire.asLong()), "discovered source must survive normal ticking");
            level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
            MinecraftThermalInput.closeActiveLevel(level);
            level.setChunkForced(fire.getX() >> 4, fire.getZ() >> 4, false);
            helper.succeed();
        });
    }

    @GameTest(template = "phase0a_empty", batch = "source_capacity_cursor", timeoutTicks = 300)
    public static void sourceAddedAfterSnapshotRecoversWhenCapacityReturns(GameTestHelper helper) {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos first = helper.absolutePos(new BlockPos(384, 2, 8));
        BlockPos second = first.east(16);
        level.setChunkForced(first.getX() >> 4, first.getZ() >> 4, true);
        level.setChunkForced(second.getX() >> 4, second.getZ() >> 4, true);
        level.getChunkAt(second);
        for (BlockPos position : new BlockPos[]{first, second}) {
            level.setBlockAndUpdate(position.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(position.above(), Blocks.AIR.defaultBlockState());
        }
        level.setBlockAndUpdate(first, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        ((ICampfireExtra) level.getBlockEntity(first)).setLifeTime(20_000);
        var input = ThermalLoadedWorldGameTests.owner(level, first).input();
        PhysicalSourceSpatialIndex sources = ThermalLoadedWorldGameTests.read(input, "physicalSources");
        int highWater = ThermalLoadedWorldGameTests.read(sources, "highWaterMark");
        ThermalLoadedWorldGameTests.write(sources, "maximumSources", highWater);
        var chunk = level.getChunkAt(second);
        helper.assertTrue(chunk.getBlockEntitiesPos().isEmpty(), "the refused source's chunk must have no BE before onPlace");
        long key = chunk.getPos().toLong();
        it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<PhysicalSourceSpatialIndex.DiscoveryCursor> cursors =
                ThermalLoadedWorldGameTests.read(input, "sourceDiscoveryCursors");
        cursors.put(key, new PhysicalSourceSpatialIndex.DiscoveryCursor(chunk));
        level.setBlockAndUpdate(second, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        ((ICampfireExtra) level.getBlockEntity(second)).setLifeTime(20_000);
        helper.assertTrue(!cursors.containsKey(key), "capacity refusal must discard the snapshot made before the new BE");
        it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap slots = ThermalLoadedWorldGameTests.read(sources, "slotsById");
        helper.assertTrue(!slots.containsKey(second.asLong()), "the new source must actually encounter full capacity");
        level.setBlockAndUpdate(first, Blocks.AIR.defaultBlockState());
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(slots.containsKey(second.asLong()),
                "queued discovery must find the new source after the old slot retires"))
                .thenExecute(() -> {
                    level.setBlockAndUpdate(second, Blocks.AIR.defaultBlockState());
                    MinecraftThermalInput.closeActiveLevel(level);
                    level.setChunkForced(first.getX() >> 4, first.getZ() >> 4, false);
                    level.setChunkForced(second.getX() >> 4, second.getZ() >> 4, false);
                }).thenSucceed();
    }

    @GameTest(template = "phase0a_empty", batch = "source_startup_retry", timeoutTicks = 300)
    public static void queuedStartupResumesWithoutAnotherIgnition(GameTestHelper helper) throws ReflectiveOperationException {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos fire = helper.absolutePos(new BlockPos(512, 2, 8));
        level.setChunkForced(fire.getX() >> 4, fire.getZ() >> 4, true);
        level.setBlockAndUpdate(fire.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(fire.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        ((ICampfireExtra) level.getBlockEntity(fire)).setLifeTime(20_000);
        MinecraftThermalInput.closeActiveLevel(level);
        ThermalMemoryBudget memory = ThermalLoadedWorldGameTests.read(MinecraftThermalInput.class, "MEMORY");
        var pressure = memory.tryReserve(64);
        helper.assertTrue(pressure != null, "fixture needs a real reservation whose release changes capacity");
        try {
            // A soft target does not necessarily refuse bounded startup work.
            // Establish the retained-failure precondition, then exercise the real
            // level tick, budget release, discovery and worker admission paths.
            var retryType = java.util.Arrays.stream(MinecraftThermalInput.class.getDeclaredClasses())
                    .filter(type -> type.getSimpleName().equals("StartupRetry")).findFirst().orElseThrow();
            var constructor = retryType.getDeclaredConstructor(long.class, long.class);
            constructor.setAccessible(true);
            java.util.Map<net.minecraft.server.level.ServerLevel, Object> retries =
                    ThermalLoadedWorldGameTests.read(MinecraftThermalInput.class, "startupRetries");
            retries.put(level, constructor.newInstance(memory.capacityRevision(), fire.asLong()));
        } catch (ReflectiveOperationException | RuntimeException | Error failure) {
            pressure.close();
            level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
            MinecraftThermalInput.closeActiveLevel(level);
            level.setChunkForced(fire.getX() >> 4, fire.getZ() >> 4, false);
            throw failure;
        }
        helper.startSequence().thenIdle(3).thenExecute(() -> {
            try {
                helper.assertTrue(ThermalLoadedWorldGameTests.owner(level, fire) == null,
                        "unchanged capacity must keep startup waiting");
            } finally { pressure.close(); }
        }).thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.enabledPhysicalSourceCount(level) > 0,
                "capacity release alone must start and offer the still-lit source"))
                .thenExecute(() -> {
                    level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
                    MinecraftThermalInput.closeActiveLevel(level);
                    level.setChunkForced(fire.getX() >> 4, fire.getZ() >> 4, false);
                }).thenSucceed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 2200)
    public static void sourceInputsSurviveRealExtinctionAndReplacement(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos fire = ContinuousAirProductionGameTests.room(helper, helper.absolutePos(BlockPos.ZERO));
        SourceInputProductionObservation.begin(fire);
        ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
        var sample = new ThermalEnvironmentSample();
        helper.startSequence().thenIdle(180).thenWaitUntil(() -> {
            MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, fire.above(),
                    WorldTemperature.naturalAir(level, fire.above()), sample);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample)
                            && SourceInputProductionObservation.freezes() > 0,
                    "Actual source discovery must build the detailed field and its immutable source input");
        }).thenExecute(() -> {
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
            helper.assertTrue(!level.getBlockState(fire).getValue(CampfireBlock.LIT), "The actual shovel interaction extinguishes the fire");
        }).thenWaitUntil(() -> {
            helper.assertTrue(!SourceInputProductionObservation.emitting(),
                    "Real extinction reaches the worker source registry: " + SourceInputProductionObservation.summary());
        })
        .thenIdle(80).thenExecute(() -> ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL))
        .thenWaitUntil(() -> helper.assertTrue(SourceInputProductionObservation.emitting(),
                "Real relighting reaches the worker source registry"))
        .thenIdle(80).thenExecute(() -> level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState()))
        .thenWaitUntil(() -> helper.assertTrue(!SourceInputProductionObservation.emitting(),
                "Breaking the actual block retires its emitting lifecycle"))
        .thenIdle(60).thenExecute(() -> {
            level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
            level.getBlockEntity(fire).getCapability(ForgeCapabilities.ITEM_HANDLER)
                    .orElseThrow(() -> new IllegalStateException("Replacement campfire lost its actual fuel capability"))
                    .insertItem(0, new ItemStack(Items.COAL, 4), false);
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
        }).thenWaitUntil(() -> {
            MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, fire.above(),
                    WorldTemperature.naturalAir(level, fire.above()), sample);
            helper.assertTrue(SourceInputProductionObservation.emitting()
                            && ContinuousAirProductionTiming.sourceField(sample)
                            && MinecraftThermalInput.describeAirRead(level, fire.above()).startsWith("geometry_current"),
                    "The real replacement must publish its current detailed Air field");
        }).thenExecute(() -> {
            helper.assertTrue(SourceInputProductionObservation.failure() == null,
                    "Frozen source inputs and commit receipts: " + SourceInputProductionObservation.summary());
            helper.assertTrue(SourceInputProductionObservation.changedChecks() >= 4
                            && SourceInputProductionObservation.commits() > 0,
                    "Retained actual build inputs must be checked across extinction, relighting, unload and replacement");
            FHMain.LOGGER.info("THERMAL_SOURCE_INPUT_PRODUCTION,{}", SourceInputProductionObservation.summary());
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
            SourceInputProductionObservation.end();
        }).thenSucceed();
    }
}

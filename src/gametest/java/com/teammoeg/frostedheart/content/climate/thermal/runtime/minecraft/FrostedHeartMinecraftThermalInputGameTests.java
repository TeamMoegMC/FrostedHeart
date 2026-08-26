/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.mojang.authlib.GameProfile;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.data.PlantTempData;
import com.teammoeg.frostedheart.content.climate.thermal.consumer.TownThermalProjection;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.profile.DependencyOffsetMask;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolverBlockView;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalResolution;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureResolver;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.StateStaticThermalResolver;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.ThermalSignatureResolverDispatcher;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.RadiationService;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.DimensionThermalRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.solver.InputWatermarks;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweep;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweepFragments;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalTimePolicy;
import com.teammoeg.frostedheart.content.climate.thermal.source.NodePowerAccumulatorArena;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceBinding;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceTimeline;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder(FHMain.MODID)
@PrefixGameTestTemplate(false)
public final class FrostedHeartMinecraftThermalInputGameTests {
    private static final String BATCH = "frostedheart_minecraft_thermal_input";
    private static final String MUTATION_BATCH = BATCH + "_mutation";
    private static final String NEIGHBOR_BATCH = BATCH + "_neighbor";
    private static final String SOURCE_BATCH = BATCH + "_source";
    private static final String SINK_BATCH = BATCH + "_sink";
    private static final String TEMPLATE = "phase0a_empty";
    private static final ResolvedThermalSignature SOLID_SIGNATURE =
            new ResolvedThermalSignature(0, 0, List.of(), 0, 0, 0, 0, 0);

    private FrostedHeartMinecraftThermalInputGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 40)
    public static void staticStateChangesReuseEquivalentThermalSignature(
            GameTestHelper helper
    ) {
        MinecraftThermalInput.prepareGameplayProfiles();
        BlockState unlit = Blocks.CAMPFIRE.defaultBlockState()
                .setValue(CampfireBlock.LIT, false);
        BlockState lit = unlit.setValue(CampfireBlock.LIT, true);
        BlockState closedDoor = Blocks.OAK_DOOR.defaultBlockState();
        BlockState openDoor = closedDoor.setValue(DoorBlock.OPEN, true);
        BlockState lowerStairs = Blocks.OAK_STAIRS.defaultBlockState();
        BlockState upperStairs = lowerStairs.setValue(BlockStateProperties.HALF, Half.TOP);

        helper.assertTrue(
                MinecraftThermalInput.staticMutationSemanticsUnchanged(unlit, lit),
                "Campfire lit state must not invalidate unchanged geometry");
        helper.assertTrue(
                !MinecraftThermalInput.staticMutationSemanticsUnchanged(closedDoor, openDoor),
                "Door open state must invalidate changed geometry");
        helper.assertTrue(
                !MinecraftThermalInput.staticMutationSemanticsUnchanged(lowerStairs, upperStairs),
                "Stair half state must invalidate changed geometry");
        helper.assertTrue(
                !MinecraftThermalInput.staticMutationSemanticsUnchanged(
                        Blocks.AIR.defaultBlockState(), Blocks.STONE.defaultBlockState()),
                "air-to-solid mutation must remain a thermal geometry change");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 40)
    public static void gameplayPlayerQueryBootstrapsAndPublishesCapturedPage(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 3, 2));
        LevelChunk chunk = level.getChunkAt(anchor);
        int sectionIndex = allAirSectionIndex(chunk);
        int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
        double playerY = SectionPos.sectionToBlockCoord(sectionY) + 2.0D;
        ServerPlayer player = new ServerPlayer(
                level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "thermal-gametest"));
        player.setPos(anchor.getX() + 0.5D, playerY, anchor.getZ() + 0.5D);
        MinecraftThermalInput.MutableEnvironmentSample sample =
                new MinecraftThermalInput.MutableEnvironmentSample();
        double fallbackTemperatureC = 7.0D;

        try {
            double bootstrap = MinecraftThermalInput.gameplayPlayerEnvironment(
                    player, fallbackTemperatureC, sample);
            helper.assertTrue(close(bootstrap, fallbackTemperatureC)
                            && !sample.airAvailable(),
                    "the first production query must use fallback while admitting its Page");

            MinecraftThermalInput.sealActiveLevel(level);
            double published = MinecraftThermalInput.gameplayPlayerEnvironment(
                    player, fallbackTemperatureC, sample);
            helper.assertTrue(sample.airAvailable()
                            && Double.isFinite(published)
                            && (sample.flags() & MinecraftThermalInput.QUERY_NO_PAGE) == 0,
                    "the production tick path must publish the captured player Page");
        } finally {
            MinecraftThermalInput.closeAll();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 40)
    public static void capturedMixedPagePublishesThroughGameplayDispatch(
        GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 3, 2));
        LevelChunk chunk = level.getChunkAt(anchor);
        int sectionIndex = allAirSectionIndex(chunk);
        LevelChunkSection section = chunk.getSections()[sectionIndex];
        int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
        BlockPos position = new BlockPos(
                anchor.getX(), SectionPos.sectionToBlockCoord(sectionY) + 2, anchor.getZ());
        setDirect(section, position, Blocks.STONE.defaultBlockState());

        DimensionThermalRuntime runtime = runtime(level.getGameTime());
        ThermalSignatureRegistry signatures = signatureRegistry();
        ThermalSignatureResolverDispatcher dispatcher =
                ThermalSignatureResolverDispatcher.builder(
                        StateStaticThermalResolver.geometryOnly(
                                ConservativeAirGeometry.MICROCELL_COUNT))
                        .build();
        try (MinecraftThermalInput input = new MinecraftThermalInput(
                level,
                9L,
                runtime,
                dispatcher,
                signatures,
                1L,
                 16,
                 16)) {
            input.enableTopologyApplication(topologyParameters());
            AtomicInteger dispatchSubmissions = new AtomicInteger();
            input.enableDispatch(command -> {
                dispatchSubmissions.incrementAndGet();
                command.run();
            });
            helper.assertTrue(input.retainPhysicalSourcePage(position, 4),
                    "the real source-interest path must admit the target Page");
            MinecraftThermalInput.sealActiveLevel(level);
            helper.assertTrue(runtime.appliedWatermarks().geometry() == 0L,
                    "captured admission must not fabricate a mutation watermark");
            helper.assertTrue(runtime.appliedWatermarks().chunk() > 0L,
                    "the admitted Page cut must reach the runtime through gameplay sealing");
            helper.assertTrue(runtime.appliedWatermarks().profile() == 1L,
                    "the frame must carry the frozen profile-table cut");
            helper.assertTrue(dispatchSubmissions.get() == 1,
                    "the configured dispatch executor must receive the sealed frame");
            helper.assertTrue(runtime.lastCompletedTargetTick() == level.getGameTime(),
                    "dispatch must run the admitted epoch on the coordinator");

            MinecraftThermalInput.MutableEnvironmentSample playerSample =
                    new MinecraftThermalInput.MutableEnvironmentSample();
            double sampleX = position.getX() + 0.5D;
            double sampleZ = position.getZ() + 0.5D;
            input.samplePlayerEnvironment(
                    101L, 1,
                    sampleX, position.getY(), position.getY() + 1.5D, sampleZ,
                    level.getGameTime(), 40, playerSample);
            helper.assertTrue(playerSample.airAvailable()
                            && close(playerSample.airTemperatureC(), 0.0D),
                    "the player query must resolve the exact air component in a mixed Brick");
            helper.assertTrue(playerSample.sampleTick() == level.getGameTime(),
                    "the player query must preserve the publication sample tick");

            input.samplePlayerEnvironment(
                    101L, 1,
                    sampleX, position.getY(), position.getY() + 0.5D, sampleZ,
                    level.getGameTime(), 40, playerSample);
            helper.assertTrue(!playerSample.airAvailable()
                            && (playerSample.flags()
                            & MinecraftThermalInput.QUERY_NO_AIR_COMPONENT) != 0,
                    "a solid mixed-Brick microcell must not alias its support cell");

            int highWaterBeforeMiss = runtime.thermalCellArena().highWaterMark();
            int liveCellsBeforeMiss = runtime.thermalCellArena().liveCellCount();
            input.samplePlayerEnvironment(
                    101L, 1,
                    sampleX + 32.0D, position.getY(), position.getY() + 1.5D, sampleZ,
                    level.getGameTime(), 40, playerSample);
            helper.assertTrue(!playerSample.airAvailable()
                            && (playerSample.flags()
                            & MinecraftThermalInput.QUERY_NO_PAGE) != 0
                            && runtime.thermalCellArena().highWaterMark() == highWaterBeforeMiss
                            && runtime.thermalCellArena().liveCellCount() == liveCellsBeforeMiss,
                    "a passive player miss must not admit or load a Page");

            input.samplePlayerEnvironment(
                    101L, 1,
                    sampleX, position.getY(), position.getY() + 1.5D, sampleZ,
                    level.getGameTime() + 41L, 40, playerSample);
            helper.assertTrue(!playerSample.airAvailable()
                            && (playerSample.flags()
                            & MinecraftThermalInput.QUERY_PUBLICATION_STALE) != 0,
                    "an over-age publication must use explicit fallback");

            double enthalpyBeforePassiveQueries = totalEnthalpy(
                    runtime.thermalCellArena());
            InputWatermarks watermarksBeforePassiveQueries = runtime.appliedWatermarks();
            long completedTickBeforePassiveQueries = runtime.lastCompletedTargetTick();
            BlockPos cropPosition = position.above();
            int arenaHighWaterBeforeCropQueries =
                    runtime.thermalCellArena().highWaterMark();
            int liveCellsBeforeCropQueries =
                    runtime.thermalCellArena().liveCellCount();
            MinecraftThermalInput.MutableEnvironmentSample cropSample =
                    new MinecraftThermalInput.MutableEnvironmentSample();
            input.sampleCropEnvironment(
                    cropPosition.getX(), cropPosition.getY(), cropPosition.getZ(),
                    level.getGameTime(), 40, cropSample);
            helper.assertTrue(cropSample.airAvailable()
                            && close(cropSample.airTemperatureC(), 0.0D)
                            && cropSample.radiantFluxWPerM2() == 0.0D,
                    "a passive crop must read an existing air publication only");

            WorldTemperature.PlantStatus cropStatus =
                    WorldTemperature.checkPlantStatus(
                            level,
                            cropPosition,
                            new PlantTempData(
                                    Blocks.WHEAT,
                                    3.0F,
                                    10.0F,
                                    4.0F,
                                    -10.0F,
                                    40.0F,
                                    40.0F,
                                    40.0F,
                                    false,
                                    false,
                                    Blocks.DEAD_BUSH,
                                    true,
                                    1,
                                    1,
                                    15),
                            5.0F);
            helper.assertTrue(cropStatus == WorldTemperature.PlantStatus.CAN_SURVIVE,
                    "the real plant-status path must use published 0 C, not legacy 5 C");

            TownThermalProjection townProjection = new TownThermalProjection();
            townProjection.include(cropPosition);
            townProjection.include((cropPosition.getX() & 3) == 3
                    ? cropPosition.west() : cropPosition.east());
            double gameplayTownTemperatureC =
                    MinecraftThermalInput.gameplayTownEnvironment(
                            level, townProjection, 5.0D);
            helper.assertTrue(close(gameplayTownTemperatureC, 0.0D),
                    "a complete town projection must use one published weighted Brick value");

            TownThermalProjection missingTownProjection =
                    new TownThermalProjection();
            for (int index = 0; index < 4_096; index++) {
                missingTownProjection.include(new BlockPos(
                        cropPosition.getX() + 32 + (index & 63) * 4,
                        cropPosition.getY(),
                        cropPosition.getZ() + (index >>> 6) * 4));
            }
            MinecraftThermalInput.MutableEnvironmentSample townSample =
                    new MinecraftThermalInput.MutableEnvironmentSample();
            input.sampleTownEnvironment(
                    missingTownProjection, level.getGameTime(), 40, townSample);
            helper.assertTrue(!townSample.airAvailable()
                            && (townSample.flags()
                            & MinecraftThermalInput.QUERY_NO_PAGE) != 0
                            && runtime.thermalCellArena().highWaterMark()
                            == arenaHighWaterBeforeCropQueries
                            && runtime.thermalCellArena().liveCellCount()
                            == liveCellsBeforeCropQueries
                            && close(totalEnthalpy(runtime.thermalCellArena()),
                            enthalpyBeforePassiveQueries),
                    "4,096 passive town groups must not create or retain thermal state");

            for (int index = 0; index < 10_000; index++) {
                input.sampleCropEnvironment(
                        cropPosition.getX() + 32 + (index & 255) * 16,
                        cropPosition.getY(),
                        cropPosition.getZ() + (index >>> 8) * 16,
                        level.getGameTime(), 40, cropSample);
            }
            helper.assertTrue(!cropSample.airAvailable()
                            && (cropSample.flags()
                            & MinecraftThermalInput.QUERY_NO_PAGE) != 0
                            && runtime.thermalCellArena().highWaterMark()
                            == arenaHighWaterBeforeCropQueries
                            && runtime.thermalCellArena().liveCellCount()
                            == liveCellsBeforeCropQueries
                            && close(totalEnthalpy(runtime.thermalCellArena()),
                            enthalpyBeforePassiveQueries)
                            && runtime.appliedWatermarks().equals(
                            watermarksBeforePassiveQueries)
                            && runtime.lastCompletedTargetTick()
                            == completedTickBeforePassiveQueries,
                    "10,000 passive crop misses must not retain thermal state");

            MinecraftThermalInput.onRawBlockContainerReplaced(section);
            MinecraftThermalInput.sealActiveLevel(level);
            helper.assertTrue(dispatchSubmissions.get() == 2,
                    "the full Page resnapshot must rebuild through dispatch");

            long geometryBeforeDetach = runtime.appliedWatermarks().geometry();
            MinecraftThermalInput.onChunkUnload(level, chunk);
            setDirect(section, position, Blocks.AIR.defaultBlockState());
            MinecraftThermalInput.sealActiveLevel(level);
            helper.assertTrue(runtime.appliedWatermarks().geometry() == geometryBeforeDetach,
                    "a detached section mutation must not enter the gameplay geometry stream");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = MUTATION_BATCH, timeoutTicks = 40)
    public static void gameplayMutationWaitsForBatchDeadline(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 3, 2));
        LevelChunk chunk = level.getChunkAt(anchor);
        int sectionIndex = allAirSectionIndex(chunk);
        LevelChunkSection section = chunk.getSections()[sectionIndex];
        int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
        BlockPos position = new BlockPos(
                anchor.getX(), SectionPos.sectionToBlockCoord(sectionY) + 2, anchor.getZ());
        long initialTick = level.getGameTime();
        DimensionThermalRuntime runtime = runtime(initialTick);
        MinecraftThermalInput input = new MinecraftThermalInput(
                level,
                9L,
                runtime,
                ThermalSignatureResolverDispatcher.builder(
                        StateStaticThermalResolver.geometryOnly(
                                ConservativeAirGeometry.MICROCELL_COUNT))
                        .build(),
                signatureRegistry(),
                1L,
                16,
                16);
        try {
            input.enableTopologyApplication(topologyParameters());
            input.enableDispatch(Runnable::run);
            helper.assertTrue(input.retainPhysicalSourcePage(position, 4),
                    "the real source-interest path must admit the target Page");
            MinecraftThermalInput.sealActiveLevel(level);

            int localX = SectionPos.sectionRelative(position.getX());
            int localY = SectionPos.sectionRelative(position.getY());
            int localZ = SectionPos.sectionRelative(position.getZ());
            section.setBlockState(
                    localX, localY, localZ, Blocks.STONE.defaultBlockState(), false);
            MinecraftThermalInput.sealActiveLevel(level);

            helper.assertTrue(runtime.appliedWatermarks().geometry() == 0L,
                    "a pre-deadline source/solve cut must not consume pending geometry");
        } catch (RuntimeException | Error exception) {
            input.close();
            throw exception;
        }

        helper.runAfterDelay(7L, () -> {
            try {
                long appliedGeometry = runtime.appliedWatermarks().geometry();
                helper.assertTrue(appliedGeometry == 1L,
                        "the gameplay level tick must release the Brick at its deadline; got "
                                + appliedGeometry);

                Thread deferredMutation = new Thread(() -> section.setBlockState(
                        8,
                        8,
                        8,
                        Blocks.STONE.defaultBlockState(),
                        false), "thermal-gametest-deferred-mutation");
                deferredMutation.start();
                try {
                    deferredMutation.join();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(
                            "interrupted while joining thermal mutation", exception);
                }
                MinecraftThermalInput.sealActiveLevel(level);
                helper.assertTrue(runtime.appliedWatermarks().geometry() == 1L,
                        "thread-deferred geometry must use the same batching deadline");
            } catch (RuntimeException | Error exception) {
                input.close();
                throw exception;
            }

            helper.runAfterDelay(7L, () -> {
                try {
                    helper.assertTrue(runtime.appliedWatermarks().geometry() == 2L,
                            "the deferred mutation must reach the production geometry authority");
                } finally {
                    input.close();
                }
                helper.succeed();
            });
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 40)
    public static void analyticFieldsComposeAfterMeshWithoutCreatingPages(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 3, 2));
        LevelChunk chunk = level.getChunkAt(anchor);
        int sectionIndex = allAirSectionIndex(chunk);
        int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
        BlockPos receiver = new BlockPos(
                anchor.getX(), SectionPos.sectionToBlockCoord(sectionY) + 4, anchor.getZ());
        long initialTick = level.getGameTime();

        DimensionThermalRuntime runtime = runtime(initialTick);
        try (MinecraftThermalInput input = new MinecraftThermalInput(
                level,
                9L,
                runtime,
                ThermalSignatureResolverDispatcher.builder(
                        StateStaticThermalResolver.geometryOnly(
                                ConservativeAirGeometry.MICROCELL_COUNT))
                        .build(),
                signatureRegistry(),
                1L,
                16,
                16)) {
            input.enableTopologyApplication(topologyParameters());
            input.enableDispatch(Runnable::run);
            input.upsertAnalyticField(new MinecraftThermalInput.AnalyticField(
                    1L, 0,
                    MinecraftThermalInput.AnalyticCombineMode.OVERRIDE,
                    receiver.getX() + 0.5D,
                    receiver.getY() + 0.5D,
                    receiver.getZ() + 0.5D,
                    4.0D,
                    5.0D));
            input.upsertAnalyticField(new MinecraftThermalInput.AnalyticField(
                    2L, 0,
                    MinecraftThermalInput.AnalyticCombineMode.MAX_HEAT,
                    receiver.getX() + 0.5D,
                    receiver.getY() + 0.5D,
                    receiver.getZ() + 0.5D,
                    4.0D,
                    10.0D));
            input.upsertAnalyticField(new MinecraftThermalInput.AnalyticField(
                    3L, 0,
                    MinecraftThermalInput.AnalyticCombineMode.MIN_COOL,
                    receiver.getX() + 0.5D,
                    receiver.getY() + 0.5D,
                    receiver.getZ() + 0.5D,
                    4.0D,
                    -5.0D));
            input.upsertAnalyticField(new MinecraftThermalInput.AnalyticField(
                    4L, 0,
                    MinecraftThermalInput.AnalyticCombineMode.ADD_DELTA,
                    receiver.getX() + 0.5D,
                    receiver.getY() + 0.5D,
                    receiver.getZ() + 0.5D,
                    4.0D,
                    2.0D));
            helper.assertTrue(runtime.thermalCellArena().liveCellCount() == 0,
                    "analytic fields must not create sparse-mesh interest");

            helper.assertTrue(
                    input.retainPhysicalSourcePage(receiver, 4),
                    "the compositor test must admit through the real interest path");
            MinecraftThermalInput.sealActiveLevel(level);
            MinecraftThermalInput.MutableEnvironmentSample sample =
                    new MinecraftThermalInput.MutableEnvironmentSample();
            input.sampleCropEnvironment(
                    receiver.getX(), receiver.getY(), receiver.getZ(),
                    initialTick, 40, sample);
            helper.assertTrue(
                    sample.airAvailable()
                            && close(sample.airTemperatureC(), -3.0D)
                            && (sample.flags()
                            & MinecraftThermalInput.QUERY_ANALYTIC_FIELD_APPLIED) != 0,
                    "OVERRIDE -> MAX_HEAT -> MIN_COOL -> ADD_DELTA must follow mesh");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 40)
    public static void undergroundAdmissionAddsOnlyOneLoadedContinuationLayer(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 3, 2));
        LevelChunk chunk = level.getChunkAt(anchor);
        int sectionIndex = boundedAllAirSectionIndex(chunk);
        int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
        int centerX = SectionPos.sectionToBlockCoord(chunk.getPos().x) + 8;
        int centerZ = SectionPos.sectionToBlockCoord(chunk.getPos().z) + 8;
        BlockPos target = new BlockPos(
                centerX, SectionPos.sectionToBlockCoord(sectionY) + 8, centerZ);
        BlockPos roof = new BlockPos(
                centerX, SectionPos.sectionToBlockCoord(sectionY) + 17, centerZ);
        level.setBlockAndUpdate(roof, Blocks.STONE.defaultBlockState());

        DimensionThermalRuntime runtime = runtime(level.getGameTime());
        try (runtime; MinecraftThermalInput input = new MinecraftThermalInput(
                level,
                9L,
                runtime,
                ThermalSignatureResolverDispatcher.builder(
                        StateStaticThermalResolver.geometryOnly(
                                ConservativeAirGeometry.MICROCELL_COUNT))
                        .build(),
                signatureRegistry(),
                1L,
                32,
                32)) {
            input.enableTopologyApplication(topologyParameters());
            input.enableDispatch(Runnable::run);

            helper.assertTrue(input.retainPhysicalSourcePage(target, 4),
                    "the loaded underground source Page must be admitted");
            MinecraftThermalInput.sealActiveLevel(level);
            int liveCells = runtime.thermalCellArena().liveCellCount();
            helper.assertTrue(liveCells >= 128 && liveCells <= 448,
                    "one direct Page must add loaded face continuations without recursion; got "
                            + liveCells + " cells");
        } finally {
            level.setBlockAndUpdate(roof, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = NEIGHBOR_BATCH, timeoutTicks = 40)
    public static void declaredNeighborDependencyInvalidatesItsResolverCenter(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 3, 2));
        LevelChunk chunk = level.getChunkAt(anchor);
        int sectionIndex = allAirSectionIndex(chunk);
        LevelChunkSection section = chunk.getSections()[sectionIndex];
        int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
        BlockPos center = new BlockPos(
                anchor.getX(), SectionPos.sectionToBlockCoord(sectionY) + 2, anchor.getZ());
        BlockPos east = center.east();
        ResolvedThermalSignature airSignature = StateStaticThermalResolver.geometryOnly(
                        ConservativeAirGeometry.MICROCELL_COUNT)
                .resolve(Blocks.AIR.defaultBlockState(),
                        Blocks.AIR.defaultBlockState().getFluidState())
                .value().orElseThrow();

        ThermalSignatureResolverDispatcher dispatcher =
                ThermalSignatureResolverDispatcher.builder(
                        StateStaticThermalResolver.geometryOnly(
                                ConservativeAirGeometry.MICROCELL_COUNT))
                        .registerExplicitOverride(
                                Blocks.OAK_FENCE, neighborAwareResolver(airSignature))
                        .build();
        setDirect(section, center, Blocks.OAK_FENCE.defaultBlockState());
        DimensionThermalRuntime runtime = runtime(level.getGameTime());
        MinecraftThermalInput input = new MinecraftThermalInput(
                level,
                9L,
                runtime,
                dispatcher,
                signatureRegistry(),
                1L,
                16,
                16);
        try {
            input.enableTopologyApplication(topologyParameters());
            input.enableDispatch(Runnable::run);
            helper.assertTrue(input.retainPhysicalSourcePage(center, 4),
                    "the real source-interest path must admit the target Page");
            MinecraftThermalInput.sealActiveLevel(level);
            MinecraftThermalInput.MutableEnvironmentSample sample =
                    new MinecraftThermalInput.MutableEnvironmentSample();
            input.sampleCropEnvironment(
                    center.getX(), center.getY(), center.getZ(),
                    level.getGameTime(), 40, sample);
            helper.assertTrue(sample.airAvailable(),
                    "the captured fence resolver must initially observe its east-side air");

            setDirect(section, east, Blocks.STONE.defaultBlockState());
            MinecraftThermalInput.sealActiveLevel(level);
            input.sampleCropEnvironment(
                    center.getX(), center.getY(), center.getZ(),
                    level.getGameTime(), 40, sample);
            helper.assertTrue(!sample.airAvailable()
                            && (sample.flags()
                            & MinecraftThermalInput.QUERY_STALE_GEOMETRY) != 0,
                    "a live mutation must reject stale publication before the rebuild deadline");
        } catch (RuntimeException | Error exception) {
            input.close();
            throw exception;
        }

        helper.runAfterDelay(7L, () -> {
            try {
                MinecraftThermalInput.MutableEnvironmentSample sample =
                        new MinecraftThermalInput.MutableEnvironmentSample();
                input.sampleCropEnvironment(
                        center.getX(), center.getY(), center.getZ(),
                        level.getGameTime(), 40, sample);
                helper.assertTrue(!sample.airAvailable()
                                && (sample.flags()
                                & MinecraftThermalInput.QUERY_NO_AIR_COMPONENT) != 0,
                        "the east mutation must rebuild the resolver center through production");
            } finally {
                input.close();
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = SOURCE_BATCH, timeoutTicks = 40)
    public static void allDeviceHeatEntersThePhysicalLedgerOnce(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 3, 2));
        LevelChunk chunk = level.getChunkAt(anchor);
        int sectionIndex = allAirSectionIndex(chunk);
        int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
        BlockPos target = new BlockPos(
                anchor.getX(), SectionPos.sectionToBlockCoord(sectionY) + 4, anchor.getZ());
        BlockPos campfire = target.below();
        BlockPos generator = target.offset(3, -3, 0);
        BlockPos fountain = target.offset(-3, -2, 0);
        BlockPos radiator = target.offset(0, -2, 3);
        long initialTick = level.getGameTime();

        DimensionThermalRuntime runtime = runtime(initialTick);
        MinecraftThermalInput input = new MinecraftThermalInput(
                level,
                9L,
                runtime,
                ThermalSignatureResolverDispatcher.builder(
                        StateStaticThermalResolver.geometryOnly(
                                ConservativeAirGeometry.MICROCELL_COUNT))
                        .build(),
                signatureRegistry(),
                1L,
                32,
                32);
        try {
            input.enableTopologyApplication(topologyParameters());
            input.enablePhysicalSources(4);
            input.enableDispatch(Runnable::run);
            helper.assertTrue(
                    input.retainPhysicalSourcePage(target, 4),
                    "the source target Page must use the real interest path");

            placeLitCampfire(level, campfire);
            MinecraftThermalInput.onGeneratorTick(
                    level, generator, target, 2.0D, true);
            MinecraftThermalInput.onFountainTick(
                    level, fountain, target, 2.0D, true);
            MinecraftThermalInput.onRadiatorTick(
                    level, radiator, target, 2.0D, true);
        } catch (RuntimeException | Error exception) {
            input.close();
            throw exception;
        }

        helper.runAfterDelay(21L, () -> {
            try {
                helper.assertTrue(runtime.lastCompletedTargetTick() > initialTick,
                        "the production level tick must advance the source timeline");
                double campfireAirJ = routedEnergy(
                        runtime, campfire, SourceBinding.Kind.THERMAL_NODE);
                double generatorAirJ = routedEnergy(
                        runtime, generator, SourceBinding.Kind.THERMAL_NODE);
                double fountainAirJ = routedEnergy(
                        runtime, fountain, SourceBinding.Kind.THERMAL_NODE);
                double radiatorAirJ = routedEnergy(
                        runtime, radiator, SourceBinding.Kind.THERMAL_NODE);
                double routedAirJ = campfireAirJ + generatorAirJ
                        + fountainAirJ + radiatorAirJ;
                helper.assertTrue(campfireAirJ > 0.0D
                                && generatorAirJ > 0.0D
                                && fountainAirJ > 0.0D
                                && radiatorAirJ > 0.0D,
                        "every open device convection port must bind to the mesh; campfire="
                                + campfireAirJ + ", generator=" + generatorAirJ
                                + ", fountain=" + fountainAirJ
                                + ", radiator=" + radiatorAirJ);
                double airEnthalpyJ = totalEnthalpy(runtime.thermalCellArena());
                helper.assertTrue(close(airEnthalpyJ, routedAirJ),
                        "every device convection share must enter the arena once; actual="
                                + airEnthalpyJ + " J, routed=" + routedAirJ + " J");
                double generatorInternalJ = routedEnergy(
                        runtime, generator, SourceBinding.Kind.INTERNAL_RESERVOIR);
                helper.assertTrue(generatorInternalJ > 0.0D
                                && close(routedEnergy(
                                        runtime, generator,
                                        SourceBinding.Kind.DECLARED_LOSS),
                                generatorInternalJ * 2.0D),
                        "generator radiation must remain a declared Phase J loss");
                helper.assertTrue(routedEnergy(
                                runtime, fountain, SourceBinding.Kind.DECLARED_LOSS) > 0.0D,
                        "fountain radiation must remain a declared Phase J loss");
                double radiatorInternalJ = routedEnergy(
                        runtime, radiator, SourceBinding.Kind.INTERNAL_RESERVOIR);
                helper.assertTrue(radiatorInternalJ > 0.0D
                                && close(routedEnergy(
                                        runtime, radiator,
                                        SourceBinding.Kind.DECLARED_LOSS),
                                radiatorInternalJ),
                        "radiator radiation must remain a declared Phase J loss");
            } finally {
                input.close();
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = SINK_BATCH, timeoutTicks = 40)
    public static void blockedAndUnresolvedPortsUseTheirDeclaredSinks(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 3, 2));
        LevelChunk chunk = level.getChunkAt(anchor);
        int sectionIndex = allAirSectionIndex(chunk);
        LevelChunkSection section = chunk.getSections()[sectionIndex];
        int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
        int baseY = SectionPos.sectionToBlockCoord(sectionY) + 4;
        BlockPos blockedTarget = new BlockPos(anchor.getX(), baseY, anchor.getZ());
        BlockPos unresolvedTarget = blockedTarget.offset(5, 0, 0);
        BlockPos campfire = blockedTarget.below();
        BlockPos generator = unresolvedTarget.offset(0, -3, 0);
        long initialTick = level.getGameTime();
        setDirect(section, blockedTarget, Blocks.STONE.defaultBlockState());
        setDirect(section, unresolvedTarget, Blocks.MOVING_PISTON.defaultBlockState());

        DimensionThermalRuntime runtime = runtime(initialTick);
        MinecraftThermalInput input = new MinecraftThermalInput(
                level,
                9L,
                runtime,
                ThermalSignatureResolverDispatcher.builder(
                        StateStaticThermalResolver.geometryOnly(
                                ConservativeAirGeometry.MICROCELL_COUNT))
                        .build(),
                signatureRegistry(),
                1L,
                32,
                32);
        try {
            input.enableTopologyApplication(topologyParameters());
            input.enablePhysicalSources(4);
            input.enableDispatch(Runnable::run);
            helper.assertTrue(
                    input.retainPhysicalSourcePage(blockedTarget, 4),
                    "the source target Page must use the real interest path");
            placeLitCampfire(level, campfire);
            MinecraftThermalInput.onGeneratorTick(
                    level, generator, unresolvedTarget, 1.0D, true);
        } catch (RuntimeException | Error exception) {
            input.close();
            throw exception;
        }

        helper.runAfterDelay(21L, () -> {
            try {
                helper.assertTrue(runtime.lastCompletedTargetTick() > initialTick,
                        "the production level tick must advance the source timeline");
                double generatorInternalJ = routedEnergy(
                        runtime, generator, SourceBinding.Kind.INTERNAL_RESERVOIR);
                double generatorSeconds = generatorInternalJ / 1_000.0D;
                helper.assertTrue(Double.isFinite(generatorSeconds)
                                && generatorSeconds > 0.0D,
                        "the production source timeline must integrate generator power");
                helper.assertTrue(close(routedEnergy(
                                runtime, generator, SourceBinding.Kind.DEGRADED_LOSS),
                                7_000.0D * generatorSeconds),
                        "unresolved generator exhaust must become degraded loss");
                helper.assertTrue(close(routedEnergy(
                                runtime, generator, SourceBinding.Kind.DECLARED_LOSS),
                                2_000.0D * generatorSeconds),
                        "generator radiation share must remain declared loss");
                double campfireDeclared = routedEnergy(
                        runtime, campfire, SourceBinding.Kind.DECLARED_LOSS);
                double campfireDegraded = routedEnergy(
                        runtime, campfire, SourceBinding.Kind.DEGRADED_LOSS);
                helper.assertTrue(campfireDeclared > 0.0D,
                        "blocked campfire convection must remain an explicit loss; declared="
                                + campfireDeclared + ", degraded=" + campfireDegraded);
                helper.assertTrue(close(totalEnthalpy(runtime.thermalCellArena()), 0.0D),
                        "blocked or unresolved ports must not inject an arbitrary air cell");
            } finally {
                input.close();
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 40)
    public static void radiationWallBlocksWithoutChangingSourceLedger(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 3, 2));
        LevelChunk chunk = level.getChunkAt(anchor);
        int sectionIndex = allAirSectionIndex(chunk);
        int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
        int baseY = SectionPos.sectionToBlockCoord(sectionY) + 4;
        BlockPos sourcePosition = new BlockPos(anchor.getX(), baseY, anchor.getZ());
        BlockPos wall = sourcePosition.east(2);

        StateStaticThermalResolver radiationResolver = new StateStaticThermalResolver(
                ConservativeAirGeometry.MICROCELL_COUNT,
                (state, fluid) -> new StateStaticThermalResolver.SignatureMetadata(
                        0, 0, 0, 0, 0, 0, 0));
        ThermalSignatureRegistry.Builder signatureBuilder = ThermalSignatureRegistry.builder();
        signatureBuilder.intern(radiationResolver.resolve(
                Blocks.AIR.defaultBlockState(),
                Blocks.AIR.defaultBlockState().getFluidState()).value().orElseThrow());
        signatureBuilder.intern(radiationResolver.resolve(
                Blocks.STONE.defaultBlockState(),
                Blocks.STONE.defaultBlockState().getFluidState()).value().orElseThrow());

        DimensionThermalRuntime runtime = runtime(level.getGameTime());
        try (runtime; MinecraftThermalInput input = new MinecraftThermalInput(
                level,
                9L,
                runtime,
                ThermalSignatureResolverDispatcher.builder(radiationResolver).build(),
                signatureBuilder.build(),
                1L,
                32,
                32)) {
            input.enableTopologyApplication(topologyParameters());
            input.enablePhysicalSources(4);

            RadiationService.Parameters parameters =
                    MinecraftThermalInput.GAMEPLAY_RADIATION_PARAMETERS;
            ThermalMemoryBudget radiationServerBudget =
                    new ThermalMemoryBudget(1_000_000L, 0L);
            helper.assertTrue(input.tryEnableRadiation(
                            parameters,
                            radiationServerBudget.createDimensionBudget(1_000_000L, 0L)),
                    "the optional radiation budget must admit the Phase J service");
            placeLitCampfire(level, sourcePosition);
            MinecraftThermalInput.MutableEnvironmentSample sample =
                    new MinecraftThermalInput.MutableEnvironmentSample();
            double receiverX = sourcePosition.getX() + 4.5D;
            double receiverFeetY = sourcePosition.getY();
            double receiverZ = sourcePosition.getZ() + 0.5D;
            long sourceWatermark = runtime.latestOfferedSourceWatermark();
            input.samplePlayerEnvironment(
                    41L, 1,
                    receiverX, receiverFeetY, receiverFeetY + 1.62D, receiverZ,
                    level.getGameTime(), 40, sample);
            double visibleFlux = sample.radiantFluxWPerM2();
            helper.assertTrue(visibleFlux > 0.0D,
                    "the unobstructed Campfire must be visible to all player rays");
            level.setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(wall.above(), Blocks.STONE.defaultBlockState(), 3);
            input.samplePlayerEnvironment(
                    41L, 1,
                    receiverX, receiverFeetY, receiverFeetY + 1.62D, receiverZ,
                    level.getGameTime(), 40, sample);
            helper.assertTrue(close(sample.radiantFluxWPerM2(), 0.0D),
                    "section revision must retrace and observe a wall for the same receiver");

            level.setBlock(wall, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(wall.above(), Blocks.AIR.defaultBlockState(), 3);
            input.samplePlayerEnvironment(
                    41L, 1,
                    receiverX, receiverFeetY, receiverFeetY + 1.62D, receiverZ,
                    level.getGameTime(), 40, sample);
            helper.assertTrue(close(sample.radiantFluxWPerM2(), visibleFlux),
                    "section revision must retrace after wall removal and restore flux");
            input.samplePlayerEnvironment(
                    41L, 1,
                    receiverX, receiverFeetY, receiverFeetY + 1.62D, receiverZ,
                    level.getGameTime(), 40, sample);
            helper.assertTrue(close(sample.radiantFluxWPerM2(), visibleFlux),
                    "an unchanged witness must preserve the visible flux");
            helper.assertTrue(runtime.latestOfferedSourceWatermark() == sourceWatermark,
                    "receiver observations must not write the physical source ledger");
        }
        helper.succeed();
    }

    private static void setDirect(
            LevelChunkSection section,
            BlockPos position,
            BlockState state
    ) {
        section.setBlockState(
                SectionPos.sectionRelative(position.getX()),
                SectionPos.sectionRelative(position.getY()),
                SectionPos.sectionRelative(position.getZ()),
                state,
                false);
    }

    private static void placeLitCampfire(ServerLevel level, BlockPos position) {
        BlockState replaced = level.getBlockState(position);
        BlockState campfire = litCampfireState();
        level.setBlock(position, campfire, 3);
        if (!(level.getBlockEntity(position) instanceof ICampfireExtra campfireExtra)) {
            throw new IllegalStateException("placed Campfire has no lifetime state");
        }
        campfireExtra.setLifeTime(200);
        MinecraftThermalInput.onPotentialPhysicalSourcePlaced(
                level, position, replaced, campfire);
    }

    private static double totalEnthalpy(ThermalCellArena arena) {
        double total = 0.0D;
        for (int slot = 0; slot < arena.highWaterMark(); slot++) {
            if (arena.isLive(slot)) {
                total += arena.enthalpyJ(slot);
            }
        }
        return total;
    }

    private static double routedEnergy(
            DimensionThermalRuntime runtime,
            BlockPos source,
            SourceBinding.Kind kind
    ) {
        return runtime.sourceTimeline().routedEnergyJ(source.asLong(), kind);
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) <= 1.0e-8D;
    }

    private static int allAirSectionIndex(LevelChunk chunk) {
        for (int index = chunk.getSections().length - 1; index >= 0; index--) {
            if (chunk.getSections()[index].hasOnlyAir()) {
                return index;
            }
        }
        throw new IllegalStateException("GameTest chunk has no all-air section metadata proof");
    }

    private static int boundedAllAirSectionIndex(LevelChunk chunk) {
        for (int index = 1; index < chunk.getSections().length - 1; index++) {
            if (chunk.getSections()[index].hasOnlyAir()) {
                return index;
            }
        }
        throw new IllegalStateException(
                "GameTest chunk has no vertically bounded all-air section");
    }

    private static ThermalSignatureResolver<BlockState, FluidState> neighborAwareResolver(
            ResolvedThermalSignature airSignature
    ) {
        return new ThermalSignatureResolver<>() {
            @Override
            public String resolverId() {
                return "frostedheart:gametest_neighbor_aware";
            }

            @Override
            public DependencyOffsetMask dependencyMask() {
                return DependencyOffsetMask.NEIGHBOR_6;
            }

            @Override
            public int maxOutputRegions() {
                return airSignature.localAirRegionCount();
            }

            @Override
            public ThermalResolution<ResolvedThermalSignature> resolve(
                    ResolverBlockView.Access<BlockState, FluidState> view
            ) {
                ThermalResolution<?> self = view.lookup(DependencyOffsetMask.SELF).asResolution();
                if (!self.isResolved()) {
                    return ThermalResolution.failure(self.reason());
                }
                ThermalResolution<ResolverBlockView.StateAndFluid<BlockState, FluidState>> east =
                        view.lookup(1, 0, 0).asResolution();
                if (!east.isResolved()) {
                    return ThermalResolution.failure(east.reason());
                }
                return ThermalResolution.resolved(
                        east.value().orElseThrow().blockState().isAir()
                                ? airSignature
                                : SOLID_SIGNATURE);
            }
        };
    }

    private static ThermalSignatureRegistry signatureRegistry() {
        ThermalSignatureRegistry.Builder builder = ThermalSignatureRegistry.builder();
        StateStaticThermalResolver resolver = StateStaticThermalResolver.geometryOnly(
                ConservativeAirGeometry.MICROCELL_COUNT);
        builder.intern(resolver.resolve(
                Blocks.AIR.defaultBlockState(),
                Blocks.AIR.defaultBlockState().getFluidState()).value().orElseThrow());
        BlockState campfire = litCampfireState();
        builder.intern(resolver.resolve(
                campfire, campfire.getFluidState()).value().orElseThrow());
        builder.intern(SOLID_SIGNATURE);
        return builder.build();
    }

    private static BlockState litCampfireState() {
        return Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
    }

    private static DimensionThermalRuntime runtime(long initialTick) {
        ThermalCellArena arena = new ThermalCellArena(0);
        NodePowerAccumulatorArena accumulators = new NodePowerAccumulatorArena(0);
        ThermalSourceTimeline sources = new ThermalSourceTimeline(
                9L,
                initialTick,
                16,
                new ThermalSourceRegistry(0, 3, accumulators),
                arena);
        ThermalSweep sweep = ThermalSweepFragments.builder(
                arena, null,
                new BuoyancyConductance.Parameters(1.0D, 1.0D, 1.0D), 0).build();
        ThermalMemoryBudget serverBudget = new ThermalMemoryBudget(1_000_000L, 0L);
        QueryPublication publication = QueryPublication.tryCreate(
                serverBudget.createDimensionBudget(1_000_000L, 0L),
                256);
        if (publication == null) {
            throw new IllegalStateException("GameTest publication admission failed");
        }
        return new DimensionThermalRuntime(
                9L,
                initialTick,
                InputWatermarks.ZERO,
                0L,
                0L,
                false,
                new ThermalTimePolicy(5L, 20L, 2),
                arena,
                sources,
                sweep,
                publication,
                0.0D,
                new DimensionThermalRuntime.Limits(256, 1024, 8, 3, 1.0e-9D));
    }

    private static MinecraftThermalTopologyApplier.Parameters topologyParameters() {
        return new MinecraftThermalTopologyApplier.Parameters(
                0,
                0,
                ConservativeAirGeometry.MICROCELL_COUNT,
                1.0D,
                0.0D,
                0.0D,
                1.0D,
                0.25D,
                false,
                new BuoyancyConductance.Parameters(1.0D, 1.0D, 1.0D));
    }
}

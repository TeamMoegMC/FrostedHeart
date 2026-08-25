/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.data.PlantTempData;
import com.teammoeg.frostedheart.content.climate.thermal.consumer.TownThermalProjection;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPage;
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
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalRuntimeCoordinator;
import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.solver.InputWatermarks;
import com.teammoeg.frostedheart.content.climate.thermal.solver.LatestSolveEpochScheduler;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweep;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalTimePolicy;
import com.teammoeg.frostedheart.content.climate.thermal.source.NodePowerAccumulatorArena;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceBinding;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceTimeline;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(FHMain.MODID)
@PrefixGameTestTemplate(false)
public final class FrostedHeartMinecraftThermalInputGameTests {
    private static final String BATCH = "frostedheart_minecraft_thermal_input";
    private static final String TEMPLATE = "phase0a_empty";
    private static final ResolvedThermalSignature SOLID_SIGNATURE =
            new ResolvedThermalSignature(0, 0, List.of(), 0, 0, 0, 0, 0);

    private FrostedHeartMinecraftThermalInputGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 40)
    public static void admittedMutationSealsPrimitiveInputWithoutFalseAck(
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

        DimensionThermalRuntime runtime = runtime(
                level.getGameTime(), chunk.getPos().x, sectionY, chunk.getPos().z);
        ThermalRuntimeCoordinator coordinator = coordinator();
        ThermalSignatureRegistry signatures = signatureRegistry();
        ThermalSignatureResolverDispatcher dispatcher =
                ThermalSignatureResolverDispatcher.builder(
                        StateStaticThermalResolver.geometryOnly(
                                ConservativeAirGeometry.MICROCELL_COUNT))
                        .build();
        try (runtime; coordinator; MinecraftThermalInput input = new MinecraftThermalInput(
                level,
                9L,
                runtime,
                dispatcher,
                signatures,
                1L,
                 16,
                 16)) {
            input.enableTopologyApplication(topologyParameters());
            input.enableShadowDispatch(coordinator, Runnable::run);
            ThermalPage page = input.admitAllAirPage(chunk, sectionIndex, 0, 0);
            helper.assertTrue(page != null, "all-air admission must create one Page");

            int localX = SectionPos.sectionRelative(position.getX());
            int localY = SectionPos.sectionRelative(position.getY());
            int localZ = SectionPos.sectionRelative(position.getZ());
            section.setBlockState(
                    localX, localY, localZ, Blocks.STONE.defaultBlockState(), false);

            helper.assertTrue(page.liveGeometryRevision() == 1L,
                    "the real section Mixin must invalidate the admitted Page immediately");
            helper.assertTrue(input.resolvedInputs().size() == 1,
                    "SELF_ONLY static geometry must publish one primitive center result");

            MinecraftThermalInput.SealReport sealed = input.sealTick(level.getGameTime());
            helper.assertTrue(
                    sealed.runtimeResult() == LatestSolveEpochScheduler.SealResult.ACCEPTED,
                    "the concrete input cut must enter the PR7 latest-frame scheduler");
            helper.assertTrue(sealed.frame().watermarks().geometry() == 1L,
                    "the sealed geometry watermark must cover the primitive result");
            helper.assertTrue(sealed.frame().watermarks().profile() == 1L,
                    "the frame must carry the frozen profile-table cut");
            MinecraftThermalTopologyApplier.ApplyReport applied =
                    input.latestShadowReport().topology();
            helper.assertTrue(
                    applied.status() == MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    "the explicit topology applier must commit the primitive frame");
            int changedBase = com.teammoeg.frostedheart.content.climate.thermal.geometry
                    .GeometrySummaryCache.baseIndex(localX, localY, localZ);
            helper.assertTrue(page.geometrySummary(changedBase).kind()
                            == com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometrySummary.Kind.MIXED,
                    "one stone in an all-air Brick must install compiled mixed coverage");
            helper.assertTrue(runtime.lastCompletedTargetTick() == level.getGameTime(),
                    "shadow dispatch must run the admitted epoch on the coordinator");

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

            int admittedBeforeMiss = input.admittedPageCount();
            input.samplePlayerEnvironment(
                    101L, 1,
                    sampleX + 32.0D, position.getY(), position.getY() + 1.5D, sampleZ,
                    level.getGameTime(), 40, playerSample);
            helper.assertTrue(!playerSample.airAvailable()
                            && (playerSample.flags()
                            & MinecraftThermalInput.QUERY_NO_PAGE) != 0
                            && input.admittedPageCount() == admittedBeforeMiss,
                    "a passive player miss must not admit or load a Page");

            input.samplePlayerEnvironment(
                    101L, 1,
                    sampleX, position.getY(), position.getY() + 1.5D, sampleZ,
                    level.getGameTime() + 41L, 40, playerSample);
            helper.assertTrue(!playerSample.airAvailable()
                            && (playerSample.flags()
                            & MinecraftThermalInput.QUERY_PUBLICATION_STALE) != 0,
                    "an over-age publication must use explicit fallback");

            double enthalpyBeforeMachineQueries = totalEnthalpy(
                    runtime.thermalCellArena());
            InputWatermarks watermarksBeforeMachineQueries = runtime.appliedWatermarks();
            long completedTickBeforeMachineQueries = runtime.lastCompletedTargetTick();
            int admittedBeforeMachineQueries = input.admittedPageCount();
            MinecraftThermalInput.MutableEnvironmentSample machineSample =
                    new MinecraftThermalInput.MutableEnvironmentSample();
            input.sampleMachineEnvironment(
                    sampleX, position.getY() + 1.5D, sampleZ,
                    level.getGameTime(), 40, machineSample);
            helper.assertTrue(machineSample.airAvailable()
                            && close(machineSample.airTemperatureC(), 0.0D)
                            && machineSample.radiantFluxWPerM2() == 0.0D
                            && (machineSample.flags()
                            & MinecraftThermalInput.QUERY_RADIATION_UNAVAILABLE) == 0,
                    "a QUERY_ONLY machine must read published air without player radiation");

            MinecraftThermalInput.observeRegisteredMachineEnvironment(
                    level, position.above(), 5.0D);
            MinecraftThermalInput.MachineShadowSnapshot observedMachine =
                    input.machineShadowSnapshot();
            helper.assertTrue(observedMachine.comparisons() == 1L
                            && close(observedMachine.meanAbsoluteErrorC(), 5.0D),
                    "an explicitly registered machine may record a bounded shadow comparison");

            for (int index = 0; index < 64; index++) {
                input.sampleMachineEnvironment(
                        sampleX + 32.0D + index * 16.0D,
                        position.getY() + 1.5D,
                        sampleZ,
                        level.getGameTime(), 40, machineSample);
            }
            helper.assertTrue(!machineSample.airAvailable()
                            && (machineSample.flags()
                            & MinecraftThermalInput.QUERY_NO_PAGE) != 0
                            && input.admittedPageCount() == admittedBeforeMachineQueries,
                    "passive machine count must not create Page interest");

            input.sampleMachineEnvironment(
                    sampleX, position.getY() + 1.5D, sampleZ,
                    level.getGameTime() + 41L, 40, machineSample);
            helper.assertTrue(!machineSample.airAvailable()
                            && (machineSample.flags()
                            & MinecraftThermalInput.QUERY_PUBLICATION_STALE) != 0,
                    "a machine must fall back from an over-age publication");
            helper.assertTrue(close(
                            totalEnthalpy(runtime.thermalCellArena()),
                            enthalpyBeforeMachineQueries)
                            && runtime.appliedWatermarks().equals(
                            watermarksBeforeMachineQueries)
                            && runtime.lastCompletedTargetTick()
                            == completedTickBeforeMachineQueries,
                    "machine observation must not mutate thermal energy or runtime state");

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
            MinecraftThermalInput.CropShadowSnapshot observedCrop =
                    input.cropShadowSnapshot();
            helper.assertTrue(cropStatus == WorldTemperature.PlantStatus.CAN_SURVIVE
                            && observedCrop.comparisons() == 1L
                            && close(observedCrop.meanAbsoluteErrorC(), 5.0D),
                    "the real plant-status path must use published 0 C, not legacy 5 C");

            TownThermalProjection townProjection = new TownThermalProjection();
            townProjection.include(cropPosition);
            townProjection.include((cropPosition.getX() & 3) == 3
                    ? cropPosition.west() : cropPosition.east());
            double gameplayTownTemperatureC =
                    MinecraftThermalInput.gameplayTownEnvironment(
                            level, townProjection, 5.0D);
            MinecraftThermalInput.TownShadowSnapshot observedTown =
                    input.townShadowSnapshot();
            helper.assertTrue(close(gameplayTownTemperatureC, 0.0D)
                            && observedTown.queryCalls() == 1L
                            && observedTown.groupLookups() == 1L
                            && observedTown.comparisons() == 1L
                            && observedTown.latestGroupCount() == 1
                            && observedTown.latestVoxelCount() == 2
                            && close(observedTown.meanAbsoluteErrorC(), 5.0D),
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
                            && input.admittedPageCount() == admittedBeforeMachineQueries
                            && runtime.thermalCellArena().highWaterMark()
                            == arenaHighWaterBeforeCropQueries
                            && runtime.thermalCellArena().liveCellCount()
                            == liveCellsBeforeCropQueries
                            && close(totalEnthalpy(runtime.thermalCellArena()),
                            enthalpyBeforeMachineQueries),
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
                            && input.admittedPageCount() == admittedBeforeMachineQueries
                            && runtime.thermalCellArena().highWaterMark()
                            == arenaHighWaterBeforeCropQueries
                            && runtime.thermalCellArena().liveCellCount()
                            == liveCellsBeforeCropQueries
                            && close(totalEnthalpy(runtime.thermalCellArena()),
                            enthalpyBeforeMachineQueries),
                    "10,000 passive crop misses must not retain thermal state");

            MinecraftThermalInput.ShadowRuntimeSnapshot shadow =
                    input.shadowRuntimeSnapshot();
            helper.assertTrue(shadow.admittedPageCount() == 1
                            && shadow.mixedBrickCount() == 1L
                            && shadow.runtime().liveCellCount() > 0,
                    "shadow diagnostics must expose the admitted mixed topology footprint");
            helper.assertTrue(shadow.publishedAirLookups()
                            == shadow.publishedAirHits() + shadow.publishedAirMisses()
                            && shadow.noPageLookups() >= 14_000L
                            && shadow.noAirComponentLookups() >= 1L
                            && shadow.stalePublicationLookups() >= 2L,
                    "shadow diagnostics must attribute passive lookup fallbacks by reason");
            helper.assertTrue(shadow.publicationAgeSamples() > 0L
                            && shadow.meanPublicationAgeTicks() >= 0.0D
                            && shadow.maximumPublicationAgeTicks() >= 41L,
                    "shadow diagnostics must retain publication age evidence");
            helper.assertTrue(shadow.sealCalls() == 1L
                            && shadow.workerFrames() == 1L
                            && shadow.executorRejectedSubmissions() == 0L
                            && shadow.latestDispatch() != null
                            && shadow.latestDispatch().solve() != null,
                    "shadow diagnostics must retain the actual dispatched solve report");
            helper.assertTrue(shadow.player().queryCalls() == 4L
                            && shadow.machine().comparisons() == 1L
                            && shadow.crop().comparisons() == 1L
                            && shadow.town().comparisons() == 1L,
                    "one snapshot must combine all real Phase K consumer evidence");

            MinecraftThermalInput.onRawBlockContainerReplaced(section);
            MinecraftThermalInput.SealReport resynced = input.sealTick(level.getGameTime());
            helper.assertTrue(resynced.fullResyncPages() == 1,
                    "raw replacement must capture one complete Page resnapshot");
            helper.assertTrue(!page.fullGeometryResyncRequired(),
                    "the matching 4096-block snapshot must clear the sticky requirement");
            helper.assertTrue(input.latestShadowReport().topology().status()
                            == MinecraftThermalTopologyApplier.ApplyStatus.APPLIED,
                    "the full Page resnapshot must rebuild and commit through shadow dispatch");

            MinecraftThermalInput.onChunkUnload(level, chunk);
            helper.assertTrue(input.admittedPageCount() == 0,
                    "chunk unload must remove the admitted Page from mutation ownership");
            setDirect(section, position, Blocks.AIR.defaultBlockState());
            helper.assertTrue(input.resolvedInputs().size() == 0,
                    "the detached section must return to the null-owner fast path");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 40)
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

        ThermalSignatureResolverDispatcher dispatcher =
                ThermalSignatureResolverDispatcher.builder(
                        StateStaticThermalResolver.geometryOnly(
                                ConservativeAirGeometry.MICROCELL_COUNT))
                        .registerExplicitOverride(Blocks.OAK_FENCE, neighborAwareResolver())
                        .build();
        DimensionThermalRuntime runtime = runtime(
                level.getGameTime(), chunk.getPos().x, sectionY, chunk.getPos().z);
        try (runtime; MinecraftThermalInput input = new MinecraftThermalInput(
                level,
                9L,
                runtime,
                dispatcher,
                signatureRegistry(),
                1L,
                16,
                16)) {
            ThermalPage page = input.admitAllAirPage(chunk, sectionIndex, 0, 0);
            helper.assertTrue(page != null, "all-air admission must create one Page");
            setDirect(section, center, Blocks.OAK_FENCE.defaultBlockState());

            ResolvedGeometryInputRing.MutableInput discarded =
                    new ResolvedGeometryInputRing.MutableInput();
            helper.assertTrue(input.resolvedInputs().poll(discarded),
                    "the initial fence placement must resolve its own center");
            helper.assertTrue(input.resolvedInputs().size() == 0,
                    "the initial SELF mutation must emit exactly one result");

            setDirect(section, east, Blocks.STONE.defaultBlockState());

            helper.assertTrue(input.resolvedInputs().size() == 2,
                    "the east mutation must resolve stone plus the fence center that declared EAST");
            helper.assertTrue(page.liveGeometryRevision() == 3L,
                    "dependency invalidation must be represented by the same Page revision authority");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 40)
    public static void campfireAndGeneratorEnterTheDormantPhysicalLedger(
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
        long initialTick = level.getGameTime();

        DimensionThermalRuntime runtime = runtime(
                initialTick, chunk.getPos().x, sectionY, chunk.getPos().z);
        ThermalRuntimeCoordinator coordinator = coordinator();
        try (runtime; coordinator; MinecraftThermalInput input = new MinecraftThermalInput(
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
            MinecraftPhysicalSourceManager sources = input.enablePhysicalSources(4);
            input.enableShadowDispatch(coordinator, Runnable::run);
            helper.assertTrue(
                    input.admitAllAirPage(chunk, sectionIndex, 0, 0) != null,
                    "the source target Page must be admitted");

            input.sealTick(initialTick);
            sources.observeCampfire(campfire, true);
            sources.observeGenerator(generator, target, 2.0D, true);
            input.sealTick(initialTick);
            input.sealTick(initialTick + 20L);
            runtime.sourceTimeline().snapshotAtCursor(campfire.asLong());
            runtime.sourceTimeline().snapshotAtCursor(generator.asLong());

            double airEnthalpyJ = totalEnthalpy(runtime.thermalCellArena());
            helper.assertTrue(close(airEnthalpyJ, 20_400.0D),
                    "one second must route 6,400 J + 14,000 J into air; actual="
                            + airEnthalpyJ + " J");
            helper.assertTrue(close(runtime.sourceTimeline().routedEnergyJ(
                            campfire.asLong(), SourceBinding.Kind.DECLARED_LOSS), 1_600.0D),
                    "campfire radiation must remain a declared Phase J loss");
            helper.assertTrue(close(runtime.sourceTimeline().routedEnergyJ(
                            generator.asLong(), SourceBinding.Kind.INTERNAL_RESERVOIR), 2_000.0D),
                    "generator contact heat must remain in its internal reservoir");
            helper.assertTrue(close(runtime.sourceTimeline().routedEnergyJ(
                            generator.asLong(), SourceBinding.Kind.DECLARED_LOSS), 4_000.0D),
                    "generator radiation must remain a declared Phase J loss");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 40)
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

        DimensionThermalRuntime runtime = runtime(
                initialTick, chunk.getPos().x, sectionY, chunk.getPos().z);
        ThermalRuntimeCoordinator coordinator = coordinator();
        try (runtime; coordinator; MinecraftThermalInput input = new MinecraftThermalInput(
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
            MinecraftPhysicalSourceManager sources = input.enablePhysicalSources(4);
            input.enableShadowDispatch(coordinator, Runnable::run);
            helper.assertTrue(
                    input.admitAllAirPage(chunk, sectionIndex, 0, 0) != null,
                    "the source target Page must be admitted");
            sources.observeCampfire(campfire, true);
            sources.observeGenerator(generator, unresolvedTarget, 1.0D, true);

            setDirect(section, blockedTarget, Blocks.STONE.defaultBlockState());
            setDirect(section, unresolvedTarget, Blocks.MOVING_PISTON.defaultBlockState());
            input.sealTick(initialTick);
            input.sealTick(initialTick);
            input.sealTick(initialTick + 20L);
            runtime.sourceTimeline().snapshotAtCursor(campfire.asLong());
            runtime.sourceTimeline().snapshotAtCursor(generator.asLong());

            helper.assertTrue(close(runtime.sourceTimeline().routedEnergyJ(
                            campfire.asLong(), SourceBinding.Kind.DECLARED_LOSS), 8_000.0D),
                    "blocked campfire convection must become explicit loss");
            helper.assertTrue(close(runtime.sourceTimeline().routedEnergyJ(
                            generator.asLong(), SourceBinding.Kind.DEGRADED_LOSS), 7_000.0D),
                    "unresolved generator exhaust must become degraded loss");
            helper.assertTrue(close(runtime.sourceTimeline().routedEnergyJ(
                            generator.asLong(), SourceBinding.Kind.INTERNAL_RESERVOIR), 1_000.0D),
                    "generator contact share must remain internal");
            helper.assertTrue(close(runtime.sourceTimeline().routedEnergyJ(
                            generator.asLong(), SourceBinding.Kind.DECLARED_LOSS), 2_000.0D),
                    "generator radiation share must remain declared loss");
            helper.assertTrue(close(totalEnthalpy(runtime.thermalCellArena()), 0.0D),
                    "blocked or unresolved ports must not inject an arbitrary air cell");
        }
        helper.succeed();
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

        DimensionThermalRuntime runtime = runtime(
                level.getGameTime(), chunk.getPos().x, sectionY, chunk.getPos().z);
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
            MinecraftPhysicalSourceManager sources = input.enablePhysicalSources(4);
            sources.observeCampfire(sourcePosition, true);

            RadiationService.Parameters parameters =
                    MinecraftThermalInput.GAMEPLAY_RADIATION_PARAMETERS;
            ThermalMemoryBudget radiationServerBudget =
                    new ThermalMemoryBudget(1_000_000L, 0L);
            helper.assertTrue(input.tryEnableRadiation(
                            parameters,
                            radiationServerBudget.createDimensionBudget(1_000_000L, 0L)),
                    "the optional radiation budget must admit the Phase J service");
            helper.assertTrue(input.radiationSourceCount() == 1,
                    "enabling radiation after physical sources must replay the live source");

            RadiationService.MutableSample sample = new RadiationService.MutableSample();
            double receiverX = sourcePosition.getX() + 4.5D;
            double receiverFeetY = sourcePosition.getY();
            double receiverZ = sourcePosition.getZ() + 0.5D;
            long sourceWatermark = runtime.latestOfferedSourceWatermark();
            input.sampleRadiation(
                    41L, 1, receiverX, receiverFeetY, receiverZ, sample);
            double visibleFlux = sample.radiantFluxWPerM2();
            helper.assertTrue(visibleFlux > 0.0D,
                    "the unobstructed Campfire must be visible to all player rays");
            level.setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(wall.above(), Blocks.STONE.defaultBlockState(), 3);
            input.sampleRadiation(
                    41L, 1, receiverX, receiverFeetY, receiverZ, sample);
            helper.assertTrue(close(sample.radiantFluxWPerM2(), 0.0D),
                    "section revision must retrace and observe a wall for the same receiver");

            level.setBlock(wall, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(wall.above(), Blocks.AIR.defaultBlockState(), 3);
            input.sampleRadiation(
                    41L, 1, receiverX, receiverFeetY, receiverZ, sample);
            helper.assertTrue(close(sample.radiantFluxWPerM2(), visibleFlux),
                    "section revision must retrace after wall removal and restore flux");
            input.sampleRadiation(
                    41L, 1, receiverX, receiverFeetY, receiverZ, sample);
            helper.assertTrue(sample.cacheHits() == 3 && sample.retraces() == 0,
                    "an unchanged witness must serve all three rays without world reads");
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

    private static double totalEnthalpy(ThermalCellArena arena) {
        double total = 0.0D;
        for (int slot = 0; slot < arena.highWaterMark(); slot++) {
            if (arena.isLive(slot)) {
                total += arena.enthalpyJ(slot);
            }
        }
        return total;
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

    private static ThermalSignatureResolver<BlockState, FluidState> neighborAwareResolver() {
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
                return 0;
            }

            @Override
            public ThermalResolution<ResolvedThermalSignature> resolve(
                    ResolverBlockView.Access<BlockState, FluidState> view
            ) {
                ThermalResolution<?> self = view.lookup(DependencyOffsetMask.SELF).asResolution();
                if (!self.isResolved()) {
                    return ThermalResolution.failure(self.reason());
                }
                ThermalResolution<?> east = view.lookup(1, 0, 0).asResolution();
                if (!east.isResolved()) {
                    return ThermalResolution.failure(east.reason());
                }
                return ThermalResolution.resolved(SOLID_SIGNATURE);
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
        builder.intern(SOLID_SIGNATURE);
        return builder.build();
    }

    private static ThermalRuntimeCoordinator coordinator() {
        ThermalRuntimeCoordinator coordinator = ThermalRuntimeCoordinator.tryCreate(
                new ThermalMemoryBudget(1_000_000L, 0L),
                4,
                4,
                1,
                20L,
                2);
        if (coordinator == null) {
            throw new IllegalStateException("GameTest coordinator admission failed");
        }
        return coordinator;
    }

    private static DimensionThermalRuntime runtime(
            long initialTick,
            int sectionX,
            int sectionY,
            int sectionZ
    ) {
        ThermalCellArena arena = new ThermalCellArena(1);
        arena.allocatePageCells(
                0,
                1,
                new ThermalCellArena.CellSpec[]{
                        ThermalCellArena.CellSpec.regularAir(
                                SectionPos.sectionToBlockCoord(sectionX),
                                SectionPos.sectionToBlockCoord(sectionY),
                                SectionPos.sectionToBlockCoord(sectionZ),
                                16,
                                0,
                                0,
                                1.0D)
                },
                new double[]{0.0D});
        NodePowerAccumulatorArena accumulators = new NodePowerAccumulatorArena(0);
        ThermalSourceTimeline sources = new ThermalSourceTimeline(
                9L,
                initialTick,
                16,
                new ThermalSourceRegistry(0, 3, 16, accumulators),
                arena);
        ThermalSweep sweep = new ThermalSweep(
                arena,
                List.of(),
                List.of(),
                new BuoyancyConductance.Parameters(1.0D, 1.0D, 1.0D));
        ThermalMemoryBudget serverBudget = new ThermalMemoryBudget(1_000_000L, 0L);
        QueryPublication publication = QueryPublication.tryCreate(
                serverBudget.createDimensionBudget(1_000_000L, 0L),
                256);
        if (publication == null) {
            throw new IllegalStateException("GameTest publication admission failed");
        }
        return new DimensionThermalRuntime(
                100L,
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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
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
            helper.assertTrue(close(airEnthalpyJ, 14_800.0D),
                    "one second must route 800 J + 14,000 J into air; actual="
                            + airEnthalpyJ + " J");
            helper.assertTrue(close(runtime.sourceTimeline().routedEnergyJ(
                            campfire.asLong(), SourceBinding.Kind.DECLARED_LOSS), 200.0D),
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
                            campfire.asLong(), SourceBinding.Kind.DECLARED_LOSS), 1_000.0D),
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

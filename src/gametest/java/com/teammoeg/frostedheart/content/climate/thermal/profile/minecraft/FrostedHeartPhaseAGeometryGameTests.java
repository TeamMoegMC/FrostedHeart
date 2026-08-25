/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.profile.DependencyOffsetMask;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolverBlockView;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalResolution;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FHMain.MODID)
@PrefixGameTestTemplate(false)
public final class FrostedHeartPhaseAGeometryGameTests {
    private static final String BATCH = "frostedheart_phase_a_geometry";
    private static final String TEMPLATE = "phase0a_empty";
    private static final int MAXIMUM_REGIONS = ConservativeAirGeometry.MICROCELL_COUNT;

    private FrostedHeartPhaseAGeometryGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 40)
    public static void vanillaStaticStateMatrixUsesCapturedCollisionGeometry(
            GameTestHelper helper
    ) {
        StateStaticThermalResolver resolver = new StateStaticThermalResolver(
                MAXIMUM_REGIONS,
                (state, fluid) -> {
                    boolean hasFluid = !fluid.isEmpty();
                    return new StateStaticThermalResolver.SignatureMetadata(
                            hasFluid ? 1 : 0,
                            0,
                            hasFluid ? 2 : 0,
                            hasFluid ? 3 : 0,
                            0, 0, 0
                    );
                }
        );

        BlockState drySlab = Blocks.OAK_SLAB.defaultBlockState();
        BlockState waterloggedSlab = drySlab.setValue(BlockStateProperties.WATERLOGGED, true);
        BlockState northSouthFence = Blocks.OAK_FENCE.defaultBlockState()
                .setValue(BlockStateProperties.NORTH, true)
                .setValue(BlockStateProperties.SOUTH, true);
        BlockState northSouthPane = Blocks.GLASS_PANE.defaultBlockState()
                .setValue(BlockStateProperties.NORTH, true)
                .setValue(BlockStateProperties.SOUTH, true);

        BlockPos airPos = place(helper, new BlockPos(2, 3, 2), Blocks.AIR.defaultBlockState());
        BlockPos solidPos = place(helper, new BlockPos(4, 3, 2), Blocks.STONE.defaultBlockState());
        BlockPos slabPos = place(helper, new BlockPos(6, 3, 2), drySlab);
        BlockPos stairPos = place(helper, new BlockPos(8, 3, 2),
                Blocks.OAK_STAIRS.defaultBlockState());
        BlockPos closedDoorPos = place(helper, new BlockPos(10, 3, 2),
                Blocks.OAK_DOOR.defaultBlockState());
        BlockPos openDoorPos = place(helper, new BlockPos(12, 3, 2),
                Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.OPEN, true));
        BlockPos closedTrapdoorPos = place(helper, new BlockPos(14, 3, 2),
                Blocks.OAK_TRAPDOOR.defaultBlockState());
        BlockPos openTrapdoorPos = place(helper, new BlockPos(16, 3, 2),
                Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, true));
        BlockPos fencePos = place(helper, new BlockPos(2, 3, 5), northSouthFence);
        BlockPos panePos = place(helper, new BlockPos(4, 3, 5), northSouthPane);
        BlockPos snowPos = place(helper, new BlockPos(6, 3, 5),
                Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1));
        BlockPos waterloggedPos = place(helper, new BlockPos(8, 3, 5), waterloggedSlab);

        ResolvedThermalSignature air = resolveStored(helper, resolver, airPos);
        ResolvedThermalSignature solid = resolveStored(helper, resolver, solidPos);
        ResolvedThermalSignature slab = resolveStored(helper, resolver, slabPos);
        ResolvedThermalSignature stairs = resolveStored(helper, resolver, stairPos);
        ResolvedThermalSignature closedDoor = resolveStored(helper, resolver, closedDoorPos);
        ResolvedThermalSignature openDoor = resolveStored(helper, resolver, openDoorPos);
        ResolvedThermalSignature closedTrapdoor = resolveStored(helper, resolver, closedTrapdoorPos);
        ResolvedThermalSignature openTrapdoor = resolveStored(helper, resolver, openTrapdoorPos);
        ResolvedThermalSignature fence = resolveStored(helper, resolver, fencePos);
        ResolvedThermalSignature pane = resolveStored(helper, resolver, panePos);
        ResolvedThermalSignature snow = resolveStored(helper, resolver, snowPos);
        ResolvedThermalSignature waterlogged = resolveStored(helper, resolver, waterloggedPos);

        helper.assertTrue(provenAirMicrocells(air) == ConservativeAirGeometry.MICROCELL_COUNT,
                "air must preserve every proven-air microcell");
        helper.assertTrue(solid.localAirRegionCount() == 0,
                "a full solid must not fabricate an air opening");
        helper.assertTrue(provenAirMicrocells(slab) == 32,
                "a bottom slab must preserve its upper-half air volume");
        helper.assertTrue(provenAirMicrocells(stairs) == 16,
                "straight bottom stairs must preserve their quarter-block air volume");
        assertPartial(helper, "closed door", closedDoor);
        assertPartial(helper, "open door", openDoor);
        assertPartial(helper, "closed trapdoor", closedTrapdoor);
        assertPartial(helper, "open trapdoor", openTrapdoor);
        assertPartial(helper, "north-south fence", fence);
        assertPartial(helper, "north-south pane", pane);
        helper.assertTrue(provenAirMicrocells(snow) == ConservativeAirGeometry.MICROCELL_COUNT,
                "one snow layer has an empty collision shape and must remain open");
        helper.assertTrue(!closedDoor.airRegions().equals(openDoor.airRegions()),
                "stored DoorBlock.OPEN state must change resolved geometry");
        helper.assertTrue(!closedTrapdoor.airRegions().equals(openTrapdoor.airRegions()),
                "stored TrapDoorBlock.OPEN state must change resolved geometry");
        helper.assertTrue(fence.localAirRegionCount() == 2
                        && pane.localAirRegionCount() == 2,
                "north-south fence and pane collision shapes must preserve both side regions");
        helper.assertTrue(!helper.getLevel().getBlockState(waterloggedPos).getFluidState().isEmpty(),
                "the waterlogged fixture must retain its independent FluidState");
        helper.assertTrue(waterlogged.localAirRegionCount() == 0,
                "non-empty fluid fallback must close air instead of fabricating an opening");
        helper.assertTrue(slab.mediumId() == 0
                        && slab.materialContactPatternId() == 0
                        && slab.radiationOcclusionPatternId() == 0
                        && waterlogged.mediumId() == 1
                        && waterlogged.materialContactPatternId() == 2
                        && waterlogged.radiationOcclusionPatternId() == 3,
                "fluid, material-contact, and radiation metadata must remain independent");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 40)
    public static void dependencyMasksAndSnapshotSentinelsStayBounded(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(10, 6, 10));
        LoadedOnlyResolverSnapshot.Capture neighborhood = LoadedOnlyResolverSnapshot.capture(
                level, center, DependencyOffsetMask.NEIGHBOR_26);

        helper.assertTrue(DependencyOffsetMask.NEIGHBOR_26.offsetCount() == 27,
                "NEIGHBOR_26 must remain a bounded 3x3x3 snapshot");
        helper.assertTrue(neighborhood.isComplete()
                        && neighborhood.presentCellCount() == 27,
                "the local GameTest neighborhood must capture all 27 loaded cells");

        ResolverBlockView.Access<BlockState, ?> outsideAccess =
                neighborhood.view().openAccess();
        outsideAccess.lookup(2, 0, 0);
        ThermalResolution<BlockState> outside = outsideAccess.normalize(
                ThermalResolution.resolved(Blocks.AIR.defaultBlockState()));
        assertReason(helper, outside,
                ThermalResolution.Reason.DEPENDENCY_OUTSIDE_DECLARED_MASK);

        BlockPos outsideBuildHeight = new BlockPos(
                center.getX(), level.getMaxBuildHeight(), center.getZ());
        LoadedOnlyResolverSnapshot.Capture missingCapture = LoadedOnlyResolverSnapshot.capture(
                level, outsideBuildHeight, DependencyOffsetMask.SELF_ONLY);
        ThermalResolution<ResolvedThermalSignature> missing =
                StateStaticThermalResolver.geometryOnly(MAXIMUM_REGIONS)
                        .resolveSnapshot(missingCapture.view());
        helper.assertTrue(missingCapture.missingCellCount() == 1,
                "outside-build-height input must be an explicit missing snapshot cell");
        assertReason(helper, missing, ThermalResolution.Reason.SNAPSHOT_DATA_MISSING);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 40)
    public static void loadedOnlySnapshotDoesNotLoadRemoteChunk(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos anchorPos = helper.absolutePos(new BlockPos(10, 6, 10));
        ChunkPos anchor = new ChunkPos(anchorPos);
        ChunkPos remote = new ChunkPos(anchor.x + 4_096, anchor.z + 4_096);
        BlockPos remotePos = new BlockPos(
                (remote.x << 4) + 8,
                anchorPos.getY(),
                (remote.z << 4) + 8
        );

        helper.assertTrue(level.getChunkSource().getChunkNow(remote.x, remote.z) == null,
                "remote dependency chunk must start unloaded");
        LoadedOnlyResolverSnapshot.Capture capture = LoadedOnlyResolverSnapshot.capture(
                level, remotePos, DependencyOffsetMask.NEIGHBOR_26);
        ThermalResolution<ResolverBlockView.StateAndFluid<BlockState, FluidState>> resolution =
                capture.view().openAccess().lookup(DependencyOffsetMask.SELF).asResolution();

        helper.assertTrue(capture.presentCellCount() == 0
                        && capture.unloadedCellCount() == 27
                        && capture.missingCellCount() == 0,
                "the absent 3x3x3 footprint must remain explicit unloaded sentinels");
        assertReason(helper, resolution, ThermalResolution.Reason.DEPENDENCY_UNLOADED);
        helper.assertTrue(level.getChunkSource().getChunkNow(remote.x, remote.z) == null,
                "loaded-only capture must not request or retain the remote chunk");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 40)
    public static void pistonStatesRespectDynamicExclusion(GameTestHelper helper) {
        StateStaticThermalResolver resolver =
                StateStaticThermalResolver.geometryOnly(MAXIMUM_REGIONS);
        BlockPos basePos = place(helper, new BlockPos(6, 3, 10),
                Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.EXTENDED, true));
        BlockPos headPos = place(helper, new BlockPos(8, 3, 10),
                Blocks.PISTON_HEAD.defaultBlockState());
        BlockPos movingPos = place(helper, new BlockPos(10, 3, 10),
                Blocks.MOVING_PISTON.defaultBlockState());

        resolveStored(helper, resolver, basePos);
        resolveStored(helper, resolver, headPos);

        LoadedOnlyResolverSnapshot.Capture movingCapture = LoadedOnlyResolverSnapshot.capture(
                helper.getLevel(), movingPos, resolver.dependencyMask());
        ThermalResolution<ResolvedThermalSignature> moving =
                resolver.resolveSnapshot(movingCapture.view());
        assertReason(helper, moving, ThermalResolution.Reason.UNRESOLVED_DYNAMIC);

        ThermalResolution<Void> blockEntityRead =
                movingCapture.view().openAccess().blockEntity(0, 0, 0);
        assertReason(helper, blockEntityRead, ThermalResolution.Reason.BLOCK_ENTITY_DEPENDENT);
        helper.succeed();
    }

    private static BlockPos place(
            GameTestHelper helper,
            BlockPos relativePosition,
            BlockState state
    ) {
        BlockPos position = helper.absolutePos(relativePosition);
        helper.getLevel().setBlock(position, state, 2);
        helper.assertTrue(helper.getLevel().getBlockState(position).equals(state),
                "fixture state was not retained at " + relativePosition);
        return position;
    }

    private static ResolvedThermalSignature resolveStored(
            GameTestHelper helper,
            StateStaticThermalResolver resolver,
            BlockPos position
    ) {
        LoadedOnlyResolverSnapshot.Capture capture = LoadedOnlyResolverSnapshot.capture(
                helper.getLevel(), position, resolver.dependencyMask());
        helper.assertTrue(capture.isComplete() && capture.presentCellCount() == 1,
                "fixture resolution must use one captured loaded state at " + position);
        ThermalResolution<ResolvedThermalSignature> resolution =
                resolver.resolveSnapshot(capture.view());
        helper.assertTrue(resolution.isResolved(),
                "fixture must resolve at " + position + ": " + resolution.reason());
        return resolution.value().orElseThrow();
    }

    private static void assertPartial(
            GameTestHelper helper,
            String fixture,
            ResolvedThermalSignature signature
    ) {
        int provenAir = provenAirMicrocells(signature);
        helper.assertTrue(provenAir > 0 && provenAir < ConservativeAirGeometry.MICROCELL_COUNT,
                fixture + " must retain partial conservative air geometry, got " + provenAir);
    }

    private static int provenAirMicrocells(ResolvedThermalSignature signature) {
        return signature.airRegions().stream()
                .mapToInt(region -> region.microcellCount())
                .sum();
    }

    private static void assertReason(
            GameTestHelper helper,
            ThermalResolution<?> resolution,
            ThermalResolution.Reason reason
    ) {
        helper.assertTrue(resolution.status() == reason.expectedStatus()
                        && resolution.reason() == reason,
                "expected " + reason + " but got "
                        + resolution.status() + "/" + resolution.reason());
    }
}

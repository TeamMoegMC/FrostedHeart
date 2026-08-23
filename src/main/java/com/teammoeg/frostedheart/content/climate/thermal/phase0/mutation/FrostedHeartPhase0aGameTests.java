/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.phase0.mutation;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@GameTestHolder(FHMain.MODID)
@PrefixGameTestTemplate(false)
public final class FrostedHeartPhase0aGameTests {
    private static final String BATCH = "frostedheart_phase0a_mutation";
    private static final String TEMPLATE = "phase0a_empty";

    private FrostedHeartPhase0aGameTests() {
    }

    @BeforeBatch(batch = BATCH)
    public static void prepareProbe(ServerLevel level) {
        if (!Phase0aMutationProbe.isEnabled()) {
            throw new IllegalStateException(
                    "Phase 0a GameTests require -D" + Phase0aMutationProbe.ENABLE_PROPERTY + "=true");
        }
        Phase0aMutationProbe.resetDiagnosticsForBatch(level);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 40)
    public static void authoritativeHookCoversMappedWritePaths(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos levelWrite = helper.absolutePos(new BlockPos(2, 3, 2));
        BlockPos chunkWrite = helper.absolutePos(new BlockPos(4, 3, 2));
        BlockPos sectionWrite = helper.absolutePos(new BlockPos(6, 3, 2));
        BlockPos waterlogged = helper.absolutePos(new BlockPos(8, 3, 2));
        BlockPos trapdoor = helper.absolutePos(new BlockPos(10, 3, 2));
        BlockPos fenceGate = helper.absolutePos(new BlockPos(12, 3, 2));

        level.setBlock(waterlogged, Blocks.OAK_FENCE.defaultBlockState(), 2);
        level.setBlock(trapdoor, Blocks.OAK_TRAPDOOR.defaultBlockState(), 2);
        level.setBlock(fenceGate, Blocks.OAK_FENCE_GATE.defaultBlockState(), 2);
        Phase0aMutationProbe.sealLevel(level);

        int boundaryY = SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(levelWrite.getY()) + 1);
        BlockPos doorLower = new BlockPos(levelWrite.getX(), boundaryY - 1, levelWrite.getZ() + 5);
        BlockPos doorUpper = doorLower.above();
        assertMapped(helper, levelWrite);
        assertMapped(helper, chunkWrite);
        assertMapped(helper, sectionWrite);
        assertMapped(helper, doorLower);
        assertMapped(helper, doorUpper);

        long cursor = Phase0aMutationProbe.deltaCursor();
        long effectiveTick = level.getGameTime();

        level.setBlockAndUpdate(levelWrite, Blocks.STONE.defaultBlockState());
        LevelChunk directChunk = loadedChunk(helper, chunkWrite);
        directChunk.setBlockState(chunkWrite, Blocks.DIRT.defaultBlockState(), false);
        LevelChunkSection directSection = sectionAt(helper, sectionWrite);
        directSection.setBlockState(
                SectionPos.sectionRelative(sectionWrite.getX()),
                SectionPos.sectionRelative(sectionWrite.getY()),
                SectionPos.sectionRelative(sectionWrite.getZ()),
                Blocks.GLASS.defaultBlockState(), false);
        level.setBlockAndUpdate(waterlogged,
                Blocks.OAK_FENCE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
        level.setBlockAndUpdate(trapdoor,
                level.getBlockState(trapdoor).setValue(TrapDoorBlock.OPEN, true));
        level.setBlockAndUpdate(fenceGate,
                level.getBlockState(fenceGate).setValue(FenceGateBlock.OPEN, true));
        level.setBlock(doorLower,
                Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 2);
        level.setBlock(doorUpper,
                Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2);
        Phase0aMutationProbe.sealLevel(level);

        Set<BlockPos> targets = Set.of(
                levelWrite, chunkWrite, sectionWrite, waterlogged, trapdoor, fenceGate, doorLower, doorUpper);
        Map<BlockPos, Phase0aMutationProbe.MutationDelta> deltas = deltasByPosition(
                helper, Phase0aMutationProbe.sealedDeltasAfter(cursor), targets);
        helper.assertTrue(deltas.size() == targets.size(),
                "all eight mapped mutations must produce exactly one coalesced delta");
        for (BlockPos target : targets) {
            Phase0aMutationProbe.MutationDelta delta = deltas.get(target);
            helper.assertTrue(delta != null, "missing mapped mutation delta at " + target);
            assertLiveDelta(helper, delta);
            helper.assertTrue(delta.effectiveTick() == effectiveTick,
                    "synchronous mutations must preserve their effective tick");
        }

        Phase0aMutationProbe.MutationDelta fluidDelta = deltas.get(waterlogged);
        helper.assertTrue(fluidDelta.oldFluidState().isEmpty() && !fluidDelta.newFluidState().isEmpty(),
                "waterlogging must expose the BlockState-derived FluidState delta");
        helper.assertTrue(!deltas.get(trapdoor).oldState().getValue(TrapDoorBlock.OPEN)
                        && deltas.get(trapdoor).newState().getValue(TrapDoorBlock.OPEN),
                "trapdoor open state must pass through the section hook");
        helper.assertTrue(!deltas.get(fenceGate).oldState().getValue(FenceGateBlock.OPEN)
                        && deltas.get(fenceGate).newState().getValue(FenceGateBlock.OPEN),
                "fence gate open state must pass through the section hook");

        Phase0aMutationProbe.MutationDelta lowerDelta = deltas.get(doorLower);
        Phase0aMutationProbe.MutationDelta upperDelta = deltas.get(doorUpper);
        helper.assertTrue(lowerDelta.watermark() == upperDelta.watermark()
                        && lowerDelta.effectiveTick() == upperDelta.effectiveTick(),
                "Door halves across y=15/16 must share one sealed watermark");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 40)
    public static void unmappedOffThreadRawAndLifecycleRecovery(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ChunkPos anchor = new ChunkPos(helper.absolutePos(new BlockPos(1, 1, 1)));
        ChunkPos protoPos = new ChunkPos(anchor.x + 10_000, anchor.z + 10_000);
        ProtoChunk proto = new ProtoChunk(
                protoPos,
                UpgradeData.EMPTY,
                level,
                level.registryAccess().registryOrThrow(Registries.BIOME),
                null);
        BlockPos protoBlock = new BlockPos(
                protoPos.getMinBlockX(), level.getMinBuildHeight(), protoPos.getMinBlockZ());
        LevelChunkSection protoSection = proto.getSection(proto.getSectionIndex(protoBlock.getY()));
        long unmappedBefore = Phase0aMutationProbe.unmappedWrites();
        long cursor = Phase0aMutationProbe.deltaCursor();
        proto.setBlockState(protoBlock, Blocks.STONE.defaultBlockState(), false);
        Phase0aMutationProbe.sealLevel(level);
        helper.assertTrue(Phase0aMutationProbe.unmappedWrites() == unmappedBefore + 1,
                "an unmapped ProtoChunk write must be observed as worldgen-only");
        helper.assertTrue(Phase0aMutationProbe.ownerFor(protoSection) == null,
                "ProtoChunk section must not acquire a loaded-world owner");
        helper.assertTrue(Phase0aMutationProbe.sealedDeltasAfter(cursor).stream()
                        .noneMatch(delta -> delta.section() == protoSection),
                "unmapped worldgen write must create zero thermal delta work");

        ChunkPos syntheticPos = new ChunkPos(protoPos.x + 1, protoPos.z);
        LevelChunk synthetic = new LevelChunk(level, syntheticPos);
        long generation = Phase0aMutationProbe.registerLoadedChunk(level, synthetic);
        LevelChunkSection section = synthetic.getSection(0);
        Phase0aMutationProbe.LoadedSectionOwner owner = Phase0aMutationProbe.ownerFor(section);
        helper.assertTrue(owner != null && owner.lifecycleGeneration() == generation,
                "synthetic loaded section must be identity-mapped with its chunk generation");
        Phase0aMutationProbe.setFingerprintInterest(section, true);

        Phase0aMutationProbe.PublicationToken beforeAsync = Phase0aMutationProbe.capturePublication(section);
        long offThreadBefore = Phase0aMutationProbe.offThreadWrites();
        AtomicReference<Throwable> asyncFailure = new AtomicReference<>();
        Thread writer = new Thread(() -> {
            try {
                section.setBlockState(1, 1, 1, Blocks.COBBLESTONE.defaultBlockState(), false);
            } catch (Throwable failure) {
                asyncFailure.set(failure);
            }
        }, "phase0a-mapped-section-writer");
        writer.start();
        try {
            writer.join();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for Phase 0a writer", interrupted);
        }
        helper.assertTrue(asyncFailure.get() == null, "off-thread candidate write failed: " + asyncFailure.get());
        helper.assertTrue(Phase0aMutationProbe.offThreadWrites() == offThreadBefore + 1,
                "mapped off-thread write must take the atomic sticky path");
        helper.assertTrue(owner.fullGeometryResyncRequired()
                        && owner.resyncReason() == Phase0aMutationProbe.ResyncReason.OFF_THREAD_WRITE,
                "mapped off-thread write must set FULL_GEOMETRY_RESYNC_REQUIRED");
        helper.assertTrue(!Phase0aMutationProbe.acceptsPublication(beforeAsync),
                "pre-write revision publication must be rejected");
        helper.assertTrue(Phase0aMutationProbe.acknowledgeFullGeometryResync(section, generation),
                "main-thread resnapshot ACK must clear the matching sticky generation");

        long hookCallsBeforeRaw = Phase0aMutationProbe.hookCalls();
        long rawBefore = Phase0aMutationProbe.rawBypassDetections();
        section.getStates().set(2, 2, 2, Blocks.GLASS.defaultBlockState());
        helper.assertTrue(Phase0aMutationProbe.hookCalls() == hookCallsBeforeRaw,
                "PalettedContainer#set must demonstrably bypass the section hook");
        Phase0aMutationProbe.FingerprintScanResult scan = Phase0aMutationProbe.scanFingerprint(section);
        helper.assertTrue(scan.scannedSections() == 1 && scan.mismatches() == 1,
                "active-section fingerprint must detect the raw palette bypass");
        helper.assertTrue(Phase0aMutationProbe.rawBypassDetections() == rawBefore + 1
                        && owner.fullGeometryResyncRequired()
                        && owner.resyncReason() == Phase0aMutationProbe.ResyncReason.RAW_PALETTE_BYPASS,
                "raw bypass must remain sticky until a bounded resnapshot ACK");
        section.getStates().set(2, 2, 2, Blocks.AIR.defaultBlockState());
        section.recalcBlockCounts();
        helper.assertTrue(Phase0aMutationProbe.acknowledgeFullGeometryResync(section, generation),
                "raw bypass recovery ACK must accept the live generation");

        Phase0aMutationProbe.PublicationToken oldPublication = Phase0aMutationProbe.capturePublication(section);
        Phase0aMutationProbe.unregisterLoadedChunk(level, synthetic);
        helper.assertTrue(!owner.isValid() && Phase0aMutationProbe.ownerFor(section) == null,
                "unload must invalidate generation before removing the section identity");
        helper.assertTrue(!Phase0aMutationProbe.acceptsPublication(oldPublication),
                "unloaded generation publication must be rejected immediately");
        long staleBefore = Phase0aMutationProbe.staleWrites();
        section.setBlockState(3, 3, 3, Blocks.DIRT.defaultBlockState(), false);
        helper.assertTrue(Phase0aMutationProbe.staleWrites() == staleBefore + 1,
                "a write through the old section identity must be rejected as stale");

        LevelChunk replacement = new LevelChunk(level, syntheticPos);
        long replacementGeneration = Phase0aMutationProbe.registerLoadedChunk(level, replacement);
        LevelChunkSection replacementSection = replacement.getSection(0);
        helper.assertTrue(replacementGeneration > generation,
                "reload incarnation must receive a strictly newer lifecycle generation");
        helper.assertTrue(!Phase0aMutationProbe.acceptsPublication(oldPublication)
                        && Phase0aMutationProbe.acceptsPublication(
                                Phase0aMutationProbe.capturePublication(replacementSection)),
                "old incarnation must stay rejected while the new incarnation can publish");
        Phase0aMutationProbe.unregisterLoadedChunk(level, replacement);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 50)
    public static void waterFlowAndRecursiveSpongeWritesAreCaptured(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int x = 2; x <= 8; x++) {
            for (int z = 2; z <= 8; z++) {
                level.setBlock(helper.absolutePos(new BlockPos(x, 3, z)), Blocks.STONE.defaultBlockState(), 2);
            }
        }
        Phase0aMutationProbe.sealLevel(level);
        BlockPos source = helper.absolutePos(new BlockPos(5, 4, 5));
        BlockPos sponge = helper.absolutePos(new BlockPos(5, 4, 4));
        assertMapped(helper, source);
        long flowCursor = Phase0aMutationProbe.deltaCursor();
        level.setBlockAndUpdate(source, Blocks.WATER.defaultBlockState());

        helper.runAfterDelay(10, () -> {
            Phase0aMutationProbe.sealLevel(level);
            List<Phase0aMutationProbe.MutationDelta> flow = Phase0aMutationProbe.sealedDeltasAfter(flowCursor);
            long flowedNeighbors = flow.stream()
                    .filter(delta -> !delta.worldPos().equals(source))
                    .filter(delta -> !delta.newFluidState().isEmpty())
                    .count();
            helper.assertTrue(flowedNeighbors > 0,
                    "scheduled vanilla water flow must reach the authoritative section hook");
            flow.stream().filter(delta -> !delta.newFluidState().isEmpty())
                    .forEach(delta -> assertLiveDelta(helper, delta));

            long spongeCursor = Phase0aMutationProbe.deltaCursor();
            Phase0aMutationProbe.LoadedSectionOwner spongeOwner =
                    Phase0aMutationProbe.ownerFor(sectionAt(helper, sponge));
            long callbackWritesBefore = spongeOwner.mainThreadMutationCount();
            level.setBlockAndUpdate(sponge, Blocks.SPONGE.defaultBlockState());
            Phase0aMutationProbe.sealLevel(level);
            List<Phase0aMutationProbe.MutationDelta> absorption =
                    Phase0aMutationProbe.sealedDeltasAfter(spongeCursor);
            long removedWater = absorption.stream()
                    .filter(delta -> !delta.oldFluidState().isEmpty() && delta.newFluidState().isEmpty())
                    .count();
            helper.assertTrue(removedWater >= 2,
                    "SpongeBlock#onPlace recursion must expose multiple water-removal deltas");
            helper.assertTrue(spongeOwner.mainThreadMutationCount() >= callbackWritesBefore + 2,
                    "sponge placement and nested wet-sponge replacement must both hit the low-level hook");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 50)
    public static void movingPistonExtendRetractAndOnRemoveAreCaptured(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos piston = helper.absolutePos(new BlockPos(10, 4, 10));
        BlockPos payload = piston.east();
        BlockPos power = piston.west();
        BlockState pistonState = Blocks.PISTON.defaultBlockState()
                .setValue(PistonBaseBlock.FACING, net.minecraft.core.Direction.EAST);
        level.setBlock(payload, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(piston, pistonState, 3);
        Phase0aMutationProbe.sealLevel(level);
        assertMapped(helper, piston);
        long cursor = Phase0aMutationProbe.deltaCursor();
        level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
        helper.runAfterDelay(4, () -> level.setBlockAndUpdate(power, Blocks.AIR.defaultBlockState()));
        helper.runAfterDelay(12, () -> {
            Phase0aMutationProbe.sealLevel(level);
            List<Phase0aMutationProbe.MutationDelta> deltas = Phase0aMutationProbe.sealedDeltasAfter(cursor).stream()
                    .filter(delta -> delta.worldPos().closerThan(piston, 5.0D))
                    .toList();
            boolean movingPlaced = deltas.stream().anyMatch(delta -> delta.newState().is(Blocks.MOVING_PISTON));
            boolean movingRemoved = deltas.stream().anyMatch(delta ->
                    delta.oldState().is(Blocks.MOVING_PISTON) && !delta.newState().is(Blocks.MOVING_PISTON));
            boolean extended = deltas.stream().anyMatch(delta ->
                    delta.newState().is(Blocks.PISTON)
                            && delta.newState().getValue(PistonBaseBlock.EXTENDED));
            helper.assertTrue(movingPlaced && movingRemoved && extended,
                    "piston extend/retract must capture moving state creation and onRemove cleanup");
            helper.assertTrue(deltas.size() >= 4,
                    "piston transition must produce the expected multi-position low-level writes");
            deltas.forEach(delta -> assertLiveDelta(helper, delta));
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 20)
    public static void sectionIndexedDynamicExclusionContract(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(1, 1, 1));
        int sectionX = SectionPos.blockToSectionCoord(base.getX());
        int sectionY = SectionPos.blockToSectionCoord(base.getY());
        int sectionZ = SectionPos.blockToSectionCoord(base.getZ());
        Phase0aDynamicExclusionIndex.SectionKey oldSection = new Phase0aDynamicExclusionIndex.SectionKey(
                level.dimension(), sectionX, sectionY, sectionZ);
        Phase0aDynamicExclusionIndex.SectionKey newSection = new Phase0aDynamicExclusionIndex.SectionKey(
                level.dimension(), sectionX + 1, sectionY, sectionZ);
        Phase0aDynamicExclusionIndex.SectionKey witnessOnly = new Phase0aDynamicExclusionIndex.SectionKey(
                level.dimension(), sectionX + 2, sectionY, sectionZ);
        Phase0aDynamicExclusionIndex index = new Phase0aDynamicExclusionIndex(8);
        index.setInterestedSections(Set.of(oldSection, newSection, witnessOnly));

        AABB oldBounds = sectionBox(oldSection, 2.0D, 6.0D);
        AABB newBounds = sectionBox(newSection, 1.0D, 7.0D);
        index.update(7L, level.dimension(), null, oldBounds, level.getGameTime(), 41L);
        Phase0aDynamicExclusionIndex.DynamicUpdate move = index.update(
                7L, level.dimension(), oldBounds, newBounds, level.getGameTime(), 42L);
        helper.assertTrue(move.oldSections().equals(Set.of(oldSection))
                        && move.newSections().equals(Set.of(newSection))
                        && move.invalidatedSections().equals(Set.of(oldSection, newSection)),
                "old/new AABB snapshots must conservatively invalidate the interested section union");
        helper.assertTrue(move.effectiveTick() == level.getGameTime() && move.watermark() == 42L,
                "old/new dynamic exclusion snapshots must share one effective tick/watermark");
        helper.assertTrue(index.stateAt(oldSection) == Phase0aDynamicExclusionIndex.ExclusionState.CLEAR
                        && index.stateAt(newSection)
                                == Phase0aDynamicExclusionIndex.ExclusionState.UNRESOLVED_DYNAMIC
                        && index.stateAt(witnessOnly) == Phase0aDynamicExclusionIndex.ExclusionState.CLEAR,
                "only intersected active/witness sections may retain UNRESOLVED_DYNAMIC");
        index.setInterestedSections(Set.of(oldSection, witnessOnly));
        helper.assertTrue(index.stateAt(newSection) == Phase0aDynamicExclusionIndex.ExclusionState.CLEAR,
                "dropping interest must release the materialized section index");
        index.setInterestedSections(Set.of(oldSection, newSection, witnessOnly));
        helper.assertTrue(index.stateAt(newSection)
                        == Phase0aDynamicExclusionIndex.ExclusionState.UNRESOLVED_DYNAMIC,
                "re-admission must replay the retained current AABB without requiring object movement");
        AABB witnessBounds = sectionBox(witnessOnly, 1.0D, 7.0D);
        Phase0aDynamicExclusionIndex.DynamicUpdate moveWithoutCallerOldBounds = index.update(
                7L, level.dimension(), null, witnessBounds, level.getGameTime(), 43L);
        helper.assertTrue(moveWithoutCallerOldBounds.invalidatedSections().equals(Set.of(newSection, witnessOnly)),
                "materialized old sections must be invalidated even when the caller omits oldBounds");
        helper.assertTrue(index.stateAt(newSection) == Phase0aDynamicExclusionIndex.ExclusionState.CLEAR
                        && index.stateAt(witnessOnly)
                                == Phase0aDynamicExclusionIndex.ExclusionState.UNRESOLVED_DYNAMIC,
                "an omitted oldBounds snapshot must not leave a stale dynamic exclusion indexed");
        helper.succeed();
    }

    private static AABB sectionBox(
            Phase0aDynamicExclusionIndex.SectionKey section, double minOffset, double maxOffset) {
        double x = SectionPos.sectionToBlockCoord(section.sectionX());
        double y = SectionPos.sectionToBlockCoord(section.sectionY());
        double z = SectionPos.sectionToBlockCoord(section.sectionZ());
        return new AABB(
                x + minOffset, y + minOffset, z + minOffset,
                x + maxOffset, y + maxOffset, z + maxOffset);
    }

    private static Map<BlockPos, Phase0aMutationProbe.MutationDelta> deltasByPosition(
            GameTestHelper helper,
            List<Phase0aMutationProbe.MutationDelta> deltas,
            Set<BlockPos> targets) {
        Map<BlockPos, Phase0aMutationProbe.MutationDelta> result = new HashMap<>();
        for (Phase0aMutationProbe.MutationDelta delta : deltas) {
            if (targets.contains(delta.worldPos())) {
                helper.assertTrue(result.put(delta.worldPos(), delta) == null,
                        "position emitted more than one coalesced delta: " + delta.worldPos());
            }
        }
        return result;
    }

    private static void assertLiveDelta(
            GameTestHelper helper, Phase0aMutationProbe.MutationDelta delta) {
        Phase0aMutationProbe.LoadedSectionOwner owner = Phase0aMutationProbe.ownerFor(delta.section());
        helper.assertTrue(owner != null && owner.isValid(), "delta section owner must still be live");
        helper.assertTrue(owner.lifecycleGeneration() == delta.lifecycleGeneration(),
                "delta lifecycle generation must match the identity-mapped owner");
        helper.assertTrue(delta.effectiveTick() >= 0L,
                "sealed mutation must retain a valid effective tick");
        helper.assertTrue(delta.watermark() > 0L, "sealed mutation must have a positive watermark");
        helper.assertTrue(Phase0aMutationProbe.acceptsPublication(
                        Phase0aMutationProbe.capturePublication(delta.section())),
                "live, resync-clean section generation must allow worker publication");
    }

    private static void assertMapped(GameTestHelper helper, BlockPos pos) {
        LevelChunkSection section = sectionAt(helper, pos);
        Phase0aMutationProbe.LoadedSectionOwner owner = Phase0aMutationProbe.ownerFor(section);
        helper.assertTrue(owner != null && owner.isValid(),
                "GameTest position must already belong to a loaded/mapped section: " + pos);
    }

    private static LevelChunkSection sectionAt(GameTestHelper helper, BlockPos pos) {
        LevelChunk chunk = loadedChunk(helper, pos);
        return chunk.getSection(chunk.getSectionIndex(pos.getY()));
    }

    private static LevelChunk loadedChunk(GameTestHelper helper, BlockPos pos) {
        LevelChunk chunk = helper.getLevel().getChunkSource().getChunkNow(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ()));
        helper.assertTrue(chunk != null, "GameTest must not load a missing chunk for " + pos);
        return chunk;
    }
}

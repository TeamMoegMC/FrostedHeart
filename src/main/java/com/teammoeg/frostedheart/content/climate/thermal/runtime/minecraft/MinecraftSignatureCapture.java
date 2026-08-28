/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.profile.DependencyOffsetMask;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolverBlockView;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalResolution;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.ThermalSignatureResolverDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/** Main-thread loaded-only signature capture with reusable indexed scratch. */
final class MinecraftSignatureCapture {
    private static final ResolverBlockView.LookupStatus[] LOOKUP_STATUSES =
            ResolverBlockView.LookupStatus.values();
    private final ThermalSignatureResolverDispatcher dispatcher;
    private final ThermalSignatureRegistry signatures;
    private final Map<BlockState, Integer> staticSignatureIds;
    private final ResolverBlockView.Scratch<BlockState, FluidState> resolver =
            new ResolverBlockView.Scratch<>();
    private final PageSignatures.Builder pageBuilder =
            new PageSignatures.Builder();
    private final SectionCells sectionCells;
    private final CubeCells cubeCells;

    MinecraftSignatureCapture(
            ServerLevel level,
            ThermalSignatureResolverDispatcher dispatcher,
            ThermalSignatureRegistry signatures,
            Map<BlockState, Integer> staticSignatureIds
    ) {
        this.dispatcher = dispatcher;
        this.signatures = signatures;
        this.staticSignatureIds = staticSignatureIds;
        sectionCells = new SectionCells(level);
        cubeCells = new CubeCells(level);
    }

    PageSignatures capturePage(
            long sectionKey,
            LevelChunkSection section
    ) {
        sectionCells.begin(sectionKey, section);
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    pageBuilder.set(
                            x | z << 4 | y << 8,
                            resolveId(sectionCells, x, y, z));
                }
            }
        }
        return pageBuilder.build();
    }

    int resolveSignatureId(int worldX, int worldY, int worldZ) {
        cubeCells.begin(worldX, worldY, worldZ);
        return resolveId(cubeCells, 0, 0, 0);
    }

    private int resolveId(CellReader cells, int x, int y, int z) {
        ResolverBlockView.LookupStatus selfStatus = cells.status(x, y, z);
        if (selfStatus != ResolverBlockView.LookupStatus.PRESENT) {
            return -1;
        }
        BlockState self = cells.blockState(x, y, z);
        Integer staticSignatureId = staticSignatureIds.get(self);
        if (staticSignatureId != null) {
            return staticSignatureId;
        }
        ThermalSignatureResolverDispatcher.DispatchPlan plan =
                dispatcher.plan(self);
        ResolverBlockView<BlockState, FluidState> view =
                resolver.begin(plan.dependencyMask());
        for (DependencyOffsetMask.Offset offset
                : plan.dependencyMask().offsets()) {
            int targetX = x + offset.x();
            int targetY = y + offset.y();
            int targetZ = z + offset.z();
            ResolverBlockView.LookupStatus status =
                    cells.status(targetX, targetY, targetZ);
            if (status == ResolverBlockView.LookupStatus.PRESENT) {
                resolver.putPresent(
                        offset.x(), offset.y(), offset.z(),
                        cells.blockState(targetX, targetY, targetZ),
                        cells.fluidState(targetX, targetY, targetZ));
            } else {
                resolver.putUnavailable(
                        offset.x(), offset.y(), offset.z(), status);
            }
        }
        ThermalResolution<ResolvedThermalSignature> resolved =
                plan.resolve(view);
        if (!resolved.isResolved()) {
            return -1;
        }
        return signatures.idOrDefault(resolved.value(), -1);
    }

    private interface CellReader {
        ResolverBlockView.LookupStatus status(int x, int y, int z);
        BlockState blockState(int x, int y, int z);
        FluidState fluidState(int x, int y, int z);
    }

    private abstract static class IndexedCells implements CellReader {
        final ServerLevel level;
        final BlockState[] blockStates;
        final FluidState[] fluidStates;
        final byte[] statuses;
        final int[] generations;
        final BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        int generation = 1;

        IndexedCells(ServerLevel level, int capacity) {
            this.level = level;
            blockStates = new BlockState[capacity];
            fluidStates = new FluidState[capacity];
            statuses = new byte[capacity];
            generations = new int[capacity];
        }

        final void nextGeneration() {
            if (++generation == 0) {
                java.util.Arrays.fill(generations, 0);
                generation = 1;
            }
        }

        abstract int index(int x, int y, int z);
        abstract void worldPosition(int x, int y, int z);
        abstract BlockState localState(int x, int y, int z);

        @Override
        public final ResolverBlockView.LookupStatus status(
                int x,
                int y,
                int z
        ) {
            int index = index(x, y, z);
            if (index < 0) {
                return ResolverBlockView.LookupStatus.MISSING;
            }
            ensure(index, x, y, z);
            return LOOKUP_STATUSES[
                    Byte.toUnsignedInt(statuses[index])];
        }

        @Override
        public final BlockState blockState(int x, int y, int z) {
            int index = index(x, y, z);
            ensure(index, x, y, z);
            return blockStates[index];
        }

        @Override
        public final FluidState fluidState(int x, int y, int z) {
            int index = index(x, y, z);
            ensure(index, x, y, z);
            return fluidStates[index];
        }

        private void ensure(int index, int x, int y, int z) {
            if (generations[index] == generation) {
                return;
            }
            generations[index] = generation;
            BlockState local = localState(x, y, z);
            if (local != null) {
                writePresent(index, local);
                return;
            }
            worldPosition(x, y, z);
            if (level.isOutsideBuildHeight(position.getY())) {
                statuses[index] =
                        (byte) ResolverBlockView.LookupStatus.MISSING.ordinal();
                return;
            }
            LevelChunk chunk = level.getChunkSource().getChunkNow(
                    SectionPos.blockToSectionCoord(position.getX()),
                    SectionPos.blockToSectionCoord(position.getZ()));
            if (chunk == null) {
                statuses[index] =
                        (byte) ResolverBlockView.LookupStatus.UNLOADED.ordinal();
                return;
            }
            writePresent(index, chunk.getBlockState(position));
        }

        private void writePresent(int index, BlockState state) {
            blockStates[index] = state;
            fluidStates[index] = state.getFluidState();
            statuses[index] =
                    (byte) ResolverBlockView.LookupStatus.PRESENT.ordinal();
        }
    }

    private static final class SectionCells extends IndexedCells {
        private static final int WIDTH = 18;
        private int minX;
        private int minY;
        private int minZ;
        private LevelChunkSection section;

        SectionCells(ServerLevel level) {
            super(level, WIDTH * WIDTH * WIDTH);
        }

        void begin(long sectionKey, LevelChunkSection section) {
            nextGeneration();
            minX = SectionPos.sectionToBlockCoord(SectionPos.x(sectionKey));
            minY = SectionPos.sectionToBlockCoord(SectionPos.y(sectionKey));
            minZ = SectionPos.sectionToBlockCoord(SectionPos.z(sectionKey));
            this.section = section;
        }

        @Override
        int index(int x, int y, int z) {
            if (x < -1 || x > 16 || y < -1 || y > 16
                    || z < -1 || z > 16) {
                return -1;
            }
            return ((y + 1) * WIDTH + z + 1) * WIDTH + x + 1;
        }

        @Override
        void worldPosition(int x, int y, int z) {
            position.set(minX + x, minY + y, minZ + z);
        }

        @Override
        BlockState localState(int x, int y, int z) {
            return section != null && x >= 0 && x < 16
                    && y >= 0 && y < 16 && z >= 0 && z < 16
                    ? section.getBlockState(x, y, z)
                    : null;
        }
    }

    private static final class CubeCells extends IndexedCells {
        private int originX;
        private int originY;
        private int originZ;

        CubeCells(ServerLevel level) {
            super(level, 27);
        }

        void begin(int x, int y, int z) {
            nextGeneration();
            originX = x;
            originY = y;
            originZ = z;
        }

        @Override
        int index(int x, int y, int z) {
            return Math.abs(x) <= 1 && Math.abs(y) <= 1 && Math.abs(z) <= 1
                    ? ((y + 1) * 3 + z + 1) * 3 + x + 1
                    : -1;
        }

        @Override
        void worldPosition(int x, int y, int z) {
            position.set(originX + x, originY + y, originZ + z);
        }

        @Override
        BlockState localState(int x, int y, int z) {
            return null;
        }
    }
}
